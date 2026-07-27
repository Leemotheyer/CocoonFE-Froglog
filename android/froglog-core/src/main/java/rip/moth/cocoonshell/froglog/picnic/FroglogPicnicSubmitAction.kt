package rip.moth.cocoonshell.froglog.picnic

import android.content.Context

/** Compose `onClick` for Picnic screenshot detail (patched smali). */
class FroglogPicnicSubmitAction(
  private val context: Context,
  private val picnicUa: Any,
) : () -> Unit {
  override fun invoke() {
    FroglogPicnicUi.submitFromPicnicUa(context, picnicUa)
  }
}
