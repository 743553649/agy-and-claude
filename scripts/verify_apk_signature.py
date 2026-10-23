#!/usr/bin/env python3
"""校验 APK 的签名证书指纹，保证新版能覆盖安装已装的旧版。

为什么需要这个：Android 只允许**同一签名**的 APK 覆盖安装。本项目自 2026-10-22 起
统一使用一把 release 密钥（见 docs/i18n/archive/HANDOFF.md 的「签名标准」），一旦某次构建
意外换成了别的密钥（例如 secret 失效回退到 debug 签名），装出来的包就无法覆盖旧版，
用户只能卸载重装、数据全丢。所以每次 CI 构建后都跑一遍这个脚本，指纹不符直接红。

注意：apksigner 在部分环境不可用（JDK 的 LoggerFinderLoader 初始化失败），这里直接
解析 APK Signing Block 的 v2 签名结构取证书 DER，再做 SHA-256，结论等价。

用法：
    python3 scripts/verify_apk_signature.py app.apk            # 比对标准指纹
    python3 scripts/verify_apk_signature.py app.apk <期望指纹>  # 比对指定指纹（可带冒号）

退出码：0 一致；1 不一致；2 没有 v2 签名或文件无法解析。
"""

import hashlib
import struct
import sys

# 本项目的唯一标准签名（不得更改；换它等于所有已安装用户必须卸载重装）
EXPECTED_SHA256 = "6FF1E3FFA0C15F2F647474FB04F478FD1D06C08840CC6B6E6E72ED6A942A7B5C"

V2_BLOCK_ID = 0x7109871A
V3_BLOCK_ID = 0xF05368C0
V1_BLOCK_ID = 0x1B93AD61


def _u32(buf, off):
    return struct.unpack_from("<I", buf, off)[0]


def _u64(buf, off):
    return struct.unpack_from("<Q", buf, off)[0]


def read_signing_blocks(data):
    """返回 APK Signing Block 的 {id: value}，找不到则返回 None。"""
    # EOCD（End of Central Directory）从尾部向前找
    pos = len(data) - 22
    while pos >= 0:
        if data[pos:pos + 4] == b"PK\x05\x06":
            break
        pos -= 1
    if pos < 0:
        return None
    cd_offset = _u32(data, pos + 16)
    if data[cd_offset - 16:cd_offset] != b"APK Sig Block 42":
        return None
    block_size = _u64(data, cd_offset - 24)
    start = cd_offset - block_size - 8
    if start < 0 or data[start:start + 8] != struct.pack("<Q", block_size):
        return None
    blocks = {}
    p = start + 8
    end = cd_offset - 24
    while p < end:
        pair_len = _u64(data, p)
        pair_id = _u32(data, p + 8)
        blocks[pair_id] = data[p + 12:p + 8 + pair_len]
        p += 8 + pair_len
    return blocks


def signer_certificate(v2):
    """取 v2 块里第一个签名者的证书 DER。

    结构：signers(len) -> signer(len) -> signed_data(len) -> digests(len) -> certificates
    """
    if _u32(v2, 0) == 0:
        raise ValueError("v2 块里没有签名者")
    signer = v2[8:8 + _u32(v2, 4)]
    signed = signer[4:4 + _u32(signer, 0)]
    p = 4 + _u32(signed, 0)          # 跳过 digests
    if _u32(signed, p) == 0:
        raise ValueError("v2 块里没有证书")
    p += 4
    cert_len = _u32(signed, p)
    p += 4
    return signed[p:p + cert_len]


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    apk = sys.argv[1]
    expected = (sys.argv[2] if len(sys.argv) > 2 else EXPECTED_SHA256).replace(":", "").upper()

    with open(apk, "rb") as f:
        data = f.read()
    blocks = read_signing_blocks(data)
    if blocks is None:
        print(f"FAIL: {apk} 没有 APK Signing Block")
        return 2
    if V2_BLOCK_ID not in blocks:
        print(f"FAIL: {apk} 没有 v2 签名（Android 7+ 要求 v2 或更新）")
        return 2

    try:
        cert = signer_certificate(blocks[V2_BLOCK_ID])
    except ValueError as e:
        print(f"FAIL: {apk}: {e}")
        return 2

    actual = hashlib.sha256(cert).hexdigest().upper()
    pretty = ":".join(actual[i:i + 2] for i in range(0, 64, 2))
    schemes = []
    if V1_BLOCK_ID in blocks:
        schemes.append("v1")
    schemes.append("v2")
    if V3_BLOCK_ID in blocks:
        schemes.append("v3")

    print(f"APK: {apk}")
    print(f"签名方案: {' / '.join(schemes)}")
    print(f"证书长度: {len(cert)} 字节")
    print(f"证书 SHA-256: {pretty}")
    print(f"标准指纹:     {':'.join(expected[i:i + 2] for i in range(0, 64, 2))}")

    if actual != expected:
        print("FAIL: 签名证书与标准指纹不一致 —— 这个包装不上已装的旧版")
        return 1
    print("OK: 签名与标准指纹一致，可以覆盖安装")
    return 0


if __name__ == "__main__":
    sys.exit(main())
