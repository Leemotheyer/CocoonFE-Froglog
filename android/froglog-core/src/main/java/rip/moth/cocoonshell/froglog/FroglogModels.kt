package rip.moth.cocoonshell.froglog

data class FroglogAuthState(
    val isSignedIn: Boolean,
    val username: String?,
)

data class FroglogSyncState(
    val pendingCount: Int,
    val pendingSessionCount: Int = pendingCount,
    val pendingScreenshotCount: Int = 0,
    val lastSyncError: String?,
    val lastSyncAtMillis: Long?,
)

data class PendingPlaySession(
    val clientSessionKey: String,
    val cocoonGameId: Long,
    val gameName: String,
    val platformId: String,
    val date: String,
    val hours: Double,
    val notes: String? = null,
    val emulatorPackage: String? = null,
)

data class PendingPicnicScreenshot(
    val clientScreenshotKey: String,
    val picnicScreenshotId: Long,
    val cocoonGameId: Long,
    val gameName: String,
    val platformId: String,
    val screenshotUri: String,
    val mimeType: String?,
    val capturedAtMillis: Long,
)

data class FroglogGameLink(
    val cocoonGameId: Long,
    val froglogGameId: Int,
    val isLiveService: Boolean = false,
)

data class LoginResult(
    val token: String,
    val username: String,
)
