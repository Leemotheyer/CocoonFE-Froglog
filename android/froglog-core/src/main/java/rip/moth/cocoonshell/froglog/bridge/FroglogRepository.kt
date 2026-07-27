package rip.moth.cocoonshell.froglog.bridge

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import rip.moth.cocoonshell.froglog.FroglogAuthState
import rip.moth.cocoonshell.froglog.FroglogSyncState
import rip.moth.cocoonshell.froglog.PendingPlaySession
import rip.moth.cocoonshell.froglog.api.FroglogApi
import rip.moth.cocoonshell.froglog.auth.FroglogAuthStore
import rip.moth.cocoonshell.froglog.data.FroglogQueueStore

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

    override suspend fun syncNow(): Result<Int> = runCatching {
        val token = auth.token() ?: error("Not signed in")
        var uploaded = 0
        for (session in queue.pending()) {
            val gameId = resolveFroglogGameId(token, session)
            api.postSession(token, gameId, session)
            queue.remove(session.clientSessionKey)
            uploaded++
        }
        queue.setLastSyncError(null)
        queue.setLastSyncAt(System.currentTimeMillis())
        refreshSync()
        uploaded
    }.onFailure { e ->
        queue.setLastSyncError(e.message)
        refreshSync()
    }

    override fun enqueueScreenshot(cocoonScreenshotId: Long): Result<Unit> =
        Result.failure(UnsupportedOperationException("Froglog screenshot API not available yet"))

    internal fun refreshAuth() {
        _auth.value = auth.authState()
    }

    internal fun refreshSync() {
        _sync.value = buildSyncState()
    }

    private fun buildSyncState() = FroglogSyncState(
        pendingCount = queue.pending().size,
        lastSyncError = queue.lastSyncError(),
        lastSyncAtMillis = queue.lastSyncAt().takeIf { it > 0L },
    )

    private fun resolveFroglogGameId(token: String, session: PendingPlaySession): Int {
        queue.gameLink(session.cocoonGameId)?.let { return it }
        val games = api.listGames(token)
        for (i in 0 until games.length()) {
            val g = games.getJSONObject(i)
            if (titlesMatch(g.getString("title"), session.gameName)) {
                val id = g.getInt("id")
                queue.saveGameLink(session.cocoonGameId, id)
                return id
            }
        }
        val created = api.createGame(token, session.gameName, session.platformId)
        queue.saveGameLink(session.cocoonGameId, created)
        return created
    }

    private fun titlesMatch(a: String, b: String): Boolean =
        a.trim().equals(b.trim(), ignoreCase = true)

    private fun scheduleSync() {
        if (!auth.authState().isSignedIn) return
        scope.launch {
            syncNow()
        }
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
