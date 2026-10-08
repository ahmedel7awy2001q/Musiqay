package com.musiqay.app

import android.Manifest
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.test.platform.app.InstrumentationRegistry
import com.musiqay.app.ui.MusicViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(AndroidJUnit4::class)
class ListeningFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val testFiles = mutableListOf<Uri>()
    private val fixtures = object : ExternalResource() {
        override fun before() {
            val context = ApplicationProvider.getApplicationContext<MusiqayApplication>()
            runBlocking { context.settingsRepository.setTheme(com.musiqay.app.data.ThemeMode.LIGHT) }
            listOf("من روائع المنشاوي سورة يوسف تلاوة عالية الجودة", "محاضرة اختبار الاستكمال").forEachIndexed { index, title ->
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "musiqay-test-$index.wav")
                    put(MediaStore.Audio.Media.TITLE, title)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/MusiqayTests/")
                    put(MediaStore.Audio.Media.IS_MUSIC, 1)
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)!!
                testFiles += uri
                val dataSize = 16_000 * 2 * 60
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                    put("RIFF".toByteArray()); putInt(36 + dataSize); put("WAVEfmt ".toByteArray())
                    putInt(16); putShort(1.toShort()); putShort(1.toShort()); putInt(16_000); putInt(32_000)
                    putShort(2.toShort()); putShort(16.toShort()); put("data".toByteArray()); putInt(dataSize)
                }.array()
                context.contentResolver.openOutputStream(uri)!!.use { it.write(header); it.write(ByteArray(dataSize)) }
                context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)
                context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Audio.Media.TITLE, title) }, null, null)
            }
        }
        override fun after() {
            val context = ApplicationProvider.getApplicationContext<MusiqayApplication>()
            testFiles.forEach { context.contentResolver.delete(it, null, null) }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(GrantPermissionRule.grant(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS)).around(fixtures).around(compose)

    @Test fun screensAndListeningToolsWorkWithArabicLargeText() {
        lateinit var vm: MusicViewModel
        compose.runOnUiThread { vm = ViewModelProvider(compose.activity)[MusicViewModel::class.java]; vm.refreshLibrary() }
        compose.waitUntil(20_000) { vm.visibleSongs.value.size >= 2 }
        capture("01-home")
        compose.onNode(hasText("المكتبة") and hasClickAction()).performClick()
        compose.onNodeWithText("التلاوات").assertIsDisplayed()
        capture("02-library")
        val first = vm.visibleSongs.value.first { it.uri == testFiles[0] }
        val second = vm.visibleSongs.value.first { it.uri == testFiles[1] }
        compose.runOnUiThread { vm.player.play(first, listOf(first, second)) }
        compose.waitUntil(15_000) { vm.player.state.value.isPlaying }
        compose.runOnUiThread { vm.player.seekTo(25_000); vm.player.setPlaybackSpeed(1.25f) }
        compose.waitUntil(5_000) { vm.player.state.value.positionMs >= 25_000 }
        assertEquals(1.25f, vm.player.state.value.playbackSpeed, 0.001f)
        compose.runOnUiThread { vm.player.togglePlayPause(); vm.addBookmark(first, "موضع الاختبار") }
        compose.waitUntil(5_000) { vm.listening.resumeFor(first) >= 25_000 }
        assertEquals(1, vm.audioBookmarks.value.count { it.mediaId == first.id })
        compose.runOnUiThread { vm.player.play(second, listOf(first, second)) }
        compose.waitUntil(5_000) { vm.player.state.value.mediaId == second.id }
        compose.runOnUiThread { vm.player.play(first, listOf(first, second)) }
        compose.waitUntil(5_000) { vm.player.state.value.mediaId == first.id && vm.player.state.value.positionMs >= 25_000 }
        compose.onNodeWithContentDescription("فتح المشغل").performClick()
        compose.onNodeWithText("علامات").assertIsDisplayed()
        compose.onNodeWithText("علامات").performClick()
        compose.onNodeWithText("موضع الاختبار", substring = true).assertIsDisplayed()
        capture("03-bookmarks")
        compose.onNodeWithText("موضع الاختبار", substring = true).performClick()
        capture("04-player")
        // End-of-recording sleep works inside the service while the Activity is backgrounded.
        compose.runOnUiThread { vm.player.setSleepAtTrackEnd(); vm.player.seekTo(59_500) }
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        compose.waitUntil(10_000) { !vm.player.sleepAtTrackEnd.value && !vm.player.state.value.wantsPlayback }
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        compose.onNodeWithContentDescription("رجوع").performClick()
        compose.onNode(hasText("راديو") and hasClickAction()).performClick()
        compose.onNodeWithText("إضافة محطة").assertIsDisplayed()
        capture("05-radio")
        compose.onNodeWithText("إضافة محطة").performClick()
        compose.onNodeWithText("اسم المحطة").assertIsDisplayed()
        capture("06-manual-station")
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val folder = File(compose.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val file = File(folder, "$name.png")
        file.outputStream().use {
            compose.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        // Gradle removes the test app after the suite. Keep visual evidence before cleanup.
        fun shell(command: String): String {
            val fd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
            return android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).bufferedReader().use { it.readText() }.trim()
        }
        // UiAutomation executes one command directly; shell operators are not interpreted.
        val destination = "/sdcard/Download/MusiqayTestShots"
        shell("mkdir -p $destination")
        shell("cp ${file.absolutePath} $destination/$name.png")
        assertEquals(file.length().toString(), shell("stat -c %s $destination/$name.png"))
    }
}
