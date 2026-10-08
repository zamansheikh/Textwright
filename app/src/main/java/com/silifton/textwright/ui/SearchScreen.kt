package com.silifton.textwright.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silifton.textwright.data.Conversation
import com.silifton.textwright.data.SearchHit
import com.silifton.textwright.data.SearchResults
import kotlinx.coroutines.delay

/** A way of narrowing the conversation list, offered as tiles before anything is typed. */
private class SearchFilter(val label: String, val icon: ImageVector, val matches: (Conversation) -> Boolean)

@Composable
fun SearchScreen(vm: MainViewModel) {
    val conversations by vm.conversations.collectAsState()
    val sims by vm.sims.collectAsState()
    val colors = MaterialTheme.colorScheme
    val query = vm.searchQuery
    val wanted = query.trim()
    var filter by remember { mutableStateOf<SearchFilter?>(null) }
    var results by remember { mutableStateOf(SearchResults.Empty) }
    val focus = remember { FocusRequester() }

    val filters = remember(sims) {
        buildList {
            add(SearchFilter("Unread", Icons.Filled.Notifications) { it.unread > 0 })
            add(SearchFilter("Known", Icons.Filled.AccountCircle) { it.name != null })
            add(SearchFilter("Unknown", Icons.Filled.Person) { it.name == null })
            if (sims.size > 1) sims.forEach { sim -> add(SearchFilter(sim.name, Icons.Filled.Phone) { it.subId == sim.subId }) }
        }
    }

    LaunchedEffect(Unit) { if (query.isEmpty()) focus.requestFocus() }
    LaunchedEffect(wanted) {
        if (wanted.isEmpty()) {
            results = SearchResults.Empty
        } else {
            delay(200) // let typing settle before reading the message store
            results = vm.search(wanted)
        }
    }

    // A filter alone lists its conversations; with text it narrows the text matches.
    val active = filter
    val hits = when {
        wanted.isEmpty() && active != null ->
            conversations.filter(active.matches).map { SearchHit(it.threadId, it.address, it.title, it.snippet, it.date) }
        active != null -> {
            val allowed = conversations.filter(active.matches).mapTo(HashSet()) { it.threadId }
            results.hits.filter { it.threadId in allowed }
        }
        else -> results.hits
    }
    val contacts = if (active == null) results.contacts else emptyList()

    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = colors.surfaceContainer,
        topBar = {
            TextField(
                value = query,
                onValueChange = { vm.searchQuery = it },
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
                    .focusRequester(focus),
                placeholder = { Text("Search messages", fontSize = 17.sp) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                leadingIcon = { BackButton(vm::back) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        ) {
            if (active != null) {
                item {
                    InputChip(
                        selected = true,
                        onClick = { filter = null },
                        label = { Text(active.label) },
                        leadingIcon = { Icon(active.icon, contentDescription = null, Modifier.size(18.dp)) },
                        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove filter", Modifier.size(18.dp)) },
                    )
                }
            } else if (wanted.isEmpty()) {
                items(filters.chunked(2).size) { row ->
                    Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val pair = filters.chunked(2)[row]
                        pair.forEach { FilterTile(it, Modifier.weight(1f)) { filter = it } }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            if (hits.isNotEmpty()) {
                item { SectionLabel("Conversations") }
                itemsIndexed(hits, key = { _, hit -> hit.threadId }) { index, hit ->
                    GroupedRow(index, hits.size, onClick = { vm.openThread(hit.threadId, hit.address) }) {
                        Avatar(hit.title, hit.address, 48.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                highlight(hit.title, wanted),
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                highlight(excerpt(hit.body, wanted), wanted),
                                fontSize = 15.sp,
                                color = colors.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            formatListTime(LocalContext.current, hit.date),
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }

            if (contacts.isNotEmpty()) {
                item { SectionLabel("Contacts") }
                itemsIndexed(contacts) { index, contact ->
                    GroupedRow(index, contacts.size, onClick = { vm.openAddress(contact.number) }) {
                        Avatar(contact.name, contact.number, 48.dp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(highlight(contact.name, wanted), fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(contact.number, fontSize = 15.sp, color = colors.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }

            if (hits.isEmpty() && contacts.isEmpty() && (wanted.isNotEmpty() || active != null)) {
                item {
                    Text(
                        "No results",
                        fontSize = 15.sp,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 32.dp, start = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterTile(filter: SearchFilter, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        modifier = modifier.height(56.dp).clip(RoundedCornerShape(28.dp)).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(filter.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(filter.label, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 12.dp),
    )
}

/** One row of a stack of results that reads as a single card: only the outer corners are fully rounded. */
@Composable
private fun GroupedRow(index: Int, count: Int, onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    val outer = 22.dp
    val inner = 4.dp
    val shape = RoundedCornerShape(
        topStart = if (index == 0) outer else inner,
        topEnd = if (index == 0) outer else inner,
        bottomStart = if (index == count - 1) outer else inner,
        bottomEnd = if (index == count - 1) outer else inner,
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp).clip(shape)
            .background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

private val HighlightBackground = Color(0xFFFBBF24)

/** Marks every occurrence of [query] in [text], ignoring case. */
private fun highlight(text: String, query: String): AnnotatedString = buildAnnotatedString {
    append(text)
    if (query.isEmpty()) return@buildAnnotatedString
    var at = text.indexOf(query, ignoreCase = true)
    while (at >= 0) {
        addStyle(SpanStyle(background = HighlightBackground, color = Color.Black), at, at + query.length)
        at = text.indexOf(query, at + query.length, ignoreCase = true)
    }
}

/** A single line of [body] that starts shortly before the match, so the match is visible in a one-line preview. */
private fun excerpt(body: String, query: String): String {
    val line = body.replace('\n', ' ')
    val at = if (query.isEmpty()) -1 else line.indexOf(query, ignoreCase = true)
    return if (at > 16) "…" + line.substring(at - 12) else line
}
