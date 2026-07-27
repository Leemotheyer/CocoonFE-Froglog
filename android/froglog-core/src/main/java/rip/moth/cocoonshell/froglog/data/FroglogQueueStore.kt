package rip.moth.cocoonshell.froglog.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import rip.moth.cocoonshell.froglog.PendingPlaySession

class FroglogQueueStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun pending(): List<PendingPlaySession> {
        val raw = prefs.getString(KEY_QUEUE, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return buildList {
            for (i in 0 until arr.length()) {
                add(arr.getJSONObject(i).toSession())
            }
        }
    }

    fun enqueue(session: PendingPlaySession) {
        val list = pending().toMutableList()
        if (list.any { it.clientSessionKey == session.clientSessionKey }) return
        list.add(session)
        save(list)
    }

    fun remove(clientSessionKey: String) {
        save(pending().filterNot { it.clientSessionKey == clientSessionKey })
    }

    fun save(sessions: List<PendingPlaySession>) {
        val arr = JSONArray()
        sessions.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_QUEUE, arr.toString()).apply()
    }

    fun gameLink(cocoonGameId: Long): Int? {
        val key = prefs.getInt(linkKey(cocoonGameId), -1)
        return if (key > 0) key else null
    }

    fun saveGameLink(cocoonGameId: Long, froglogGameId: Int) {
        prefs.edit().putInt(linkKey(cocoonGameId), froglogGameId).apply()
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

    private fun PendingPlaySession.toJson(): JSONObject = JSONObject()
        .put("clientSessionKey", clientSessionKey)
        .put("cocoonGameId", cocoonGameId)
        .put("gameName", gameName)
        .put("platformId", platformId)
        .put("date", date)
        .put("hours", hours)
        .put("notes", notes)
        .put("emulatorPackage", emulatorPackage)

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
    )

    companion object {
        private const val PREFS = "froglog_queue"
        private const val KEY_QUEUE = "pending"
        private const val KEY_LAST_ERROR = "last_error"
        private const val KEY_LAST_SYNC = "last_sync"
    }
}
