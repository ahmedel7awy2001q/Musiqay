package com.musiqay.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun SleepTimerDialog(active: Boolean, onSet: (Int?) -> Unit, onAtEnd: (() -> Unit)? = null, atEndActive: Boolean = false, onDismiss: () -> Unit) {
    var custom by rememberSaveable { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    if (custom) AlertDialog(onDismissRequest = onDismiss, title = { Text("مدة مخصصة") },
        text = { OutlinedTextField(value = input, onValueChange = { value ->
            input = value.mapNotNull { char -> Character.digit(char, 10).takeIf { it >= 0 }?.digitToChar() }.joinToString("").take(4)
        }, label = { Text("عدد الدقائق، من 1 إلى 1440") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
        confirmButton = { Button(enabled = input.toIntOrNull()?.let { it in 1..1440 } == true,
            onClick = { onSet(input.toIntOrNull()); onDismiss() }) { Text("تشغيل المؤقت") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
    else AlertDialog(onDismissRequest = onDismiss, title = { Text("مؤقت النوم") },
        text = { Column {
            listOf(15, 30, 45, 60, 90).forEach { minutes ->
                TextButton(onClick = { onSet(minutes); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Text("إيقاف بعد $minutes دقيقة")
                }
            }
            if (onAtEnd != null) TextButton(onClick = { onAtEnd(); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("إيقاف عند نهاية المقطع") }
            TextButton(onClick = { custom = true }) { Text("مدة مخصصة") }
            if (active || atEndActive) TextButton(onClick = { onSet(null); onDismiss() }) { Text("إلغاء المؤقت") }
        } }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } })
}
