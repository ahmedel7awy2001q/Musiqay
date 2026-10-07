package com.musiqay.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musiqay.app.MusiqayApplication
import com.musiqay.app.data.BackupPreview
import com.musiqay.app.data.BackupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun BackupControls(vm: MusicViewModel) {
    val repository = remember(vm) { BackupRepository(vm.getApplication<MusiqayApplication>()) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<BackupPreview?>(null) }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try { repository.export(uri); message = "تم حفظ النسخة الاحتياطية" }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { message = "تعذر حفظ النسخة. اختر مكانًا آخر وحاول مجددًا" }
            finally { busy = false }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try { pending = repository.preview(uri) }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { message = "الملف غير صالح أو ليس نسخة احتياطية لموسيقاي" }
            finally { busy = false }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("المفضلة والقوائم والعلامات ومواضع المتابعة والإعدادات. الملفات الصوتية نفسها لا تُنسخ. مراجع الملفات مخصصة لهذا الهاتف؛ نقلها إلى هاتف آخر يحتاج إعادة ربطها.", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = !busy, onClick = { create.launch("Musiqay-backup.json") }) { Text("تصدير") }
            OutlinedButton(enabled = !busy, onClick = { open.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("استيراد") }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (message.isNotBlank()) Text(message, style = MaterialTheme.typography.bodySmall)
    }
    pending?.let { preview -> AlertDialog(onDismissRequest = { if (!busy) pending = null }, title = { Text("استيراد نسخة احتياطية") },
        text = { Text("${preview.favorites} ملف مفضل، ${preview.playlists} قائمة، ${preview.bookmarks} علامة. سنَدمجها مع بياناتك الحالية ونستعيد الإعدادات؛ القوائم ذات الاسم نفسه تُدمج. قد تحتاج الملفات إلى صلاحية وصول على هذا الهاتف.") },
        confirmButton = { Button(enabled = !busy, onClick = { scope.launch {
            busy = true
            try { repository.restore(preview); vm.refreshLibrary(); message = "تم دمج النسخة مع بياناتك"; pending = null }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { message = "لم يكتمل الاستيراد. بياناتك الحالية محفوظة؛ يمكنك إعادة المحاولة"; pending = null }
            finally { busy = false }
        } }) { Text("دمج واستيراد") } }, dismissButton = { TextButton(enabled = !busy, onClick = { pending = null }) { Text("إلغاء") } }) }
}
