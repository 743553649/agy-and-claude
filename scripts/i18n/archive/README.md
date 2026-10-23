# 汉化脚本归档

**这些脚本已完成使命，不要重跑。**

2026-10-23 汉化工程结档时，`scripts/i18n/` 下除 `verify.py` 外的全部工具移入此处。
它们是一次性的**源码改写**工具，不是只读检查器——运行会直接修改 Kotlin 源码与资源文件。

## 为什么单独隔离

`apply_*.py` 这批脚本的作用就是「把英文文案回写成 `stringResource` / `getString`」。
汉化已经完成，代码里不再有硬编码的英文文案，重跑它们要么什么都不改，要么在已经改过的
站点上二次改写，破坏现有代码。

2026-10-22 有过一次真实事故（见 `docs/i18n/archive/HANDOFF.md` 坑 9）：为了核对而
`import` 了 `apply_dyn_attrs.py`，其顶层代码直接执行，一口气改了 18 处源码，其中 2 处把
发给模型的提示词改成了 `stringResource`（服务类不是 Composable，编译必然失败）。

## 仍然保留在用的

`../verify.py` —— CI 的 `Verify i18n resources` 步骤在每次构建前跑它，校验资源完整性、
占位符、R 导入。只读，无副作用，长期保留。

## 需要找回某个脚本时

这些文件都在 git 历史里。按需取出**单个**，不要整批恢复：

```bash
git log --oneline -- scripts/i18n/apply_toast.py
git checkout <commit>^ -- scripts/i18n/apply_toast.py
```

取出后只在临时副本上运行，并逐条 `git diff` 复核（坑 9 的教训）。

## 内容清单

| 类别 | 文件 | 副作用 |
|---|---|---|
| 一次性源码改写 | `apply*.py`（27 个），各自对应一个汉化批次 | 改写 Kotlin 源码 |
| 资源与覆盖表生成 | `gen_resources.py`、`gen_coverage.py`、`fix_imports.py`、`dynamic.py`、`dynamic_apply.py` | 重写 `values*/strings_*.xml`、`COVERAGE.md`、源码 import |
| 扫描与目录流水线 | `scan.py`、`scan_ext_js.py`、`build_catalog.py`、`rebuild_catalog.py`、`merge_zh.py` | 只写 `/tmp` 下的 catalog |
| 批次翻译对照表 | `*.tsv`（4 个） | 无；产物已固化为 `res/values-zh-rCN/` |
