package com.silifton.textwright.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.silifton.textwright.security.AppLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Shows the system fingerprint prompt with the given title and calls back on success. */
typealias Authenticate = (title: String, onSuccess: () -> Unit) -> Unit

/** Covers the whole app while it is locked. */
@Composable
fun LockScreen(authenticate: Authenticate) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Textwright", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            VerifyPattern("Draw your pattern to unlock", onVerified = AppLock::unlock)
            if (remember { AppLock.biometricEnabled(context) }) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { authenticate("Unlock Textwright", AppLock::unlock) }) { Text("Use fingerprint") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockSettingsScreen(vm: MainViewModel, authenticate: Authenticate) {
    val context = LocalContext.current
    var hasPattern by remember { mutableStateOf(AppLock.hasPattern(context)) }
    var fingerprint by remember { mutableStateOf(AppLock.biometricEnabled(context)) }
    var step by remember { mutableStateOf<LockStep?>(null) }
    var mismatch by remember { mutableStateOf(false) }
    val canUseBiometric = remember { AppLock.canUseBiometric(context) }

    BackHandler(enabled = step != null) { step = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App lock") },
                navigationIcon = { BackButton { if (step != null) step = null else vm.back() } },
            )
        },
    ) { padding ->
        val current = step
        if (current == null) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (!hasPattern) {
                    Column(Modifier.padding(24.dp)) {
                        Text(
                            "Ask for a pattern every time Textwright is opened. " +
                                "Once a pattern is set you can also unlock with your fingerprint.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "A forgotten pattern can't be recovered. The only way back in is to clear the app's " +
                                "data, which also loses the saved originals of edited messages.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { mismatch = false; step = LockStep.Draw }) { Text("Set a pattern") }
                    }
                } else {
                    ListItem(
                        headlineContent = { Text("Unlock with fingerprint") },
                        supportingContent = {
                            Text(
                                if (canUseBiometric) "The pattern still works as a fallback"
                                else "Add a fingerprint in the phone's settings first"
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = fingerprint,
                                enabled = canUseBiometric,
                                onCheckedChange = { on ->
                                    if (on) {
                                        authenticate("Confirm your fingerprint") {
                                            AppLock.setBiometricEnabled(context, true)
                                            fingerprint = true
                                        }
                                    } else {
                                        AppLock.setBiometricEnabled(context, false)
                                        fingerprint = false
                                    }
                                },
                            )
                        },
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Change pattern") },
                        modifier = Modifier.clickable { step = LockStep.Verify(remove = false) },
                    )
                    ListItem(
                        headlineContent = { Text("Turn off app lock") },
                        modifier = Modifier.clickable { step = LockStep.Verify(remove = true) },
                    )
                }
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (current) {
                is LockStep.Verify -> VerifyPattern("Draw your current pattern") {
                    if (current.remove) {
                        AppLock.clear(context)
                        hasPattern = false
                        fingerprint = false
                        step = null
                    } else {
                        mismatch = false
                        step = LockStep.Draw
                    }
                }
                LockStep.Draw -> {
                    var short by remember { mutableStateOf(false) }
                    PatternPrompt(
                        when {
                            short -> "Connect at least ${AppLock.MIN_DOTS} dots"
                            mismatch -> "The patterns didn't match. Draw a new pattern"
                            else -> "Draw a new pattern"
                        },
                        error = short || mismatch,
                    )
                    PatternGrid(
                        error = short,
                        onStart = { short = false; mismatch = false },
                        onPattern = { if (it.size < AppLock.MIN_DOTS) short = true else step = LockStep.Confirm(it) },
                    )
                }
                is LockStep.Confirm -> {
                    PatternPrompt("Draw the pattern again to confirm", error = false)
                    // Keyed so the dots from the first drawing don't carry over.
                    key(current) {
                        PatternGrid(onPattern = {
                            if (it == current.first) {
                                AppLock.setPattern(context, it)
                                hasPattern = true
                                step = null
                            } else {
                                mismatch = true
                                step = LockStep.Draw
                            }
                        })
                    }
                }
            }
        }
    }
}

