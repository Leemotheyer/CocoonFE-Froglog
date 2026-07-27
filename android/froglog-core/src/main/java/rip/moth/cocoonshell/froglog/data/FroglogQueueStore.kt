package rip.moth.cocoonshell.froglog.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import rip.moth.cocoonshell.froglog.PendingPicnicScreenshot
import rip.moth.cocoonshell.froglog.PendingPlaySession

class FroglogQueueStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun pendingSessions(): List<PendingPlaySession> {
        val raw = prefs.getString(KEY_QUEUE, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return buildList {
            for (i in 0 until arr.length()) {
                add(arr.getJSONObject(i).toSession())
            }
        }
    }

    fun pendingScreenshots(): List<PendingPicnicScreenshot> {
        val raw = prefs.getString(KEY_PICNIC_QUEUE, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return buildList {
            for (i in 0 until arr.length()) {
                add(arr.getJSONObject(i).toScreenshot())
            }
        }
    }

    fun enqueue(session: PendingPlaySession) {
        val list = pendingSessions().toMutableList()
        if (list.any { it.clientSessionKey == session.clientSessionKey }) return
        list.add(session)
        saveSessions(list)
    }

    fun enqueueScreenshot(screenshot: PendingPicnicScreenshot) {
        val list = pendingScreenshots().toMutableList()
        if (list.any { it.clientScreenshotKey == screenshot.clientScreenshotKey }) return
        list.add(screenshot)
        saveScreenshots(list)
    }

    fun removeSession(clientSessionKey: String) {
        saveSessions(pendingSessions().filterNot { it.clientSessionKey == clientSessionKey })
    }

    fun removeScreenshot(clientScreenshotKey: String) {
        saveScreenshots(pendingScreenshots().filterNot { it.clientScreenshotKey == clientScreenshotKey })
    }

    fun saveSessions(sessions: List<PendingPlaySession>) {
        val arr = JSONArray()
        sessions.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_QUEUE, arr.toString()).apply()
    }

    fun saveScreenshots(screenshots: List<PendingPicnicScreenshot>) {
        val arr = JSONArray()
        screenshots.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_PICNIC_QUEUE, arr.toString()).apply()
    }

    fun gameLink(cocoonGameId: Long): Int? {
        val key = prefs.getInt(linkKey(cocoonGameId), -1)
        return if (key > 0) key else null
    }

    fun saveGameLink(cocoonGameId: Long, froglogGameId: Int) {
        prefs.edit().putInt(linkKey(cocoonGameId), froglogGameId).apply()
    }

    fun gameLinkByTitle(title: String): Int? {
        val key = prefs.getInt(titleLinkKey(title), -1)
        return if (key > 0) key else null
    }

    fun saveGameLinkByTitle(title: String, froglogGameId: Int) {
        prefs.edit().putInt(titleLinkKey(title), froglogGameId).apply()
    }

    fun clearGameLink(cocoonGameId: Long) {
        prefs.edit().remove(linkKey(cocoonGameId)).apply()
    }

    fun lastSyncError(): String? = prefs.getString(KEY_LAST_ERROR, null)

    fun setLastSyncError(msg: String?) {
        prefs.edit().putString(KEY_LAST_ERROR, msg).apply()
    }

    fun lastSyncAt(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    fun setLastSyncAt(millis: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, millis).apply()
    }

    private fun linkKey(cocoonGameId: Long) = "link_$cocoonGameId"

    private fun titleLinkKey(title: String) = "link_t_${title.trim().lowercase()}"

    fun setItemError(clientKey: String, error: String?) {
        if (clientKey.startsWith("cocoon:picnic:")) {
            val list = pendingScreenshots().map {
                if (it.clientScreenshotKey == clientKey) it.copy(lastError = error) else it
            }
            saveScreenshots(list)
        } else {
            val list = pendingSessions().map {
                if (it.clientSessionKey == clientKey) it.copy(lastError = error) else it
            }
            saveSessions(list)
        }
    }

    fun clearItemError(clientKey: String) = setItemError(clientKey, null)

    private fun PendingPlaySession.toJson(): JSONObject = JSONObject()
        .put("clientSessionKey", clientSessionKey)
        .put("cocoonGameId", cocoonGameId)
        .put("gameName", gameName)
        .put("platformId", platformId)
        .put("date", date)
        .put("hours", hours)
        .put("notes", notes)
        .put("emulatorPackage", emulatorPackage)
        .put("lastError", lastError)

    private fun JSONObject.toSession(): PendingPlaySession = PendingPlaySession(
        clientSessionKey = getString("clientSessionKey"),
        cocoonGameId = getLong("cocoonGameId"),
        gameName = getString("gameName"),
        platformId = getString("platformId"),
        date = getString("date"),
        hours = getDouble("hours"),
        notes = if (has("notes") && !isNull("notes")) getString("notes") else null,
        emulatorPackage = if (has("emulatorPackage") && !isNull("emulatorPackage")) {
            getString("emulatorPackage")
        } else {
            null
        },
        lastError = if (has("lastError") && !isNull("lastError")) getString("lastError") else null,
    )

    private fun PendingPicnicScreenshot.toJson(): JSONObject = JSONObject()
        .put("clientScreenshotKey", clientScreenshotKey)
        .put("picnicScreenshotId", picnicScreenshotId)
        .put("cocoonGameId", cocoonGameId)
        .put("gameName", gameName)
        .put("platformId", platformId)
        .put("screenshotUri", screenshotUri)
        .put("mimeType", mimeType)
        .put("capturedAtMillis", capturedAtMillis)
        .put("lastError", lastError)

    private fun JSONObject.toScreenshot(): PendingPicnicScreenshot = PendingPicnicScreenshot(
        clientScreenshotKey = getString("clientScreenshotKey"),
        picnicScreenshotId = getLong("picnicScreenshotId"),
        cocoonGameId = getLong("cocoonGameId"),
        gameName = getString("gameName"),
        platformId = getString("platformId"),
        screenshotUri = getString("screenshotUri"),
        mimeType = if (has("mimeType") && !isNull("mimeType")) getString("mimeType") else null,
        capturedAtMillis = getLong("capturedAtMillis"),
        lastError = if (has("lastError") && !isNull("lastError")) getString("lastError") else null,
    )

    companion object {
        private const val PREFS = "froglog_queue"
        private const val KEY_QUEUE = "pending"
        private const val KEY_PICNIC_QUEUE = "pending_picnic"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_LAST_SYNC = "last_sync"
    }
}
