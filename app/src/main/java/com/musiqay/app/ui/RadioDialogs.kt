package com.musiqay.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musiqay.app.data.RadioStation
import com.musiqay.app.util.safeRadioUrl

@Composable
fun ManualRadioDialog(onAdd: (String, String) -> RadioStation?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var failed by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("إضافة محطة") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(name, { name = it.take(150) }, label = { Text("اسم المحطة") }, singleLine = true)
            OutlinedTextField(url, { url = it.take(2_000); failed = false }, label = { Text("رابط البث المباشر") }, singleLine = true)
            if (failed) Text("تعذرت الإضافة. تحقق من الرابط وعدد المحطات اليدوية.", color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = { Button(enabled = name.isNotBlank() && safeRadioUrl(url) != null, onClick = {
        failed = onAdd(name, url) == null
    }) { Text("إضافة وتشغيل") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioFavoritesSheet(stations: List<RadioStation>, order: List<String>, onMove: (String, Int) -> Unit, onDismiss: () -> Unit) {
    val byId = remember(stations) { stations.associateBy { it.id } }
    val favorites = order.mapNotNull(byId::get)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
            Text("ترتيب محطاتك المفضلة", style = MaterialTheme.typography.titleLarge)
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                itemsIndexed(favorites, key = { _, station -> station.id }) { index, station ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(station.name, Modifier.weight(1f).padding(vertical = 12.dp), maxLines = 2)
                        IconButton(enabled = index > 0, onClick = { onMove(station.id, -1) }) { Icon(Icons.Rounded.ArrowUpward, "تحريك لأعلى") }
                        IconButton(enabled = index < favorites.lastIndex, onClick = { onMove(station.id, 1) }) { Icon(Icons.Rounded.ArrowDownward, "تحريك لأسفل") }
                    }
                }
            }
        }
    }
}
