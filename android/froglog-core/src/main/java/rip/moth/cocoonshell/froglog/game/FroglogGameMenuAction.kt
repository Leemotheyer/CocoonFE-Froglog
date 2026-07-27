package rip.moth.cocoonshell.froglog.game

import android.content.Context
import rip.moth.cocoonshell.froglog.game.FroglogGameLinkActivity

/** Game start menu action (patched smali). */
class FroglogGameMenuAction(
    private val context: Context,
    private val gameTitle: String,
) : () -> Unit {
    override fun invoke() {
        if (gameTitle.isBlank()) return
        FroglogGameLinkActivity.launch(context, gameTitle, "", 0L)
    }
}
