package com.silifton.textwright.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

/**
 * Covers the whole app while it is locked. Shows one way in at a time: the fingerprint when it is turned on,
 * and the pattern pad only when there is no fingerprint or the user asks for the pattern.
 */
@Composable
fun LockScreen(authenticate: Authenticate) {
    val context = LocalContext.current
    val hasPattern = remember { AppLock.hasPattern(context) }
    val fingerprint = remember { AppLock.biometricEnabled(context) }
    val showPattern = hasPattern && (!fingerprint || AppLock.patternRequested)
    fun askFingerprint() {
        AppLock.patternRequested = false
        authenticate("Unlock Textwright", AppLock::unlock)
    }
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(72.dp)
            Spacer(Modifier.height(16.dp))
            Text("Textwright is locked", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            if (showPattern) {
                VerifyPattern("Draw your pattern to unlock", onVerified = AppLock::unlock)
                if (fingerprint) {
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = ::askFingerprint) { Text("Use fingerprint") }
                }
            } else {
                PatternPrompt("Use your fingerprint to unlock", error = false)
                Button(onClick = ::askFingerprint, modifier = Modifier.height(52.dp)) { Text("Unlock with fingerprint") }
                if (hasPattern) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { AppLock.patternRequested = true }) { Text("Use pattern") }
                }
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
                Text(
                    "Ask for your fingerprint, a pattern, or either one every time Textwright is opened.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp),
                )
                SectionHeader("Fingerprint")
                SwitchRow(
                    "Unlock with fingerprint",
                    when {
                        !canUseBiometric -> "Add a fingerprint in the phone's settings first"
                        hasPattern -> "The pattern still works as a fallback"
                        else -> "The phone's screen lock works as a fallback"
                    },
                    checked = fingerprint,
                    enabled = canUseBiometric,
                ) { on ->
                    if (on) {
                        authenticate("Confirm your fingerprint") {
                            AppLock.setBiometricEnabled(context, true)
                            fingerprint = true
                        }
                    } else {
                        AppLock.setBiometricEnabled(context, false)
                        fingerprint = false
                    }
                }
                SectionHeader("Pattern")
                if (!hasPattern) {
                    SettingRow("Set a pattern", "Join at least ${AppLock.MIN_DOTS} dots on a grid", onClick = {
                        mismatch = false
                        step = LockStep.Draw
                    })
                } else {
                    SettingRow("Change pattern", "Draw the current pattern, then a new one", onClick = {
                        step = LockStep.Verify(remove = false)
                    })
                    SettingRow("Remove pattern", "Draw the current pattern to remove it", onClick = {
                        step = LockStep.Verify(remove = true)
                    })
                }
                Text(
                    when {
                        hasPattern -> "App lock is on. A forgotten pattern can't be recovered: without a working " +
                            "fingerprint, the only way back in is to clear the app's data, which also loses the " +
                            "saved originals of edited messages."
                        fingerprint -> "App lock is on. If the fingerprint isn't recognised, the phone's screen lock " +
                            "opens Textwright instead."
                        else -> "App lock is off. Turn on fingerprint, set a pattern, or both."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
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
                        AppLock.removePattern(context)
                        hasPattern = false
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
