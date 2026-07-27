package rip.moth.cocoonshell.froglog.api

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import rip.moth.cocoonshell.froglog.BuildConfig
import rip.moth.cocoonshell.froglog.LoginResult
import rip.moth.cocoonshell.froglog.PendingPlaySession
import java.util.concurrent.TimeUnit

class FroglogApi(
    private val client: OkHttpClient = defaultClient(),
) {
    private val json = "application/json; charset=utf-8".toMediaType()
    private val base = BuildConfig.FROGLOG_API_BASE.trimEnd('/')

    fun login(username: String, password: String): LoginResult {
        val body = JSONObject()
            .put("username", username)
            .put("password", password)
            .toString()
            .toRequestBody(json)
        val req = Request.Builder()
            .url("$base/auth/login")
            .post(body)
            .build()
        val (code, text) = execute(req)
        if (code == 401) error("Invalid credentials")
        if (code !in 200..299) error("Login failed ($code): $text")
        val obj = JSONObject(text)
        return LoginResult(
            token = obj.getString("token"),
            username = obj.getString("username"),
        )
    }

    fun register(username: String, password: String): LoginResult {
        val body = JSONObject()
            .put("username", username)
            .put("password", password)
            .toString()
            .toRequestBody(json)
        val req = Request.Builder()
            .url("$base/auth/register")
            .post(body)
            .build()
        val (code, text) = execute(req)
        if (code == 400) error("Username already taken")
        if (code !in 200..299) error("Register failed ($code): $text")
        val obj = JSONObject(text)
        return LoginResult(
            token = obj.getString("token"),
            username = obj.getString("username"),
        )
    }

    fun listGames(token: String): JSONArray {
        val req = Request.Builder()
            .url("$base/games")
            .header("Authorization", bearer(token))
            .get()
            .build()
        val (code, text) = execute(req)
        if (code == 401) error("Unauthorized")
        if (code !in 200..299) error("List games failed ($code)")
        return JSONArray(text)
    }

    fun createGame(token: String, title: String, platform: String?): Int {
        val body = JSONObject()
            .put("title", title)
            .put("session_tracking", true)
            .put("sessions_public", true)
        if (!platform.isNullOrBlank()) body.put("platform", platform)
        val req = Request.Builder()
            .url("$base/games")
            .header("Authorization", bearer(token))
            .post(body.toString().toRequestBody(json))
            .build()
        val (code, text) = execute(req)
        if (code == 429) error("Rate limited creating games")
        if (code !in 200..299) error("Create game failed ($code): $text")
        return JSONObject(text).getInt("id")
    }

    fun postSession(token: String, froglogGameId: Int, session: PendingPlaySession): Int {
        val body = JSONObject()
            .put("date", session.date)
            .put("hours", session.hours)
            .put("is_public", true)
            .put("spoiler", false)
            .put("sync_ref", session.clientSessionKey)
        val note = buildString {
            append("Cocoon")
            session.emulatorPackage?.let { append(" · $it") }
            session.notes?.let { append(" · $it") }
        }
        if (note.isNotBlank()) body.put("notes", note)

        val req = Request.Builder()
            .url("$base/games/$froglogGameId/sessions")
            .header("Authorization", bearer(token))
            .post(body.toString().toRequestBody(json))
            .build()
        val (code, text) = execute(req)
        if (code == 404) error("Game not found on Froglog")
        if (code !in 200..299) error("Post session failed ($code): $text")
        return JSONObject(text).getInt("id")
    }

    private fun execute(req: Request): Pair<Int, String> {
        client.newCall(req).execute().use { resp ->
            return resp.code to (resp.body?.string() ?: "")
        }
    }

    private fun bearer(token: String) = "Bearer $token"

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
