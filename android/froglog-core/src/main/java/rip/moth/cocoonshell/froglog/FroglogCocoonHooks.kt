package rip.moth.cocoonshell.froglog

import rip.moth.cocoonshell.froglog.bridge.FroglogBridge
import rip.moth.cocoonshell.froglog.bridge.FroglogRepository

/**
 * Entry point for decompiled Cocoon code (e.g. after session insert).
 *
 * From Java:
 * ```java
 * FroglogCocoonHooks.onGameSessionEnded(context, session);
 * ```
 */
object FroglogCocoonHooks {
  @JvmStatic
  fun bridge(context: android.content.Context): FroglogBridge =
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
