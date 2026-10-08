package com.silifton.textwright.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Patterns
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
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silifton.textwright.R
import com.silifton.textwright.data.Conversation
import com.silifton.textwright.data.Message
import com.silifton.textwright.data.Sim
import com.silifton.textwright.security.AppLock
import java.util.Calendar
import kotlinx.coroutines.launch
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
                Screen.Search -> SearchScreen(vm)
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
    val conversations by vm.conversations.collectAsState()
    val colors = MaterialTheme.colorScheme
    Scaffold(
        containerColor = colors.surfaceContainer,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().height(72.dp).padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Messages",
                    modifier = Modifier.weight(1f),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                )
                IconButton(onClick = vm::openSearch) {
                    Icon(Icons.Filled.Search, contentDescription = "Search", modifier = Modifier.size(26.dp))
                }
                IconButton(onClick = { vm.open(Screen.Settings) }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", modifier = Modifier.size(26.dp))
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { vm.openCompose() },
                icon = { Icon(painterResource(R.drawable.ic_chat), contentDescription = null) },
                text = { Text("Start chat", fontSize = 16.sp) },
                shape = RoundedCornerShape(18.dp),
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
            )
        },
    ) { padding ->
        // The list sits on a sheet with rounded top corners, under the title bar.
        Surface(
            modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = colors.surface,
        ) {
            if (conversations.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.size(88.dp).clip(CircleShape).background(colors.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_chat),
                            contentDescription = null,
                            tint = colors.onPrimaryContainer,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("No messages yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Messages you send and receive will show up here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    // Bottom space so the last row can scroll clear of the button.
                    contentPadding = PaddingValues(top = 8.dp, bottom = padding.calculateBottomPadding() + 96.dp),
                ) {
                    items(conversations, key = { it.threadId }) { conversation ->
                        ConversationRow(conversation) { vm.openThread(conversation.threadId, conversation.address) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, onClick: () -> Unit) {
    val unread = conversation.unread > 0
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Avatar(conversation.title, conversation.address, 56.dp)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f).padding(top = 4.dp)) {
            Text(
                conversation.title,
                fontSize = 18.sp,
                fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            // Unread conversations get a second line of preview, as the message has not been seen yet.
            Text(
                if (conversation.outgoing) "You: ${conversation.snippet}" else conversation.snippet,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = if (unread) colors.onSurface else colors.onSurfaceVariant,
                fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = if (unread) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.padding(top = 6.dp), horizontalAlignment = Alignment.End) {
            Text(
                formatListTime(LocalContext.current, conversation.date),
                fontSize = 13.sp,
                color = if (unread) colors.onSurface else colors.onSurfaceVariant,
                fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
            )
            if (unread) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier.height(20.dp).widthIn(min = 20.dp).clip(CircleShape)
                        .background(colors.primaryContainer).padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (conversation.unread > 99) "99+" else conversation.unread.toString(),
                        fontSize = 12.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onPrimaryContainer,
                    )
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
    var adding by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.imePadding(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(title, screen.address, 40.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(title, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                navigationIcon = { BackButton(vm::back) },
                actions = {
                    IconButton(onClick = { dial(context, screen.address) }) {
                        Icon(Icons.Filled.Call, contentDescription = "Call", modifier = Modifier.size(26.dp))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Add message") },
                                onClick = {
                                    menuOpen = false
                                    adding = true
                                },
                            )
                        }
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
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        ) {
            // Newest first: index + 1 is the older neighbour, drawn above.
            itemsIndexed(messages, key = { _, message -> message.id }) { index, message ->
                val older = messages.getOrNull(index + 1)
                val newer = messages.getOrNull(index - 1)
                val startsRun = older == null || startsRun(older, message)
                Column(Modifier.fillMaxWidth()) {
                    if (startsRun) TimeHeader(message.date)
                    MessageBubble(
                        message = message,
                        joinsOlder = !startsRun && older != null && older.isIncoming == message.isIncoming,
                        joinsNewer = newer != null && !startsRun(message, newer) && newer.isIncoming == message.isIncoming,
                        sim = if (sims.size > 1) sims.firstOrNull { it.subId == message.subId } else null,
                        isLatest = index == 0,
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

    if (adding) {
        AddMessageDialog(
            onDismiss = { adding = false },
            onSave = { body, date, incoming ->
                vm.addMessage(screen, body.trim(), date, incoming)
                adding = false
            },
        )
    }

    deleting?.let { message ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete message?") },
            text = { Text("This removes it from this phone. You can undo it for a few seconds afterwards.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(message)
                    deleting = null
                    scope.launch {
                        val result = snackbar.showSnackbar("Message deleted", "Undo", duration = SnackbarDuration.Long)
                        if (result == SnackbarResult.ActionPerformed) vm.undoDelete()
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } },
        )
    }
}

/** Centred "Yesterday • 6:05 PM" line that opens each run of messages. */
@Composable
private fun TimeHeader(millis: Long) {
    val context = LocalContext.current
    Text(
        "${formatDay(context, millis)} • ${DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_TIME)}",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 12.dp),
    )
}

/**
 * [joinsOlder] and [joinsNewer] say whether the bubble above or below belongs to the same run from the same
 * side; joined bubbles square off the corners between them. Tapping a bubble shows its time and whether it
 * was edited.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    joinsOlder: Boolean,
    joinsNewer: Boolean,
    sim: Sim?,
    isLatest: Boolean,
    onLongClick: () -> Unit,
) {
    val context = LocalContext.current
    val incoming = message.isIncoming
    val colors = MaterialTheme.colorScheme
    val dark = colors.surface.luminance() < 0.5f
    var expanded by remember(message.id) { mutableStateOf(false) }
    val round = 22.dp
    val joined = 4.dp
    val shape = if (incoming) {
        RoundedCornerShape(if (joinsOlder) joined else round, round, round, if (joinsNewer) joined else round)
    } else {
        RoundedCornerShape(round, if (joinsOlder) joined else round, if (joinsNewer) joined else round, round)
    }
    val linkColor = when {
        incoming -> colors.onSurface
        dark -> colors.onPrimaryContainer
        else -> colors.onPrimary
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = if (joinsOlder) 2.dp else 8.dp),
        horizontalAlignment = if (incoming) Alignment.Start else Alignment.End,
    ) {
        Surface(
            color = when {
                incoming -> colors.surfaceContainerHigh
                dark -> colors.primaryContainer
                else -> colors.primary
            },
            contentColor = linkColor,
            shape = shape,
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .wrapContentWidth(if (incoming) Alignment.Start else Alignment.End)
                .clip(shape)
                .combinedClickable(onClick = { expanded = !expanded }, onLongClick = onLongClick),
        ) {
            Text(
                remember(message.body, linkColor) { withLinks(message.body, linkColor) },
                fontSize = 16.sp,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        // Sending problems are always shown; the time and the edited marker only when the bubble is tapped.
        val details = buildList {
            if (expanded) add(DateUtils.formatDateTime(context, message.date, DateUtils.FORMAT_SHOW_TIME))
            if (sim != null && (expanded || isLatest)) add(sim.name)
            if (message.isEdited && expanded) add("Edited")
            if (message.added && expanded) add("Added")
            if (message.isSending) add("Sending…")
            if (message.isFailed) add("Not sent")
            if (message.isUndelivered) add("Not delivered")
            if (message.isDelivered && (expanded || isLatest)) add("Delivered")
            if (message.isAwaitingDelivery && expanded && !message.isSending && !message.isFailed) add("Sent, no delivery report yet")
        }
        if (details.isNotEmpty()) {
            Text(
                details.joinToString(" • "),
                fontSize = 12.sp,
                color = if (message.isFailed || message.isUndelivered) colors.error else colors.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
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

@Composable
private fun EditDialog(message: Message, onDismiss: () -> Unit, onSave: (String, Long) -> Unit) {
    MessageDialog(
        title = "Edit message",
        note = "Changes the copy on this phone only. The other person's copy stays the same.",
        initialBody = message.body,
        initialDate = message.date,
        chooseSide = false,
        onDismiss = onDismiss,
        onSave = { body, date, _ -> onSave(body, date) },
    )
}

/** Writes a message into the conversation by hand, for example to put back one that was deleted. */
@Composable
private fun AddMessageDialog(onDismiss: () -> Unit, onSave: (body: String, date: Long, incoming: Boolean) -> Unit) {
    MessageDialog(
        title = "Add message",
        note = "Adds a message to this conversation on this phone only. Nothing is sent, and it is marked as added.",
        initialBody = "",
        initialDate = remember { System.currentTimeMillis() },
        chooseSide = true,
        onDismiss = onDismiss,
        onSave = onSave,
    )
}

/** Text, date and time of one message; with [chooseSide], also whether it was received or sent. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageDialog(
    title: String,
    note: String,
    initialBody: String,
    initialDate: Long,
    chooseSide: Boolean,
    onDismiss: () -> Unit,
    onSave: (body: String, date: Long, incoming: Boolean) -> Unit,
) {
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf(initialBody) }
    var date by rememberSaveable { mutableStateOf(initialDate) }
    var incoming by rememberSaveable { mutableStateOf(true) }
    var pickingDate by remember { mutableStateOf(false) }
    var pickingTime by remember { mutableStateOf(false) }
    fun withDate(change: Calendar.() -> Unit) {
        date = Calendar.getInstance().apply { timeInMillis = date; change() }.timeInMillis
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (chooseSide) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = incoming,
                            onClick = { incoming = true },
                            shape = SegmentedButtonDefaults.itemShape(0, 2),
                        ) { Text("Received") }
                        SegmentedButton(
                            selected = !incoming,
                            onClick = { incoming = false },
                            shape = SegmentedButtonDefaults.itemShape(1, 2),
                        ) { Text("Sent") }
                    }
                    Spacer(Modifier.height(12.dp))
                }
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
                Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(text, date, incoming) },
                enabled = text.isNotBlank() && (text != initialBody || date != initialDate),
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
    val sim = sims.firstOrNull { it.subId == selectedSubId }
    Surface {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Surface(color = colors.surfaceContainerHigh, shape = RoundedCornerShape(28.dp), modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    if (sims.size > 1) SimPicker(sims, selectedSubId, onSelectSim)
                    TextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Text message", fontSize = 16.sp) },
                        textStyle = MaterialTheme.typography.bodyLarge,
                        maxLines = 5,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    onSend(text.trim())
                    text = ""
                },
                enabled = enabled && text.isNotBlank(),
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = if (sim != null) "Send with ${sim.name}" else "Send",
                )
            }
        }
    }
}

/** Shown only on multi-SIM phones, inside the message field: which SIM the next message goes out on. */
@Composable
private fun SimPicker(sims: List<Sim>, selectedSubId: Int, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(Modifier.padding(start = 4.dp, bottom = 4.dp)) {
        TextButton(onClick = { open = true }) {
            Text(sims.firstOrNull { it.subId == selectedSubId }?.name ?: "SIM", fontSize = 15.sp)
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
                fontSize = (size.value * 0.44f).sp,
            )
        } else {
            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.58f))
        }
    }
}

private const val DAY_MS = 86_400_000L
private const val RUN_GAP_MS = 60 * 60_000L

/** Local calendar day number, for telling whether two messages fall on the same day. */
private fun dayOf(millis: Long): Long = (millis + TimeZone.getDefault().getOffset(millis)) / DAY_MS

/** A new run, with its own time header, starts on a new day or after an hour of silence. */
private fun startsRun(older: Message, newer: Message): Boolean =
    dayOf(older.date) != dayOf(newer.date) || newer.date - older.date > RUN_GAP_MS

/** Underlines web addresses in a message and makes them open in the browser. */
private fun withLinks(body: String, color: Color): AnnotatedString = buildAnnotatedString {
    append(body)
    val matcher = Patterns.WEB_URL.matcher(body)
    while (matcher.find()) {
        val url = matcher.group()
        addLink(
            LinkAnnotation.Url(
                if (url.contains("://")) url else "https://$url",
                TextLinkStyles(SpanStyle(color = color, textDecoration = TextDecoration.Underline)),
            ),
            matcher.start(),
            matcher.end(),
        )
    }
}

internal fun formatListTime(context: Context, millis: Long): String {
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
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_MONTH,
        )
    }

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
}

private fun dial(context: Context, address: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", address, null))) }
}
