package rip.moth.cocoonshell.froglog.bridge

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rip.moth.cocoonshell.froglog.FroglogAuthState
import rip.moth.cocoonshell.froglog.FroglogNetwork
import rip.moth.cocoonshell.froglog.FroglogOutboxItem
import rip.moth.cocoonshell.froglog.FroglogOutboxKind
import rip.moth.cocoonshell.froglog.FroglogSyncState
import rip.moth.cocoonshell.froglog.PendingPicnicScreenshot
import rip.moth.cocoonshell.froglog.PendingPlaySession
import rip.moth.cocoonshell.froglog.api.FroglogApi
import rip.moth.cocoonshell.froglog.auth.FroglogAuthStore
import rip.moth.cocoonshell.froglog.data.FroglogQueueStore
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class FroglogRepository(
    context: Context,
    private val api: FroglogApi = FroglogApi(),
    private val auth: FroglogAuthStore = FroglogAuthStore(context),
    private val queue: FroglogQueueStore = FroglogQueueStore(context),
) : FroglogBridge {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _auth = MutableStateFlow(auth.authState())
    private val _sync = MutableStateFlow(buildSyncState())

    override val authState: StateFlow<FroglogAuthState> = _auth.asStateFlow()
    override val syncState: StateFlow<FroglogSyncState> = _sync.asStateFlow()

    override suspend fun login(username: String, password: String): Result<Unit> = runCatching {
        val result = api.login(username.trim(), password)
        auth.saveToken(result.token, result.username)
        refreshAuth()
        scheduleSync()
    }

    override suspend fun register(username: String, password: String): Result<Unit> = runCatching {
        val result = api.register(username.trim(), password)
        auth.saveToken(result.token, result.username)
        refreshAuth()
        scheduleSync()
    }

    override fun signOut() {
        auth.clear()
        refreshAuth()
    }

    override fun enqueueSession(session: PendingPlaySession) {
        queue.enqueue(session)
        refreshSync()
        scheduleSync()
    }

    override fun enqueueCocoonGameSession(
        gameSessionId: Long,
        cocoonGameId: Long,
        gameName: String,
        platformId: String,
        date: String,
        durationMinutes: Int,
        emulatorPackage: String?,
    ) {
        val hours = durationMinutes / 60.0
        if (hours <= 0.0) return
        enqueueSession(
            PendingPlaySession(
                clientSessionKey = "cocoon:session:$gameSessionId",
                cocoonGameId = cocoonGameId,
                gameName = gameName,
                platformId = platformId,
                date = date,
                hours = hours,
                emulatorPackage = emulatorPackage,
            ),
        )
    }

    fun enqueuePicnicScreenshot(
        picnicScreenshotId: Long,
        cocoonGameId: Long,
        gameName: String,
        platformId: String,
        screenshotUri: String,
        mimeType: String?,
        capturedAtMillis: Long,
    ) {
        if (!auth.authState().isSignedIn) return
        queue.enqueueScreenshot(
            PendingPicnicScreenshot(
                clientScreenshotKey = "cocoon:picnic:$picnicScreenshotId",
                picnicScreenshotId = picnicScreenshotId,
                cocoonGameId = cocoonGameId,
                gameName = gameName,
                platformId = platformId,
                screenshotUri = screenshotUri,
                mimeType = mimeType,
                capturedAtMillis = capturedAtMillis,
            ),
        )
        refreshSync()
        scheduleSync()
    }

    override suspend fun syncNow(): Result<Int> = runCatching {
        if (!FroglogNetwork.isOnline(app)) {
            refreshSync()
            error("offline")
        }
        val token = auth.token() ?: error("Not signed in")
        var uploaded = 0
        var lastError: String? = null
        for (session in queue.pendingSessions()) {
            try {
                val gameId = resolveFroglogGameId(token, session.cocoonGameId, session.gameName, session.platformId)
                api.postSession(token, gameId, session)
                queue.removeSession(session.clientSessionKey)
                uploaded++
            } catch (e: Exception) {
                queue.setItemError(session.clientSessionKey, e.message)
                lastError = e.message
            }
        }
        for (shot in queue.pendingScreenshots()) {
            try {
                uploadPicnicScreenshot(token, shot)
                queue.removeScreenshot(shot.clientScreenshotKey)
                uploaded++
            } catch (e: Exception) {
                queue.setItemError(shot.clientScreenshotKey, e.message)
                lastError = e.message
            }
        }
        queue.setLastSyncError(lastError)
        if (uploaded > 0) {
            queue.setLastSyncAt(System.currentTimeMillis())
        }
        refreshSync()
        uploaded
    }.onFailure { e ->
        if (e.message != "offline") {
            queue.setLastSyncError(e.message)
        }
        refreshSync()
    }

    fun outboxItems(): List<FroglogOutboxItem> {
        val sessions = queue.pendingSessions().map {
            FroglogOutboxItem(
                key = it.clientSessionKey,
                kind = FroglogOutboxKind.SESSION,
                title = it.gameName,
                subtitle = "${it.date} · ${"%.1f".format(Locale.US, it.hours)} h",
                error = it.lastError,
                cocoonGameId = it.cocoonGameId,
            )
        }
        val shots = queue.pendingScreenshots().map {
            FroglogOutboxItem(
                key = it.clientScreenshotKey,
                kind = FroglogOutboxKind.SCREENSHOT,
                title = it.gameName,
                subtitle = "Picnic screenshot",
                error = it.lastError,
                cocoonGameId = it.cocoonGameId,
            )
        }
        return sessions + shots
    }

    fun dismissOutboxItem(key: String) {
        if (key.startsWith("cocoon:picnic:")) {
            queue.removeScreenshot(key)
        } else {
            queue.removeSession(key)
        }
        refreshSync()
    }

    suspend fun retryOutboxItem(key: String): Result<Unit> = runCatching {
        if (!FroglogNetwork.isOnline(app)) error("offline")
        val token = auth.token() ?: error("Not signed in")
        if (key.startsWith("cocoon:picnic:")) {
            val shot = queue.pendingScreenshots().firstOrNull { it.clientScreenshotKey == key }
                ?: error("Item not found")
            uploadPicnicScreenshot(token, shot)
            queue.removeScreenshot(key)
        } else {
            val session = queue.pendingSessions().firstOrNull { it.clientSessionKey == key }
                ?: error("Item not found")
            val gameId = resolveFroglogGameId(token, session.cocoonGameId, session.gameName, session.platformId)
            api.postSession(token, gameId, session)
            queue.removeSession(key)
        }
        queue.clearItemError(key)
        refreshSync()
    }

    fun linkedFroglogGameId(cocoonGameId: Long, gameTitle: String): Int? {
        queue.gameLink(cocoonGameId)?.let { return it }
        return queue.gameLinkByTitle(gameTitle)
    }

    suspend fun searchFroglogGames(query: String): JSONArray = runCatching {
        val token = auth.token() ?: return JSONArray()
        api.searchGames(token, query)
    }.getOrElse { JSONArray() }

    suspend fun linkGame(cocoonGameId: Long, gameTitle: String, froglogGameId: Int) {
        if (cocoonGameId > 0L) {
            queue.saveGameLink(cocoonGameId, froglogGameId)
        }
        queue.saveGameLinkByTitle(gameTitle, froglogGameId)
    }

    suspend fun createFroglogGameAndLink(
        cocoonGameId: Long,
        gameTitle: String,
        platformId: String,
    ): Int {
        val token = auth.token() ?: error("Not signed in")
        val id = api.createGame(token, gameTitle, platformId)
        linkGame(cocoonGameId, gameTitle, id)
        return id
    }

    fun gameWebUrl(froglogGameId: Int): String =
        "${rip.moth.cocoonshell.froglog.BuildConfig.FROGLOG_WEB_BASE.trimEnd('/')}/games/$froglogGameId"

    override fun enqueueScreenshot(cocoonScreenshotId: Long): Result<Unit> =
        Result.failure(UnsupportedOperationException("Load screenshot record from Picnic first"))

    internal fun refreshAuth() {
        _auth.value = auth.authState()
    }

    internal fun refreshSync() {
        _sync.value = buildSyncState()
    }

    private fun buildSyncState(): FroglogSyncState {
        val sessionList = queue.pendingSessions()
        val shotList = queue.pendingScreenshots()
        val sessions = sessionList.size
        val shots = shotList.size
        val errors = sessionList.count { !it.lastError.isNullOrBlank() } +
            shotList.count { !it.lastError.isNullOrBlank() }
        val offline = !FroglogNetwork.isOnline(app)
        val pending = sessions + shots
        val statusLine = when {
            !auth.authState().isSignedIn && pending > 0 -> null
            offline && pending > 0 -> "queued_offline"
            pending > 0 && errors > 0 -> "pending_with_errors"
            pending > 0 -> "queued"
            errors > 0 -> "errors"
            else -> null
        }
        return FroglogSyncState(
            pendingCount = pending,
            pendingSessionCount = sessions,
            pendingScreenshotCount = shots,
            errorCount = errors,
            isOffline = offline,
            statusLine = statusLine,
            lastSyncError = queue.lastSyncError(),
            lastSyncAtMillis = queue.lastSyncAt().takeIf { it > 0L },
        )
    }

    private fun resolveFroglogGameId(
        token: String,
        cocoonGameId: Long,
        gameName: String,
        platformId: String,
    ): Int {
        queue.gameLink(cocoonGameId)?.let { return it }
        queue.gameLinkByTitle(gameName)?.let {
            if (cocoonGameId > 0L) queue.saveGameLink(cocoonGameId, it)
            return it
        }
        val games = api.listGames(token)
        for (i in 0 until games.length()) {
            val g = games.getJSONObject(i)
            if (titlesMatch(g.getString("title"), gameName)) {
                val id = g.getInt("id")
                queue.saveGameLink(cocoonGameId, id)
                return id
            }
        }
        val created = api.createGame(token, gameName, platformId)
        queue.saveGameLink(cocoonGameId, created)
        return created
    }

    private fun resolveFroglogGameId(token: String, session: PendingPlaySession): Int =
        resolveFroglogGameId(token, session.cocoonGameId, session.gameName, session.platformId)

    private fun uploadPicnicScreenshot(token: String, shot: PendingPicnicScreenshot) {
        val gameId = resolveFroglogGameId(token, shot.cocoonGameId, shot.gameName, shot.platformId)
        val bytes = readScreenshotBytes(shot.screenshotUri)
        val mime = shot.mimeType?.takeIf { it.isNotBlank() } ?: "image/jpeg"
        val temp = File.createTempFile("froglog-picnic-", extensionForMime(mime), app.cacheDir)
        try {
            temp.writeBytes(bytes)
            try {
                api.postGameScreenshot(
                    token = token,
                    froglogGameId = gameId,
                    imageFile = temp,
                    mimeType = mime,
                    syncRef = shot.clientScreenshotKey,
                    caption = "Picnic · ${shot.gameName}",
                )
            } catch (uploadEx: Exception) {
                val session = PendingPlaySession(
                    clientSessionKey = shot.clientScreenshotKey,
                    cocoonGameId = shot.cocoonGameId,
                    gameName = shot.gameName,
                    platformId = shot.platformId,
                    date = formatDate(shot.capturedAtMillis),
                    hours = 0.02,
                    notes = "Picnic screenshot (${shot.screenshotUri}) — ${uploadEx.message}",
                )
                api.postSession(token, gameId, session)
            }
        } finally {
            temp.delete()
        }
    }

    private fun readScreenshotBytes(uriString: String): ByteArray {
        val uri = Uri.parse(uriString)
        app.contentResolver.openInputStream(uri)?.use { input ->
            return input.readBytes()
        }
        throw IllegalStateException("Could not read screenshot: $uriString")
    }

    private fun extensionForMime(mime: String): String = when {
        mime.contains("png", ignoreCase = true) -> ".png"
        mime.contains("webp", ignoreCase = true) -> ".webp"
        else -> ".jpg"
    }

    private fun formatDate(millis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        fmt.timeZone = TimeZone.getDefault()
        return fmt.format(Date(millis))
    }

    private fun titlesMatch(a: String, b: String): Boolean =
        a.trim().equals(b.trim(), ignoreCase = true)

    private fun scheduleSync() {
        if (!auth.authState().isSignedIn) return
        scope.launch {
            syncNow()
        }
    }

    fun scheduleSyncAfterPicnicSubmit() {
        scheduleSync()
    }

    fun refreshConnectivity() {
        refreshSync()
    }

    companion object {
        @Volatile
        private var instance: FroglogRepository? = null

        fun get(context: Context): FroglogRepository {
            return instance ?: synchronized(this) {
                instance ?: FroglogRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
