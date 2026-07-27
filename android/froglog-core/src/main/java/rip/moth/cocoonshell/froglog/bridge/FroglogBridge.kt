package rip.moth.cocoonshell.froglog.bridge

import kotlinx.coroutines.flow.StateFlow
import rip.moth.cocoonshell.froglog.FroglogAuthState
import rip.moth.cocoonshell.froglog.FroglogSyncState
import rip.moth.cocoonshell.froglog.PendingPlaySession

interface FroglogBridge {
    val authState: StateFlow<FroglogAuthState>
    val syncState: StateFlow<FroglogSyncState>

    suspend fun login(username: String, password: String): Result<Unit>
    suspend fun register(username: String, password: String): Result<Unit>
    fun signOut()

    fun enqueueSession(session: PendingPlaySession)
    fun enqueueCocoonGameSession(
        gameSessionId: Long,
        cocoonGameId: Long,
        gameName: String,
        platformId: String,
        date: String,
        durationMinutes: Int,
        emulatorPackage: String?,
    )

    suspend fun syncNow(): Result<Int>

    fun enqueueScreenshot(cocoonScreenshotId: Long): Result<Unit>
}
