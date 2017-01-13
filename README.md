# Mangosta for Android

Mangosta for Android is an open source, open standard, XMPP/Jabber client.

It is designed to be a technology demonstration: it is not available on the Play Store, F-Droid or any other app store. You can install it as you wish.

Mangosta for Android is part of the [MongooseIM platform](https://github.com/esl/MongooseIM).

It comes with features such as:
* Group chats (MUC Light)
* One-to-one chats
* Microblogging
* Block/unblock users
* Contacts management (Roster)
* Stickers (Bits of Binary)
* Token-based reconnection
* Instant stream resumption
* Background service managing the xmpp connection and sending notifications (when the app is closed)

It includes unit tests and UI tests (using Espresso).

2 different implementations (same features in both):
* **master** branch: only xmpp
* **use.mongoose.rest.api** branch: xmpp + some features using the Mongoose REST API

Feel free to comment, we will be glad to hear from your uses.
You can [report issues](https://github.com/esl/mangosta-android/issues) and/or [contribute](https://github.com/esl/mangosta-android/pulls).
