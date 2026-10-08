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
) {
    val title: String get() = name ?: address.ifBlank { "Unknown" }
}

data class Sim(
    val subId: Int,
    val slot: Int,
    val carrier: String,
) {
    val label: String get() = "SIM ${slot + 1}"
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
