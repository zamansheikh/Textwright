package com.silifton.textwright.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.silifton.textwright.R
import com.silifton.textwright.data.Conversation
import com.silifton.textwright.data.Message
import com.silifton.textwright.data.Sim
import com.silifton.textwright.security.AppLock
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

@Composable
fun TextwrightRoot(vm: MainViewModel, isDefault: Boolean, onRequestDefault: () -> Unit, authenticate: Authenticate) {
    TextwrightTheme {
        if (AppLock.locked) {
            LockScreen(authenticate)
            return@TextwrightTheme
        }
        if (!isDefault) {
            SetupScreen(onRequestDefault)
            return@TextwrightTheme
        }
        val screen by vm.screen.collectAsState()
        BackHandler(enabled = screen != Screen.List) { vm.back() }
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "screen",
        ) { current ->
            when (current) {
                Screen.List -> ConversationListScreen(vm)
                is Screen.Thread -> ThreadScreen(vm, current)
                is Screen.Compose -> ComposeScreen(vm, current)
                Screen.Settings -> SettingsScreen(vm)
                Screen.LockSettings -> LockSettingsScreen(vm, authenticate)
                Screen.About -> AboutScreen(vm)
            }
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
            AppIcon(96.dp)
            Spacer(Modifier.height(24.dp))
            Text("Welcome to Textwright", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(
                "Android only lets the default SMS app edit, delete or store messages. " +
                    "Make Textwright your default SMS app to continue.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onRequestDefault, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Set as default SMS app")
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "You can switch back to another app at any time in the phone's settings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ConversationListScreen(vm: MainViewModel) {
    val all by vm.conversations.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }
    val conversations = remember(all, query) {
        val wanted = query.trim()
        if (wanted.isEmpty()) all
        else all.filter {
            it.title.contains(wanted, ignoreCase = true) || it.address.contains(wanted) ||
                it.snippet.contains(wanted, ignoreCase = true)
        }
    }
    Scaffold(
        topBar = { SearchBar(query, onQueryChange = { query = it }, onSettings = { vm.open(Screen.Settings) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.openCompose() },
                icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                text = { Text("New message") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        if (conversations.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(88.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_notification),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    if (all.isEmpty()) "No messages yet" else "No conversations found",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (all.isEmpty()) "Messages you send and receive will show up here."
                    else "Nothing matches \"${query.trim()}\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Bottom space so the last row can scroll clear of the button.
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
            ) {
                items(conversations, key = { it.threadId }) { conversation ->
                    ConversationRow(conversation) { vm.openThread(conversation.threadId, conversation.address) }
                }
            }
        }
    }
}

/** Search field across the top of the conversation list, with the way into settings beside it. */
@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search conversations") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceContainerHigh,
                    unfocusedContainerColor = colors.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            IconButton(onClick = onSettings) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, onClick: () -> Unit) {
    val unread = conversation.unread > 0
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(conversation.title, conversation.address, 48.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conversation.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    formatListTime(LocalContext.current, conversation.date),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unread) colors.primary else colors.onSurfaceVariant,
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (conversation.outgoing) "You: ${conversation.snippet}" else conversation.snippet,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (unread) colors.onSurface else colors.onSurfaceVariant,
                    fontWeight = if (unread) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (unread) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.height(20.dp).widthIn(min = 20.dp).clip(CircleShape)
                            .background(colors.primary).padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (conversation.unread > 99) "99+" else conversation.unread.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadScreen(vm: MainViewModel, screen: Screen.Thread) {
    val context = LocalContext.current
    val messages by vm.messages.collectAsState()
    val sims by vm.sims.collectAsState()
    val sendSubId by vm.sendSubId.collectAsState()
    val title = remember(screen.address) { vm.titleFor(screen.address) }
    var selected by remember { mutableStateOf<Message?>(null) }
    var editing by remember { mutableStateOf<Message?>(null) }
    var deleting by remember { mutableStateOf<Message?>(null) }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(title, screen.address, 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (title != screen.address) {
                                Text(
                                    screen.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                },
                navigationIcon = { BackButton(vm::back) },
                actions = {
                    IconButton(onClick = { dial(context, screen.address) }) {
                        Icon(Icons.Filled.Call, contentDescription = "Call")
                    }
                },
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
        ) {
            // Newest first: index + 1 is the older neighbour, drawn above.
            itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                val older = messages.getOrNull(index + 1)
                val newer = messages.getOrNull(index - 1)
                val startsDay = older == null || dayOf(older.date) != dayOf(message.date)
                Column(Modifier.fillMaxWidth()) {
                    if (startsDay) DayHeader(message.date)
                    MessageBubble(
                        message = message,
                        joinsOlder = !startsDay && older != null && sameGroup(older, message),
                        joinsNewer = newer != null && dayOf(newer.date) == dayOf(message.date) && sameGroup(message, newer),
                        simLabel = if (sims.size > 1) sims.firstOrNull { it.subId == message.subId }?.label else null,
                        onLongClick = { selected = message },
                    )
                }
            }
        }
    }

    selected?.let { message ->
        MessageActions(
            message = message,
            onDismiss = { selected = null },
            onEdit = { editing = message },
            onCopy = { copy(context, message.body) },
            onRestore = { vm.restore(message) },
            onDelete = { deleting = message },
        )
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
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DayHeader(millis: Long) {
    Box(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
        Text(
            formatDay(LocalContext.current, millis),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        )
    }
}

/**
 * [joinsOlder] and [joinsNewer] say whether the bubble above or below belongs to the same run of messages;
 * joined bubbles sit closer and square off the corners between them.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    joinsOlder: Boolean,
    joinsNewer: Boolean,
    simLabel: String?,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val incoming = message.isIncoming
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    val round = 20.dp
    val joined = 6.dp
    val shape = if (incoming) {
        RoundedCornerShape(if (joinsOlder) joined else round, round, round, if (joinsNewer) joined else round)
    } else {
        RoundedCornerShape(round, if (joinsOlder) joined else round, if (joinsNewer) joined else round, round)
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = if (joinsOlder) 2.dp else 10.dp),
        horizontalAlignment = if (incoming) Alignment.Start else Alignment.End,
    ) {
        Surface(
            color = when {
                incoming -> colors.surfaceContainerHigh
                dark -> colors.primaryContainer
                else -> colors.primary
            },
            contentColor = when {
                incoming -> colors.onSurface
                dark -> colors.onPrimaryContainer
                else -> colors.onPrimary
            },
            shape = shape,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .combinedClickable(onClick = {}, onLongClick = onLongClick),
        ) {
            Text(
                message.body,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
        // The time closes a run of messages; anything unusual about a message is always shown.
        val flags = buildList {
            if (message.isEdited) add("Edited")
            if (message.isSending) add("Sending…")
            if (message.isFailed) add("Not sent")
        }
        if (!joinsNewer || flags.isNotEmpty()) {
            val status = buildList {
                add(DateUtils.formatDateTime(context, message.date, DateUtils.FORMAT_SHOW_TIME))
                if (simLabel != null) add(simLabel)
                addAll(flags)
            }.joinToString(" · ")
            Text(
                status,
                style = MaterialTheme.typography.labelSmall,
                color = if (message.isFailed) colors.error else colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageActions(
    message: Message,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            message.body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp),
        )
        HorizontalDivider()
        SheetAction(rememberVectorPainter(Icons.Filled.Edit), "Edit") { onDismiss(); onEdit() }
        SheetAction(painterResource(R.drawable.ic_copy), "Copy text") { onDismiss(); onCopy() }
        if (message.isEdited) {
            SheetAction(painterResource(R.drawable.ic_restore), "Restore original") { onDismiss(); onRestore() }
        }
        SheetAction(rememberVectorPainter(Icons.Filled.Delete), "Delete", MaterialTheme.colorScheme.error) {
            onDismiss()
            onDelete()
        }
        Spacer(Modifier.navigationBarsPadding().height(8.dp))
    }
}

@Composable
private fun SheetAction(icon: Painter, label: String, color: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDialog(message: Message, onDismiss: () -> Unit, onSave: (String, Long) -> Unit) {
    val context = LocalContext.current
    var text by rememberSaveable(message.id) { mutableStateOf(message.body) }
    var date by rememberSaveable(message.id) { mutableStateOf(message.date) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
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
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 8,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = { pickingDate = true },
                        label = { Text(DateFormat.getMediumDateFormat(context).format(Date(date))) },
                        leadingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null, Modifier.size(18.dp)) },
                    )
                    AssistChip(
                        onClick = { pickingTime = true },
                        label = { Text(DateFormat.getTimeFormat(context).format(Date(date))) },
                    )
                }
                Spacer(Modifier.height(8.dp))
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

    if (pickingDate) {
        // The picker works in UTC days, so the local calendar date is carried across by its fields.
        val utc = TimeZone.getTimeZone("UTC")
        val local = Calendar.getInstance().apply { timeInMillis = date }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = Calendar.getInstance(utc).apply {
                clear()
                set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
            }.timeInMillis,
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { picked ->
                        val day = Calendar.getInstance(utc).apply { timeInMillis = picked }
                        withDate { set(day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH)) }
                    }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }

    if (pickingTime) {
        val cal = Calendar.getInstance().apply { timeInMillis = date }
        val state = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { pickingTime = false },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    withDate {
                        set(Calendar.HOUR_OF_DAY, state.hour)
                        set(Calendar.MINUTE, state.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    pickingTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingTime = false }) { Text("Cancel") } },
        )
    }
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
                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
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
    val colors = MaterialTheme.colorScheme
    Surface {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (sims.size > 1) SimPicker(sims, selectedSubId, onSelectSim)
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Text message") },
                maxLines = 5,
                shape = RoundedCornerShape(26.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surfaceContainerHigh,
                    unfocusedContainerColor = colors.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    onSend(text.trim())
                    text = ""
                },
                enabled = enabled && text.isNotBlank(),
                modifier = Modifier.padding(bottom = 4.dp).size(48.dp),
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
    Box(Modifier.padding(bottom = 4.dp)) {
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
internal fun BackButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

/** The launcher icon, drawn from its adaptive layers so it matches the home screen. */
@Composable
internal fun AppIcon(size: Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.24f)), contentAlignment = Alignment.Center) {
        // Adaptive layers are 108dp with the visible part in the middle 72dp, hence the 1.5x overdraw.
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.requiredSize(size * 1.5f))
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(size * 1.5f))
    }
}

