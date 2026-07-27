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
}
