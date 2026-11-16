package com.musiqay.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SleepTimerDialog(
    active: Boolean,
    onSet: (Int?) -> Unit,
    onAtEnd: (() -> Unit)? = null,
    atEndActive: Boolean = false,
    onDismiss: () -> Unit
) {
    var custom by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }

    if (custom) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("مدة مخصصة") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { value ->
                        input = value.mapNotNull { char ->
                            Character.digit(char, 10).takeIf { it >= 0 }?.digitToChar()
                        }.joinToString("").take(4)
                    },
                    label = { Text("عدد الدقائق، من 1 إلى 1440") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            },
            confirmButton = {
                Button(
                    enabled = input.toIntOrNull()?.let { it in 1..1440 } == true,
                    onClick = { onSet(input.toIntOrNull()); onDismiss() }
                ) { Text("تشغيل المؤقت") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
        )
        return
    }

    val options = listOf(15, 30, 45, 60, 90)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مؤقت النوم") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { minutes ->
                            FilledTonalButton(
                                onClick = { onSet(minutes); onDismiss() },
                                modifier = Modifier.weight(1f)
                            ) { Text("$minutes د") }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                if (onAtEnd != null) {
                    OutlinedButton(
                        onClick = { onAtEnd(); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (atEndActive) "الإيقاف عند نهاية المقطع مفعّل" else "إيقاف عند نهاية المقطع")
                    }
                }

                OutlinedButton(
                    onClick = { custom = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("مدة مخصصة") }

                if (active || atEndActive) {
                    TextButton(
                        onClick = { onSet(null); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("إلغاء المؤقت الحالي") }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}
