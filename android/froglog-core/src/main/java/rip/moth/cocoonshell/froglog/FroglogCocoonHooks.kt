package rip.moth.cocoonshell.froglog

import android.content.Context
import android.util.Log
import rip.moth.cocoonshell.froglog.bridge.FroglogRepository

/**
 * Called from patched Cocoon smali ([pf/c0.smali](android/apk-patches/README.md)) after a session is saved.
 */
object FroglogCocoonHooks {
    private const val TAG = "FroglogHooks"

    @JvmStatic
    fun onGameSessionEndedWithD0(
        context: Context?,
        d0: Any?,
        sessionId: Long,
        date: String?,
    ) {
        if (context == null || d0 == null || date.isNullOrBlank()) return
        try {
            val clazz = d0.javaClass
            val gameId = clazz.getDeclaredField("a").apply { isAccessible = true }.getLong(d0)
            val gameName = clazz.getDeclaredField("b").apply { isAccessible = true }.get(d0) as String
            val platformId = clazz.getDeclaredField("c").apply { isAccessible = true }.get(d0) as String
            val emulator = clazz.getDeclaredField("d").apply { isAccessible = true }.get(d0) as String
            val durationMinutes = clazz.getDeclaredField("g").apply { isAccessible = true }.getInt(d0)
            FroglogRepository.get(context).enqueueCocoonGameSession(
                gameSessionId = sessionId,
                cocoonGameId = gameId,
                gameName = gameName,
                platformId = platformId,
                date = date,
                durationMinutes = durationMinutes,
                emulatorPackage = emulator,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to enqueue Froglog session", e)
        }
    }

    @JvmStatic
    fun bridge(context: Context): rip.moth.cocoonshell.froglog.bridge.FroglogBridge =
        FroglogRepository.get(context)

    @JvmStatic
    fun onGameSessionEnded(
        context: android.content.Context,
        gameSessionId: Long,
        cocoonGameId: Long,
        gameName: String,
        platformId: String,
        date: String,
        durationMinutes: Int,
        emulatorPackage: String?,
    ) {
        FroglogRepository.get(context).enqueueCocoonGameSession(
            gameSessionId = gameSessionId,
            cocoonGameId = cocoonGameId,
            gameName = gameName,
            platformId = platformId,
            date = date,
            durationMinutes = durationMinutes,
            emulatorPackage = emulatorPackage,
        )
    }

    /**
     * Called when Picnic saves a screenshot ([PicnicScreenshotDao_Impl]) or when the user shares from Picnic.
     */
    @JvmStatic
    fun onPicnicScreenshotSaved(context: Context?, record: Any?) {
        if (context == null || record == null) return
        try {
            val clazz = record.javaClass
            val id = clazz.getDeclaredField("id").apply { isAccessible = true }.getLong(record)
            if (id <= 0L) return
            val gameId = clazz.getDeclaredField("gameId").apply { isAccessible = true }.getLong(record)
            val gameName = clazz.getDeclaredField("gameName").apply { isAccessible = true }.get(record) as String
            val platformId = clazz.getDeclaredField("platformId").apply { isAccessible = true }.get(record) as String
            val screenshotUri = clazz.getDeclaredField("screenshotUri").apply { isAccessible = true }.get(record) as String
            val mimeType = runCatching {
                clazz.getDeclaredField("mimeType").apply { isAccessible = true }.get(record) as? String
            }.getOrNull()
            val capturedAt = clazz.getDeclaredField("capturedAt").apply { isAccessible = true }.getLong(record)
            FroglogRepository.get(context).enqueuePicnicScreenshot(
                picnicScreenshotId = id,
                cocoonGameId = gameId,
                gameName = gameName,
                platformId = platformId,
                screenshotUri = screenshotUri,
                mimeType = mimeType,
                capturedAtMillis = capturedAt,
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to enqueue Picnic screenshot for Froglog", e)
        }
    }
}
