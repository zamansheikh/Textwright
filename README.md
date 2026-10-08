# Textwright

**An open-source SMS app for Android that gives you full control over your own message history.**

Textwright is a native Android messaging app, built with Kotlin and Jetpack Compose. It starts from one idea: the messages stored on *your* phone should be yours to edit. Today it is a working SMS client with message and timestamp editing. The goal is a complete, modern default messaging app.

> **Status:** early development (`v0.1.0`). Usable and tested, but expect rough edges and breaking changes. See the [roadmap](#roadmap).

---

## Features

### Available now
- **Edit message text**: change what a stored message says.
- **Edit message date and time**: change the timestamp a message shows.
- **Restore original**: undo any edit and bring back the original text *and* date. Textwright keeps the first original, however many times you edit.
- **Edited marker**: edited messages are labelled, so you always know what you changed.
- **Syncs with other messaging apps**: edits are written so that Google Messages shows them when it becomes the default app again (see [How edits work](#how-edits-work)).
- **Crash-safe edits**: an edit interrupted by a crash or app kill is finished or cleaned up on the next launch.
- **Send and receive SMS**: conversation list, threads, new message, reply.
- **Dual-SIM support**: pick the SIM to send from; replies default to the SIM the conversation last used.
- **Contact names**: conversations show contact names when the contacts permission is granted.
- **Notifications** for incoming messages.
- Copy and delete messages.

### Not yet supported
- MMS (receiving is stubbed; sending and viewing are not implemented)
- RCS, group messaging, attachments
- Search, backup and restore, archived and blocked conversations
- Tablets and foldables (layout not tuned)

## How edits work

Android lets only the **default SMS app** write to the system SMS store, so Textwright must be set as the default to edit anything.

- **Edits are local.** They change the copy stored on your device. The other person's phone and your carrier's records are not affected, and nothing is sent.
- **Edits are written as a replacement row.** Other messaging apps (Google Messages, Samsung Messages) keep their own database and ignore messages changed in place. When you save, Textwright inserts a new row with the same address, SIM and read state but the new text and date, then deletes the old one. Apps that re-sync on becoming the default then see the edited message.
- **Originals live in Textwright's private database** (`edits.db`) so they can be restored. Uninstalling the app, or clearing its data, loses them.
- **Known limitation:** another messaging app only shows an edit after it becomes the default app again. Android has no way to tell it to reload.

Verified with Google Messages on a physical Pixel 6 and on an emulator. **Samsung Messages and other apps are untested.** See [Contributing](#contributing) if you can help.

## Responsible use

Textwright edits the copy of your messages on your own device. It is not a tool for fabricating evidence, impersonating others, or misleading anyone about what was said. It deliberately does **not** let you edit the sender of a message or create fake received messages. Edited messages carry a visible marker in Textwright. Please use it lawfully.

## Requirements

| | |
|---|---|
| Android | 8.0 (API 26) or newer |
| Hardware | A device with telephony (SMS). Dual-SIM supported. |
| Role | Textwright must be the **default SMS app** to edit, send or receive |

## Getting started

### Build from source

Prerequisites: JDK 17 and Android Studio (or just the Android SDK with platform 36).

```bash
git clone <repo-url>
cd Textwright
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### First run
1. Open Textwright and tap **Set as default** (grant the permissions it asks for).
2. Open a conversation, long-press a message, and choose **Edit**.
3. Change the text, the date or the time, then **Save**.
4. To use another messaging app again, make it the default in *Settings → Apps → Default apps → SMS app*.

> **Back up first.** Editing replaces messages in your system SMS store. Try it on a test conversation before relying on it, and keep a backup of messages you care about.

## Permissions

| Permission | Why |
|---|---|
| `READ_SMS`, `RECEIVE_SMS`, `SEND_SMS` | Read, receive and send text messages |
| `RECEIVE_MMS`, `RECEIVE_WAP_PUSH` | Required to qualify as a default SMS app (MMS is not yet implemented) |
| `READ_CONTACTS` | Show contact names instead of numbers |
| `READ_PHONE_STATE` | List the active SIMs for the SIM picker |
| `POST_NOTIFICATIONS` | Notify you of incoming messages |

Textwright has no network permission and sends no data anywhere.

## Architecture

Single-module Android app, package `com.silifton.textwright`.

```
app/src/main/java/com/silifton/textwright/
├── MainActivity.kt          entry point, default-SMS role handling
├── data/
│   ├── SmsRepository.kt     reads and writes the system SMS store, row replacement and crash recovery
│   ├── EditStore.kt         private SQLite store: originals and the in-flight edit journal
│   └── Models.kt            Conversation, Message, Sim
├── sms/                     receivers, sender, SIM helpers, notifications
└── ui/                      Compose screens, view model, theme
```

- **UI:** Jetpack Compose, Material 3.
- **State:** a single `MainViewModel` exposing `StateFlow`s; I/O runs on `Dispatchers.IO`.
- **Storage:** the system SMS provider (`Telephony.Sms`) plus a private `edits.db`.

## Roadmap

The aim is a full-featured default messaging app. Rough order of priority:

- [ ] Samsung Messages and other apps: test and handle their re-sync behaviour
- [ ] MMS and attachments
- [ ] Unit and instrumented tests, CI
- [ ] Search
- [ ] Backup and restore, including Textwright's saved originals
- [ ] Archive, mute, block and spam handling
- [ ] Group messaging
- [ ] Scheduled sending
- [ ] RCS
- [ ] Tablet and foldable layouts, accessibility pass, localization

Ideas and priorities are open for discussion in issues.

## Contributing

Contributions are welcome, from bug reports to new features.

1. Open an issue describing the bug or idea first for anything non-trivial.
2. Fork the repo and create a branch from `main`.
3. Keep changes focused, and match the surrounding code style.
4. Test on a real device or emulator. SMS behaviour differs between devices and OS versions, so please say what you tested on.
5. Open a pull request with a clear description.

Particularly useful right now: testing edits against **Samsung Messages and other default apps**, and writing tests.

## License

A license has not been chosen yet. Until a `LICENSE` file is added, all rights are reserved by the authors.

## Acknowledgements

Built with [Kotlin](https://kotlinlang.org/) and [Jetpack Compose](https://developer.android.com/jetpack/compose).
