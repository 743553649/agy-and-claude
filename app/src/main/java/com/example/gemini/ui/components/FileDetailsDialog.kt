package com.example.gemini.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.io.File
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.gemini.R

@Composable
fun FileDetailsDialog(
    name: String,
    path: String,
    isDir: Boolean,
    size: Long = 0L,
    modTime: Long = 0L,
    childCount: Int? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val localFile = remember(path) { File(path) }
    val existsLocally = remember(localFile) {
        try { localFile.exists() } catch (_: Exception) { false }
    }

    val resolvedSize = remember(size, existsLocally, localFile) {
        if (size > 0) size
        else if (existsLocally) {
            try { if (localFile.isFile) localFile.length() else 0L } catch (_: Exception) { 0L }
        } else 0L
    }

    val resolvedModTime = remember(modTime, existsLocally, localFile) {
        if (modTime > 0) modTime
        else if (existsLocally) {
            try { localFile.lastModified() } catch (_: Exception) { 0L }
        } else 0L
    }

    val permissionsInfo = remember(existsLocally, localFile) {
        if (existsLocally) {
            try {
                val r = if (localFile.canRead()) "r" else "-"
                val w = if (localFile.canWrite()) "w" else "-"
                val x = if (localFile.canExecute()) "x" else "-"
                val readStr = if (localFile.canRead()) context.getString(R.string.component_yes) else context.getString(R.string.component_no)
                val writeStr = if (localFile.canWrite()) context.getString(R.string.component_yes) else context.getString(R.string.component_no)
                val execStr = if (localFile.canExecute()) context.getString(R.string.component_yes) else context.getString(R.string.component_no)
                context.getString(R.string.component_permissions_line, r, w, x, readStr, writeStr, execStr)
            } catch (_: Exception) {
                context.getString(R.string.component_bridge_managed_access)
            }
        } else {
            context.getString(R.string.component_bridge_managed_access)
        }
    }

    val ext = remember(name) { name.substringAfterLast('.', "").lowercase() }
    val typeName = if (isDir) stringResource(R.string.component_file_type_folder)
        else when (ext) {
            "kt", "kts" -> stringResource(R.string.component_file_type_kt, ext)
            "java" -> stringResource(R.string.component_file_type_java)
            "py" -> stringResource(R.string.component_file_type_py)
            "js" -> stringResource(R.string.component_file_type_js)
            "ts" -> stringResource(R.string.component_file_type_ts)
            "jsx", "tsx" -> stringResource(R.string.component_file_type_react, ext)
            "html", "htm" -> stringResource(R.string.component_file_type_html, ext)
            "css", "scss", "sass" -> stringResource(R.string.component_file_type_css, ext)
            "json" -> stringResource(R.string.component_file_type_json)
            "xml" -> stringResource(R.string.component_file_type_xml)
            "yaml", "yml" -> stringResource(R.string.component_file_type_yaml, ext)
            "toml" -> stringResource(R.string.component_file_type_toml)
            "md", "markdown" -> stringResource(R.string.component_file_type_md, ext)
            "txt", "log" -> stringResource(R.string.component_file_type_txt, ext)
            "sh", "bash", "zsh" -> stringResource(R.string.component_file_type_shell, ext)
            "c", "cpp", "h", "hpp" -> stringResource(R.string.component_file_type_c, ext)
            "go" -> stringResource(R.string.component_file_type_go)
            "rs" -> stringResource(R.string.component_file_type_rs)
            "png", "jpg", "jpeg", "webp", "gif", "svg" -> stringResource(R.string.component_file_type_image, ext)
            "zip", "tar", "gz", "rar" -> stringResource(R.string.component_file_type_archive, ext)
            "" -> stringResource(R.string.common_file)
            else -> stringResource(R.string.component_file_type_other, ext.uppercase(), ext)
        }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header: Icon + Name + Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (isDir) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDir) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = if (isDir) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = if (isDir) stringResource(R.string.component_badge_directory) else stringResource(R.string.component_badge_file),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(14.dp))

                // Detail Items
                DetailRow(
                    icon = Icons.Outlined.Category,
                    label = stringResource(R.string.component_type),
                    value = typeName
                )

                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(
                    icon = Icons.Outlined.FolderOpen,
                    label = stringResource(R.string.component_location),
                    value = path,
                    isMonospace = true,
                    actionIcon = Icons.Default.ContentCopy,
                    actionDescription = stringResource(R.string.component_copy_path),
                    onActionClick = {
                        clipboardManager.setText(AnnotatedString(path))
                        Toast.makeText(context, context.getString(R.string.component_toast_path_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                val parentPath = remember(path) { File(path).parent ?: "/" }
                DetailRow(
                    icon = Icons.Outlined.DriveFileMove,
                    label = stringResource(R.string.component_parent_directory),
                    value = parentPath,
                    isMonospace = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                val sizeText = remember(isDir, resolvedSize, childCount) {
                    if (isDir) {
                        if (childCount != null) context.getString(R.string.component_child_items, childCount)
                        else if (resolvedSize > 0) formatDetailedFileSize(context, resolvedSize)
                        else context.getString(R.string.component_dir_label)
                    } else {
                        formatDetailedFileSize(context, resolvedSize)
                    }
                }
                DetailRow(
                    icon = Icons.Outlined.DataUsage,
                    label = stringResource(R.string.component_size),
                    value = sizeText
                )

                Spacer(modifier = Modifier.height(10.dp))

                DetailRow(
                    icon = Icons.Outlined.Security,
                    label = stringResource(R.string.component_permissions),
                    value = permissionsInfo,
                    isMonospace = true
                )

                if (resolvedModTime > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy · hh:mm:ss a", Locale.getDefault()) }
                    DetailRow(
                        icon = Icons.Outlined.Schedule,
                        label = stringResource(R.string.component_last_modified),
                        value = dateFormat.format(Date(resolvedModTime))
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_close), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    isMonospace: Boolean = false,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.5.sp,
                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }

            if (actionIcon != null && onActionClick != null) {
                IconButton(
                    onClick = onActionClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = actionDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

fun formatDetailedFileSize(context: Context, size: Long): String {
    if (size <= 0) return context.getString(R.string.component_size_zero)
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
    val formattedVal = DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble()))
    val rawFormatted = NumberFormat.getNumberInstance(Locale.getDefault()).format(size)
    return if (digitGroups == 0) context.getString(R.string.component_size_bytes, rawFormatted) else context.getString(R.string.component_size_with_raw, "$formattedVal ${units[digitGroups]}", rawFormatted)
}