private val avatarColors = listOf(
    Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFDB2777), Color(0xFFDC2626),
    Color(0xFFD97706), Color(0xFF059669), Color(0xFF0891B2), Color(0xFF2563EB),
)

/** A coloured disc with the contact's initial, or a person for numbers and short codes. [seed] fixes the colour. */
@Composable
internal fun Avatar(title: String, seed: String, size: Dp) {
    val initial = title.firstOrNull()?.takeIf { it.isLetter() }
    Box(
        modifier = Modifier.size(size).clip(CircleShape)
            .background(avatarColors[Math.floorMod(seed.hashCode(), avatarColors.size)]),
        contentAlignment = Alignment.Center,
    ) {
        if (initial != null) {
            Text(
                initial.uppercase(),
                color = Color.White,
                style = if (size >= 44.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
        } else {
            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.58f))
        }
    }
}

private const val DAY_MS = 86_400_000L
private const val GROUP_MS = 5 * 60_000L

/** Local calendar day number, for telling whether two messages fall on the same day. */
private fun dayOf(millis: Long): Long = (millis + TimeZone.getDefault().getOffset(millis)) / DAY_MS

/** Messages from the same side, minutes apart, read as one run. */
private fun sameGroup(older: Message, newer: Message): Boolean =
    older.isIncoming == newer.isIncoming && newer.date - older.date < GROUP_MS

private fun formatListTime(context: Context, millis: Long): String {
    val age = dayOf(System.currentTimeMillis()) - dayOf(millis)
    val flags = when {
        age == 0L -> DateUtils.FORMAT_SHOW_TIME
        age in 1..6 -> DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY
        else -> DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
    }
    return DateUtils.formatDateTime(context, millis, flags)
}

private fun formatDay(context: Context, millis: Long): String =
    when (dayOf(System.currentTimeMillis()) - dayOf(millis)) {
        0L -> "Today"
        1L -> "Yesterday"
        else -> DateUtils.formatDateTime(
            context,
            millis,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL,
        )
    }

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
}

private fun dial(context: Context, address: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", address, null))) }
}
