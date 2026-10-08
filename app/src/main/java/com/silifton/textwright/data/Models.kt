package com.silifton.textwright.data

import android.provider.Telephony

data class Conversation(
    val threadId: Long,
    val address: String,
    val name: String?,
    val snippet: String,
    val date: Long,
    val unread: Int,
    /** True when the latest message was sent from this phone. */
    val outgoing: Boolean,
    /** Subscription of the latest message, or -1 if unknown. */
    val subId: Int,
) {
    val title: String get() = name ?: address.ifBlank { "Unknown" }
}

data class Sim(
    val subId: Int,
    val slot: Int,
    val carrier: String,
) {
    val label: String get() = "SIM ${slot + 1}"

    /** Carrier name when the SIM reports one, otherwise the slot label. */
    val name: String get() = carrier.ifBlank { label }
}

data class Message(
    val id: Long,
    val threadId: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int,
    /** Subscription the message was sent or received on, or -1 if unknown. */
    val subId: Int,
    /** Text and date the message had before its first edit in Textwright, or null if never edited. */
    val original: EditStore.Original?,
) {
    val isIncoming: Boolean get() = type == Telephony.Sms.MESSAGE_TYPE_INBOX
    val isFailed: Boolean get() = type == Telephony.Sms.MESSAGE_TYPE_FAILED
    val isSending: Boolean get() = type == Telephony.Sms.MESSAGE_TYPE_OUTBOX
    val isEdited: Boolean get() = original != null
}

/** A conversation found by search. [body] is the message, or latest message, that matched. */
data class SearchHit(val threadId: Long, val address: String, val title: String, val body: String, val date: Long)

data class ContactHit(val name: String, val number: String)

class SearchResults(val hits: List<SearchHit>, val contacts: List<ContactHit>) {
    companion object {
        val Empty = SearchResults(emptyList(), emptyList())
    }
}
