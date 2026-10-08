package com.silifton.textwright.ui

import android.content.ClipData
import java.util.Date
import java.util.Calendar
import android.text.format.DateFormat
import android.app.TimePickerDialog
import android.app.DatePickerDialog
import android.content.ClipboardManager
import android.content.Context
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.silifton.textwright.data.Conversation
import com.silifton.textwright.data.Message
import com.silifton.textwright.data.Sim

@Composable
fun TextwrightRoot(vm: MainViewModel, isDefault: Boolean, onRequestDefault: () -> Unit) {
    TextwrightTheme {
        if (!isDefault) {
            SetupScreen(onRequestDefault)
            return@TextwrightTheme
        }
        val screen by vm.screen.collectAsState()
        BackHandler(enabled = screen != Screen.List) { vm.back() }
        when (val current = screen) {
            Screen.List -> ConversationListScreen(vm)
            is Screen.Thread -> ThreadScreen(vm, current)
            is Screen.Compose -> ComposeScreen(vm, current)
        }
    }
}

@Composable
private fun SetupScreen(onRequestDefault: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Textwright", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "Android only lets the default SMS app edit, delete, or store messages. " +
                    "Set Textwright as your default SMS app to continue.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRequestDefault) { Text("Set as default SMS app") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationListScreen(vm: MainViewModel) {
    val conversations by vm.conversations.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Textwright") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.openCompose() }) {
                Icon(Icons.Filled.Add, contentDescription = "New message")
            }
        },
    ) { padding ->
        if (conversations.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No messages yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize(), contentPadding = padding) {
                items(conversations, key = { it.threadId }) { conversation ->
                    ConversationRow(conversation) { vm.openThread(conversation.threadId, conversation.address) }
                    HorizontalDivider(Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, onClick: () -> Unit) {
    val unread = conversation.unread > 0
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                conversation.title.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "#",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conversation.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    formatTime(LocalContext.current, conversation.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                conversation.snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = if (unread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (unread) FontWeight.Medium else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadScreen(vm: MainViewModel, screen: Screen.Thread) {
    val messages by vm.messages.collectAsState()
    val sims by vm.sims.collectAsState()
    val sendSubId by vm.sendSubId.collectAsState()
    val title = remember(screen.address) { vm.titleFor(screen.address) }
    var editing by remember { mutableStateOf<Message?>(null) }
    var deleting by remember { mutableStateOf<Message?>(null) }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(vm::back) },
            )
        },
        bottomBar = {
            MessageInput(
                onSend = { vm.send(screen.address, it) },
                sims = sims,
                selectedSubId = sendSubId,
                onSelectSim = vm::selectSim,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            reverseLayout = true,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    simLabel = if (sims.size > 1) sims.firstOrNull { it.subId == message.subId }?.label else null,
                    onEdit = { editing = message },
                    onRestore = { vm.restore(message) },
                    onDelete = { deleting = message },
                )
            }
        }
    }

    editing?.let { message ->
        EditDialog(
            message = message,
            onDismiss = { editing = null },
            onSave = { body, date ->
                vm.edit(message, body, date)
                editing = null
            },
        )
    }

    deleting?.let { message ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete message?") },
            text = { Text("This removes it from this device and can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(message)
                    deleting = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    simLabel: String?,
    onEdit: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val incoming = message.isIncoming
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (incoming) Alignment.Start else Alignment.End,
    ) {
        Box {
            Surface(
                color = if (incoming) colors.surfaceVariant else colors.primary,
                contentColor = if (incoming) colors.onSurfaceVariant else colors.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .combinedClickable(onClick = {}, onLongClick = { menuOpen = true }),
            ) {
                Text(message.body, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEdit() })
                DropdownMenuItem(text = { Text("Copy") }, onClick = { menuOpen = false; copy(context, message.body) })
                if (message.isEdited) {
                    DropdownMenuItem(text = { Text("Restore original") }, onClick = { menuOpen = false; onRestore() })
                }
                DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
            }
        }
        val status = buildList {
            add(formatTime(context, message.date))
            if (simLabel != null) add(simLabel)
            if (message.isEdited) add("edited")
            if (message.isSending) add("sending")
            if (message.isFailed) add("not sent")
        }.joinToString(" · ")
        Text(
            status,
            style = MaterialTheme.typography.labelSmall,
            color = if (message.isFailed) colors.error else colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun EditDialog(message: Message, onDismiss: () -> Unit, onSave: (String, Long) -> Unit) {
    val context = LocalContext.current
    var text by rememberSaveable(message.id) { mutableStateOf(message.body) }
    var date by rememberSaveable(message.id) { mutableStateOf(message.date) }
    fun withDate(change: Calendar.() -> Unit) {
        date = Calendar.getInstance().apply { timeInMillis = date; change() }.timeInMillis
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit message") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = date }
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> withDate { set(y, m, d) } },
                            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
                        ).show()
                    }) { Text(DateFormat.getMediumDateFormat(context).format(Date(date))) }
                    TextButton(onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = date }
                        TimePickerDialog(
                            context,
                            { _, h, m ->
                                withDate {
                                    set(Calendar.HOUR_OF_DAY, h)
                                    set(Calendar.MINUTE, m)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                            },
                            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE),
                            DateFormat.is24HourFormat(context),
                        ).show()
                    }) { Text(DateFormat.getTimeFormat(context).format(Date(date))) }
                }
                Text(
                    "Changes the copy on this phone only. The other person's copy stays the same.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text, date) },
                enabled = text.isNotBlank() && (text != message.body || date != message.date),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComposeScreen(vm: MainViewModel, screen: Screen.Compose) {
    var address by rememberSaveable { mutableStateOf(screen.address) }
    val sims by vm.sims.collectAsState()
    val sendSubId by vm.sendSubId.collectAsState()
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(title = { Text("New message") }, navigationIcon = { BackButton(vm::back) })
        },
        bottomBar = {
            MessageInput(
                initial = screen.body,
                enabled = address.isNotBlank(),
                onSend = { vm.sendNew(address.trim(), it) },
                sims = sims,
                selectedSubId = sendSubId,
                onSelectSim = vm::selectSim,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("To") },
                placeholder = { Text("Phone number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun MessageInput(
    onSend: (String) -> Unit,
    initial: String = "",
    enabled: Boolean = true,
    sims: List<Sim> = emptyList(),
    selectedSubId: Int = -1,
    onSelectSim: (Int) -> Unit = {},
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (sims.size > 1) SimPicker(sims, selectedSubId, onSelectSim)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Text message") },
                maxLines = 5,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            IconButton(
                onClick = {
                    onSend(text.trim())
                    text = ""
                },
                enabled = enabled && text.isNotBlank(),
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
            }
        }
    }
}

/** Shown only on multi-SIM phones: which SIM the next message goes out on. */
@Composable
private fun SimPicker(sims: List<Sim>, selectedSubId: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text(sims.firstOrNull { it.subId == selectedSubId }?.label ?: "SIM")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            sims.forEach { sim ->
                DropdownMenuItem(
                    text = { Text(if (sim.carrier.isBlank()) sim.label else "${sim.label} · ${sim.carrier}") },
                    onClick = {
                        open = false
                        onSelect(sim.subId)
                    },
                )
            }
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

private fun formatTime(context: Context, millis: Long): String {
    val flags = if (DateUtils.isToday(millis)) {
        DateUtils.FORMAT_SHOW_TIME
    } else {
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL
    }
    return DateUtils.formatDateTime(context, millis, flags)
}

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
}
