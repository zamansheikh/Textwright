package com.silifton.textwright.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.silifton.textwright.data.AppSettings
import com.silifton.textwright.security.AppLock

private const val SOURCE_URL = "https://github.com/zamansheikh/Textwright"
private const val DEVELOPER_GITHUB = "https://github.com/zamansheikh"
private const val DEVELOPER_FACEBOOK = "https://www.facebook.com/zamansheikh.404"
private const val LICENSE_URL = "$SOURCE_URL/blob/main/LICENSE"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    var pickingTheme by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { BackButton(vm::back) }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SectionHeader("Appearance")
            SettingRow("Theme", AppSettings.themeMode.label, onClick = { pickingTheme = true })
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(
                    "Wallpaper colours",
                    "Take the app's colours from your wallpaper",
                    AppSettings.dynamicColor,
                ) { AppSettings.setDynamicColor(context, it) }
            }

            SectionDivider()
            SectionHeader("Notifications")
            SwitchRow(
                "Show message text",
                "Turn off to show only \"New message\" in notifications",
                AppSettings.notificationPreview,
            ) { AppSettings.setNotificationPreview(context, it) }
            SettingRow("Sound, vibration and more", "Opens the phone's notification settings", onClick = {
                open(
                    context,
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                )
            })

            SectionDivider()
            SectionHeader("Privacy and security")
            SettingRow(
                "App lock",
                when {
                    AppLock.hasPattern(context) && AppLock.biometricEnabled(context) -> "On · fingerprint or pattern"
                    AppLock.hasPattern(context) -> "On · pattern"
                    AppLock.biometricEnabled(context) -> "On · fingerprint"
                    else -> "Off"
                },
                onClick = { vm.open(Screen.LockSettings) },
            )
            SwitchRow(
                "Block screenshots",
                "Also hides Textwright in the recent apps screen",
                AppSettings.blockScreenshots,
            ) { AppSettings.setBlockScreenshots(context, it) }

            SectionDivider()
            SectionHeader("Messaging")
            SettingRow("Default SMS app", "Textwright. Tap to switch to another app", onClick = {
                open(context, Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            })

            SectionDivider()
            SectionHeader("About")
            SettingRow("About Textwright", "Version ${versionName(context)}", onClick = { vm.open(Screen.About) })
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickingTheme) {
        AlertDialog(
            onDismissRequest = { pickingTheme = false },
            title = { Text("Theme") },
            text = {
                Column {
                    AppSettings.ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                AppSettings.setThemeMode(context, mode)
                                pickingTheme = false
                            }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = AppSettings.themeMode == mode, onClick = null)
                            Spacer(Modifier.width(16.dp))
                            Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickingTheme = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(vm: MainViewModel) {
    val context = LocalContext.current
    Scaffold(
        topBar = { TopAppBar(title = { Text("About") }, navigationIcon = { BackButton(vm::back) }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppIcon(96.dp)
                Spacer(Modifier.height(16.dp))
                Text("Textwright", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Version ${versionName(context)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "An SMS app that lets you edit the text and time of the messages stored on your own phone.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
            }

            InfoCard(
                "How edits work",
                "Edits change the copy on this phone only. Nothing is sent, and the other person's copy and your " +
                    "carrier's records stay the same. The original is kept so any edit can be undone.",
            )
            InfoCard(
                "Responsible use",
                "Textwright is not a tool for fabricating evidence or misleading anyone about what was said. " +
                    "It can't change who sent a message or create received messages, and tapping an edited message shows it was edited.",
            )
            InfoCard(
                "Your data",
                "Textwright has no internet permission. Your messages never leave the phone.",
            )

            SectionHeader("Developer")
            SettingRow("Zaman Sheikh", "Designed and developed Textwright")
            SettingRow("GitHub", "github.com/zamansheikh", onClick = {
                open(context, Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_GITHUB)))
            })
            SettingRow("Facebook", "facebook.com/zamansheikh.404", onClick = {
                open(context, Intent(Intent.ACTION_VIEW, Uri.parse(DEVELOPER_FACEBOOK)))
            })

            SectionHeader("Project")
            SettingRow("Source code", "github.com/zamansheikh/Textwright", onClick = {
                open(context, Intent(Intent.ACTION_VIEW, Uri.parse(SOURCE_URL)))
            })
            SettingRow("License", "Source-available. Personal use and contributions only", onClick = {
                open(context, Intent(Intent.ACTION_VIEW, Uri.parse(LICENSE_URL)))
            })
            SettingRow("App info", "Permissions, storage and battery in the phone's settings", onClick = {
                open(
                    context,
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            })
            Text(
                "Developed by Zaman Sheikh · Silifton",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp),
            )
        }
    }
}

@Composable
internal fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
internal fun SettingRow(
    title: String,
    summary: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth()
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) colors.onSurface else colors.onSurface.copy(alpha = 0.38f),
            )
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) colors.onSurfaceVariant else colors.onSurfaceVariant.copy(alpha = 0.38f),
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(16.dp))
            trailing()
        }
    }
}

@Composable
internal fun SwitchRow(
    title: String,
    summary: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    SettingRow(title, summary, enabled, onClick = { onChange(!checked) }) {
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
private fun InfoCard(title: String, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun versionName(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: ""

/** Not every phone has every settings page or a browser; a missing one is ignored. */
private fun open(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent) }
}
