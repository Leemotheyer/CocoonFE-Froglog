package rip.moth.cocoonshell.froglog.game

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import rip.moth.cocoonshell.froglog.R
import rip.moth.cocoonshell.froglog.bridge.FroglogRepository
import rip.moth.cocoonshell.froglog.pod.FroglogCocoonTheme
import rip.moth.cocoonshell.froglog.pod.FroglogPodChrome
import rip.moth.cocoonshell.froglog.pod.FroglogPodLauncher

class FroglogGameLinkActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FroglogPodChrome.apply(this)
        val repo = FroglogRepository.get(this)
        val title = intent.getStringExtra(EXTRA_GAME_TITLE).orEmpty()
        val platform = intent.getStringExtra(EXTRA_PLATFORM_ID).orEmpty()
        val cocoonGameId = intent.getLongExtra(EXTRA_COCOON_GAME_ID, 0L)

        setContent {
            val auth by repo.authState.collectAsStateWithLifecycle()
            FroglogCocoonTheme {
                GameLinkContent(
                    gameTitle = title,
                    platformId = platform,
                    cocoonGameId = cocoonGameId,
                    isSignedIn = auth.isSignedIn,
                    onBack = { finish() },
                    onNeedSignIn = {
                        FroglogPodLauncher.open(this)
                        finish()
                    },
                    repo = repo,
                )
            }
        }
    }

    companion object {
        const val EXTRA_GAME_TITLE = "froglog_game_title"
        const val EXTRA_PLATFORM_ID = "froglog_platform_id"
        const val EXTRA_COCOON_GAME_ID = "froglog_cocoon_game_id"

        @JvmStatic
        fun launch(context: Context, gameTitle: String, platformId: String, cocoonGameId: Long) {
            val intent = Intent(context, FroglogGameLinkActivity::class.java).apply {
                putExtra(EXTRA_GAME_TITLE, gameTitle)
                putExtra(EXTRA_PLATFORM_ID, platformId)
                putExtra(EXTRA_COCOON_GAME_ID, cocoonGameId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameLinkContent(
    gameTitle: String,
    platformId: String,
    cocoonGameId: Long,
    isSignedIn: Boolean,
    onBack: () -> Unit,
    onNeedSignIn: () -> Unit,
    repo: FroglogRepository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableStateOf("loading") }
    var matches by rememberSaveable { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var busy by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(gameTitle, isSignedIn) {
        if (!isSignedIn) {
            step = "sign_in"
            return@LaunchedEffect
        }
        val linked = repo.linkedFroglogGameId(cocoonGameId, gameTitle)
        if (linked != null) {
            FroglogWeb.open(context, repo.gameWebUrl(linked))
            onBack()
            return@LaunchedEffect
        }
        step = "choose"
        val arr = repo.searchFroglogGames(gameTitle)
        matches = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optInt("id", o.optInt("game_id", -1))
                val t = o.optString("title", o.optString("name", ""))
                if (id > 0 && t.isNotBlank()) add(id to t)
            }
        }
    }

    fun openGame(id: Int) {
        FroglogWeb.open(context, repo.gameWebUrl(id))
        onBack()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.froglog_game_link_title), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 20.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = gameTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            if (platformId.isNotBlank()) {
                Text(
                    text = platformId,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            when (step) {
                "loading" -> CircularProgressIndicator()
                "sign_in" -> {
                    Text(stringResource(R.string.froglog_sign_in_required))
                    Button(onClick = onNeedSignIn, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.froglog_open_froglog_pod))
                    }
                }
                else -> {
                    Text(stringResource(R.string.froglog_game_link_prompt))
                    if (matches.isNotEmpty()) {
                        Text(stringResource(R.string.froglog_game_link_matches), fontWeight = FontWeight.Medium)
                        matches.forEach { (id, name) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !busy) {
                                        busy = true
                                        scope.launch {
                                            runCatching { repo.linkGame(cocoonGameId, gameTitle, id) }
                                                .onFailure {
                                                    Toast.makeText(context, R.string.froglog_error_generic, Toast.LENGTH_SHORT).show()
                                                }
                                                .onSuccess { openGame(id) }
                                            busy = false
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                ),
                            ) {
                                Text(name, modifier = Modifier.padding(14.dp))
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            busy = true
                            scope.launch {
                                runCatching {
                                    val id = repo.createFroglogGameAndLink(cocoonGameId, gameTitle, platformId)
                                    openGame(id)
                                }.onFailure {
                                    Toast.makeText(context, R.string.froglog_error_generic, Toast.LENGTH_SHORT).show()
                                }
                                busy = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !busy,
                    ) {
                        Text(stringResource(R.string.froglog_game_link_create))
                    }
                }
            }
        }
    }
}
