package rip.moth.cocoonshell.froglog.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import rip.moth.cocoonshell.froglog.bridge.FroglogRepository

class FroglogSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repo = FroglogRepository.get(applicationContext)
        if (!repo.authState.value.isSignedIn) return Result.success()
        return repo.syncNow().fold(
            onSuccess = { Result.success() },
            onFailure = {
                if (runAttemptCount < 5) Result.retry() else Result.failure()
            },
        )
    }

    companion object {
        const val UNIQUE_NAME = "froglog-sync"
    }
}