private sealed interface LockStep {
    data class Verify(val remove: Boolean) : LockStep
    data object Draw : LockStep
    data class Confirm(val first: List<Int>) : LockStep
}

/** Asks for the saved pattern and calls [onVerified] when it matches. Too many wrong attempts force a wait. */
@Composable
private fun VerifyPattern(prompt: String, onVerified: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var wrong by remember { mutableStateOf(false) }
    var retryAt by remember { mutableStateOf(AppLock.retryAt(context)) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(retryAt) {
        while (true) {
            now = System.currentTimeMillis()
            if (now >= retryAt) break
            delay(250)
        }
    }
    val waiting = retryAt > now

    PatternPrompt(
        when {
            waiting -> "Too many attempts. Try again in ${(retryAt - now + 999) / 1000} s"
            wrong -> "Wrong pattern. Try again"
            else -> prompt
        },
        error = waiting || wrong,
    )
    PatternGrid(
        enabled = !waiting,
        error = wrong,
        onStart = { wrong = false },
        onPattern = { pattern ->
            scope.launch {
                if (withContext(Dispatchers.Default) { AppLock.verify(context, pattern) }) {
                    onVerified()
                } else {
                    wrong = true
                    retryAt = AppLock.retryAt(context)
                }
            }
        },
    )
}

@Composable
private fun PatternPrompt(text: String, error: Boolean) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(24.dp))
}

/**
 * A 3x3 pattern pad. Dots are numbered 0-8, row by row. [onPattern] gets the dots in the order they were
 * joined once the finger lifts; the drawing stays visible until the next touch.
 */
@Composable
private fun PatternGrid(
    onPattern: (List<Int>) -> Unit,
    enabled: Boolean = true,
    error: Boolean = false,
    onStart: () -> Unit = {},
) {
    var dots by remember { mutableStateOf(emptyList<Int>()) }
    var finger by remember { mutableStateOf<Offset?>(null) }
    val colors = MaterialTheme.colorScheme
    val active = if (error) colors.error else colors.primary
    val idle = colors.outline

    Canvas(
        Modifier.size(264.dp).pointerInput(enabled) {
            if (!enabled) return@pointerInput
            val cell = size.width / 3f
            fun centre(dot: Int) = Offset((dot % 3 + 0.5f) * cell, (dot / 3 + 0.5f) * cell)
            fun visit(position: Offset) {
                finger = position
                val dot = (0..8).firstOrNull { (centre(it) - position).getDistance() < cell * 0.32f } ?: return
                if (dot in dots) return
                // A straight line over a dot joins that dot too, as the system lock screen does.
                dots.lastOrNull()?.let { last ->
                    val between = (last + dot) / 2
                    val passesOver = (last % 3 + dot % 3) % 2 == 0 && (last / 3 + dot / 3) % 2 == 0
                    if (passesOver && between !in dots) dots = dots + between
                }
                dots = dots + dot
            }
            awaitEachGesture {
                val down = awaitFirstDown()
                dots = emptyList()
                onStart()
                visit(down.position)
                drag(down.id) { change ->
                    visit(change.position)
                    change.consume()
                }
                finger = null
                if (dots.isNotEmpty()) onPattern(dots)
            }
        }
    ) {
        val cell = size.width / 3f
        fun centre(dot: Int) = Offset((dot % 3 + 0.5f) * cell, (dot / 3 + 0.5f) * cell)
        val stroke = 6.dp.toPx()
        dots.zipWithNext { a, b -> drawLine(active, centre(a), centre(b), stroke, StrokeCap.Round) }
        val tail = finger
        if (tail != null && dots.isNotEmpty()) drawLine(active, centre(dots.last()), tail, stroke, StrokeCap.Round)
        for (dot in 0..8) {
            val joined = dot in dots
            drawCircle(if (joined) active else idle, if (joined) 10.dp.toPx() else 6.dp.toPx(), centre(dot))
        }
    }
}
