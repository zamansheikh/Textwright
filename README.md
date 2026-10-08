<div align="center">

<img src="docs/icon.png" alt="Textwright icon" width="128" height="128">

# Textwright

**An SMS app for Android that lets you edit your own message history.**

[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)](#requirements)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Version](https://img.shields.io/badge/version-0.1.0-4F46E5)](#roadmap)
[![License](https://img.shields.io/badge/license-source--available-7C3AED)](LICENSE)

[Features](#features) · [How edits work](#how-edits-work) · [App lock](#app-lock) · [Getting started](#getting-started) · [Roadmap](#roadmap) · [License](#license)

</div>

---

Textwright is a native Android messaging app built with Kotlin and Jetpack Compose. It starts from one idea: the messages stored on *your* phone should be yours to edit. Today it is a working SMS client that can change the text and timestamp of stored messages, restore the originals, and keep itself behind a pattern or fingerprint lock. The goal is a complete, modern default messaging app.

> **Status:** early development (`v0.1.0`). Usable and tested on a small number of devices, but expect rough edges and breaking changes. See the [roadmap](#roadmap).

## Features

### Available now
- **Edit message text**: change what a stored message says.
- **Edit message date and time**: change the timestamp a message shows.
- **Restore original**: undo any edit and bring back the original text *and* date. Textwright keeps the first original, however many times you edit.
- **Edited marker**: edited messages are labelled, so you always know what you changed.
- **Syncs with other messaging apps**: edits are written so that Google Messages shows them when it becomes the default app again (see [How edits work](#how-edits-work)).
- **Crash-safe edits**: an edit interrupted by a crash or app kill is finished or cleaned up on the next launch.
- **App lock**: require a pattern, or your fingerprint, every time the app is opened (see [App lock](#app-lock)).
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

## App lock

Textwright can ask for a pattern each time it is opened. Tap the lock icon on the conversation list to set it up.

- **Pattern:** join at least four dots on a 3×3 grid. Only a salted, stretched hash of the pattern is stored (PBKDF2-HMAC-SHA256), never the pattern itself.
- **Fingerprint:** once a pattern is set, you can turn on fingerprint unlock. It uses the fingerprints already enrolled in Android, and the pattern stays available as a fallback.
- **When it locks:** on every cold start, and whenever you leave the app. Rotating the screen does not lock it.
- **Wrong attempts:** five wrong patterns in a row force a 30-second wait.
- **Changing or turning off** the lock requires the current pattern.

What the lock does not do:

- **It does not encrypt your messages.** They stay in the system SMS store, where the default SMS app and anything with SMS permission can read them. The lock stops someone holding your unlocked phone from opening Textwright.
- **It does not hide notifications.** Incoming messages still show their text in the notification.
- **There is no pattern recovery.** If you forget the pattern, the only way back in is to clear the app's data, which also deletes the saved originals of edited messages.

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
git clone https://github.com/zamansheikh/Textwright.git
cd Textwright
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### First run
1. Open Textwright and tap **Set as default** (grant the permissions it asks for).
2. Open a conversation, long-press a message, and choose **Edit**.
3. Change the text, the date or the time, then **Save**.
4. Optional: tap the lock icon on the conversation list to set a pattern and turn on fingerprint unlock.
5. To use another messaging app again, make it the default in *Settings → Apps → Default apps → SMS app*.

> **Back up first.** Editing replaces messages in your system SMS store. Try it on a test conversation before relying on it, and keep a backup of messages you care about.

## Permissions

| Permission | Why |
|---|---|
| `READ_SMS`, `RECEIVE_SMS`, `SEND_SMS` | Read, receive and send text messages |
| `RECEIVE_MMS`, `RECEIVE_WAP_PUSH` | Required to qualify as a default SMS app (MMS is not yet implemented) |
| `READ_CONTACTS` | Show contact names instead of numbers |
| `READ_PHONE_STATE` | List the active SIMs for the SIM picker |
| `POST_NOTIFICATIONS` | Notify you of incoming messages |
| `USE_BIOMETRIC` | Fingerprint unlock for the app lock (added by the AndroidX Biometric library) |

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
├── security/
│   └── AppLock.kt           app lock state, pattern hash, attempt lockout
├── sms/                     receivers, sender, SIM helpers, notifications
└── ui/                      Compose screens, lock screens, view model, theme
```

- **UI:** Jetpack Compose, Material 3.
- **State:** a single `MainViewModel` exposing `StateFlow`s; I/O runs on `Dispatchers.IO`.
- **Storage:** the system SMS provider (`Telephony.Sms`) plus a private `edits.db`.
- **App icon:** an adaptive icon built from vector drawables, with a monochrome layer for themed icons. The README artwork is in [docs/](docs/).

## Roadmap

The aim is a full-featured default messaging app. Rough order of priority:

- [ ] Samsung Messages and other apps: test and handle their re-sync behaviour
- [ ] MMS and attachments
- [ ] Unit and instrumented tests, CI
- [ ] Search
- [ ] Backup and restore, including Textwright's saved originals
- [ ] Archive, mute, block and spam handling
- [ ] App lock: hide notification content while locked, optional screenshot blocking
- [ ] Group messaging
- [ ] Scheduled sending
- [ ] RCS
- [ ] Tablet and foldable layouts, accessibility pass, localization

Ideas and priorities are open for discussion in issues.

## Contributing

Contributions are welcome, from bug reports to new features. By submitting a pull request you agree to the contribution terms in section 4 of the [LICENSE](LICENSE).

1. Open an issue describing the bug or idea first for anything non-trivial.
2. Fork the repo and create a branch from `main`.
3. Keep changes focused, and match the surrounding code style.
4. Test on a real device or emulator. SMS behaviour differs between devices and OS versions, so please say what you tested on.
5. Open a pull request with a clear description.

Particularly useful right now: testing edits against **Samsung Messages and other default apps**, and writing tests.

## License

Textwright is **source-available, not open source**. You can read the code, build it for your own personal use, and contribute to this repository. You may **not** copy or reuse the code in your own project, redistribute it, or publish your own builds. Contributions are licensed to the project owner as described in the license.

See [LICENSE](LICENSE) for the full terms. For any use it doesn't allow, contact the owner.

## Acknowledgements

Built with [Kotlin](https://kotlinlang.org/) and [Jetpack Compose](https://developer.android.com/jetpack/compose).
