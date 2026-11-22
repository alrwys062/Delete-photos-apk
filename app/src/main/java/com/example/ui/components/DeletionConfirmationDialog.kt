package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.db.ScheduledDeletionEntity
import com.example.ui.theme.LocalTimeDeleteColors
import com.example.util.FormatUtils
import com.example.util.LocalAppLocale
import com.example.util.appString

@Composable
fun DeletionConfirmationDialog(
    items: List<ScheduledDeletionEntity>,
    onDismiss: () -> Unit,
    onConfirm: (List<ScheduledDeletionEntity>) -> Unit
) {
    val ext = LocalTimeDeleteColors.current
    val locale = LocalAppLocale.current
    val totalBytes = items.sumOf { it.sizeBytes }
    val wordFile = appString(if (items.size == 1) R.string.word_file else R.string.word_files)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteForever,
                contentDescription = null,
                tint = ext.danger,
                modifier = Modifier.size(34.dp)
            )
        },
        title = {
            Text(
                text = if (items.size == 1) {
                    appString(R.string.confirm_delete_title_single)
                } else {
                    appString(R.string.confirm_delete_title_multi, items.size)
                },
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = appString(
                        R.string.confirm_delete_body,
                        items.size,
                        wordFile,
                        FormatUtils.formatFileSize(totalBytes, locale)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                items.take(3).forEach { item ->
                    Text(
                        text = "• ${item.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (items.size > 3) {
                    Text(
                        text = appString(R.string.confirm_delete_more, items.size - 3),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = appString(R.string.confirm_delete_android_note),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(items) },
                colors = ButtonDefaults.buttonColors(containerColor = ext.danger),
                modifier = Modifier.testTag("final_delete_confirm_button")
            ) {
                Text(appString(R.string.confirm_delete_btn), color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("final_delete_cancel_button")
            ) {
                Text(appString(R.string.confirm_keep_btn))
            }
        }
    )
}
