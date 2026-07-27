package rip.moth.cocoonshell.froglog.picnic

import android.content.Context
import android.widget.Toast
import rip.moth.cocoonshell.froglog.R

/** Picnic gallery batch selection (patched smali). */
class FroglogPicnicBatchSubmitAction(
    private val context: Context,
    private val selected: List<*>,
) : () -> Unit {
    override fun invoke() {
        if (selected.isEmpty()) return
        var queued = 0
        for (item in selected) {
            val ua = item ?: continue
            FroglogPicnicUi.submitFromPicnicUa(context, ua, showToast = false)
            queued++
        }
        if (queued > 1) {
            Toast.makeText(
                context,
                context.getString(R.string.picnic_froglog_batch_queued, queued),
                Toast.LENGTH_SHORT,
            ).show()
        }
    }
}
