package com.silifton.textwright.ui

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.silifton.textwright.data.Conversation
import com.silifton.textwright.data.Message
import com.silifton.textwright.data.Sim
import com.silifton.textwright.data.SmsRepository
import com.silifton.textwright.sms.Notifier
import com.silifton.textwright.sms.Sims
import com.silifton.textwright.sms.SmsSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface Screen {
    data object List : Screen
    data class Thread(val threadId: Long, val address: String) : Screen
    data class Compose(val address: String = "", val body: String = "") : Screen
    data object LockSettings : Screen
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SmsRepository(app)

    private val _screen = MutableStateFlow<Screen>(Screen.List)
    val screen: StateFlow<Screen> = _screen

    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    private val _sims = MutableStateFlow<List<Sim>>(emptyList())
    val sims: StateFlow<List<Sim>> = _sims

    /** SIM the next message goes out on. */
    private val _sendSubId = MutableStateFlow(-1)
    val sendSubId: StateFlow<Int> = _sendSubId

    private var observing = false
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    /** Call once Textwright holds the default SMS role; before that the store is not readable. */
    fun start() {
        if (!observing) {
            try {
                getApplication<Application>().contentResolver
                    .registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer)
                observing = true
            } catch (e: SecurityException) {
                // Retried on the next start().
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val sims = Sims.active(getApplication())
                _sims.value = sims
                _sendSubId.value = Sims.choose(sims, _sendSubId.value)
            }
            runCatching {
                _conversations.value = repo.conversations()
                (_screen.value as? Screen.Thread)?.let { _messages.value = repo.messages(it.threadId) }
            }
        }
    }

    fun openThread(threadId: Long, address: String) {
        _messages.value = emptyList()
        _screen.value = Screen.Thread(threadId, address)
        Notifier.cancel(getApplication(), threadId)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repo.markRead(threadId) }
            // Reply on the SIM the conversation last used.
            runCatching {
                val last = repo.messages(threadId).firstOrNull { it.subId >= 0 }?.subId
                _sendSubId.value = Sims.choose(Sims.active(getApplication()), last)
            }
            refresh()
        }
    }

    fun openCompose(address: String = "", body: String = "") {
        _sendSubId.value = Sims.choose(_sims.value)
        _screen.value = Screen.Compose(address, body)
    }

    fun openLockSettings() {
        _screen.value = Screen.LockSettings
    }

    fun selectSim(subId: Int) {
        _sendSubId.value = subId
    }

    fun back() {
        _screen.value = Screen.List
    }

    fun titleFor(address: String): String = repo.contactName(address) ?: address

    fun send(address: String, body: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { SmsSender.send(getApplication(), address, body, _sendSubId.value) }
        }
    }

    fun sendNew(address: String, body: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val threadId = repo.threadIdFor(address)
                val subId = _sendSubId.value
                SmsSender.send(getApplication(), address, body, subId)
                openThread(threadId, address)
            }
        }
    }

    fun edit(message: Message, newBody: String, newDate: Long) = write { repo.editMessage(message, newBody, newDate) }

    fun restore(message: Message) = write { repo.restore(message) }

    fun delete(message: Message) = write { repo.delete(message.id) }

    private fun write(block: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching(block)
            refresh()
        }
    }

    override fun onCleared() {
        if (observing) getApplication<Application>().contentResolver.unregisterContentObserver(observer)
    }
}
