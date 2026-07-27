package rip.moth.cocoonshell.froglog.picnic

import android.content.Context
import android.widget.Toast
import rip.moth.cocoonshell.froglog.R
import rip.moth.cocoonshell.froglog.bridge.FroglogRepository

object FroglogPicnicUi {
  /** @param picnicUa Picnic list item (`ke/ua`) holding `PicnicScreenshotRecord` in field `a`. */
  @JvmStatic
  fun submitFromPicnicUa(context: Context, picnicUa: Any) {
    val repo = FroglogRepository.get(context)
    if (!repo.authState.value.isSignedIn) {
      Toast.makeText(context, context.getString(R.string.froglog_sign_in_required), Toast.LENGTH_SHORT).show()
      return
    }
    try {
      val record = picnicUa.javaClass.getDeclaredField("a").apply { isAccessible = true }.get(picnicUa)
        ?: return
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
      repo.enqueuePicnicScreenshot(
        picnicScreenshotId = id,
        cocoonGameId = gameId,
        gameName = gameName,
        platformId = platformId,
        screenshotUri = screenshotUri,
        mimeType = mimeType,
        capturedAtMillis = capturedAt,
      )
      repo.scheduleSyncAfterPicnicSubmit()
      Toast.makeText(context, context.getString(R.string.picnic_froglog_queued), Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
      Toast.makeText(context, context.getString(R.string.froglog_error_generic), Toast.LENGTH_SHORT).show()
    }
  }
}
