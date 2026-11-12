package io.github.akrishna87.daybook.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.akrishna87.daybook.SpeechToText

/**
 * Speech to text: returns a function that opens the phone's speech recogniser with [prompt] and
 * hands what was said to [onText]. Says so in the snackbar when the phone has no recogniser.
 */
@Composable
fun rememberSpeech(prompt: String, onText: (String) -> Unit): () -> Unit {
    val actions = LocalActions.current
    val latest = rememberUpdatedState(onText)
    val launcher = rememberLauncherForActivityResult(SpeechToText()) { text -> if (text != null) latest.value(text) }
    return {
        if (!SpeechToText.launch(launcher, prompt)) {
            actions.message("Voice input needs a speech app, such as Google, on this phone")
        }
    }
}

/** A microphone button that listens and hands back what was said. */
@Composable
fun MicButton(label: String, prompt: String, modifier: Modifier = Modifier, onText: (String) -> Unit) {
    val listen = rememberSpeech(prompt, onText)
    IconButton(onClick = listen, modifier.semantics { contentDescription = label }) {
        Icon(Icons.Rounded.Mic, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
