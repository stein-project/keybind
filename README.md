# Keybind

A [Stein Loader](https://github.com/Stein-Project) mod for Minecraft 1.8.9: chat messages and commands on a key.
Press F and the game sends `/home`.

- Each bind has a key (or a mouse button), an optional modifier (Ctrl, Shift or Alt) and a text.
- One key can send several messages: separate them with `|` (`/home | gg`). They go out one after the other, with a
  gap between them (half a second by default, adjustable) so the server does not kick you for spam.
- When the same key has a bind with a modifier and one without, only the more specific one fires: Ctrl+F does not
  also send what is on F.
- It fires only in the game, with no screen open, and only on the press: holding the key does not repeat.
- Messages go out exactly as if you typed them in the chat.

Set it up on the Panel (Right Shift) > Keybind. The binds are kept in `config/steinkeybind.json`.

<!-- Screenshot: docs/keybind.png (the Keybind page on the Panel) -->

## Download

The [Stein Client](https://github.com/Stein-Project/stein-client/releases/latest) installs and updates it for you.
By hand: download `Keybind.steinmod` from the
[latest release](https://github.com/Stein-Project/keybind/releases/latest), put it in `.minecraft/mods` and play
with the **Stein Loader** profile.

## Building

You need **JDK 25 or newer**.
The [Stein SDK](https://github.com/Stein-Project/stein-sdk) Gradle plugin prepares Minecraft 1.8.9 for compiling the
first time, from your own installation or from Mojang.

```sh
./gradlew build            # build/libs/Keybind.steinmod (also checks the formatting)
./gradlew spotlessApply    # format the code (google-java-format, AOSP style) and add the licence header
./gradlew installMod       # build and copy into .minecraft/mods
```

Before a Stein SDK release (developing the SDK and the mod together), build against a local SDK: in a clone of
[sdk](https://github.com/Stein-Project/sdk) run `./gradlew build publishPlugin`, then here
`./gradlew build -Pstein.sdkDir=<path to that clone>` (the plugin comes from its `build/maven-repo`, the SDK from
its `build/sdk/stein-sdk.jar`).

## Contributing

Bug reports, ideas and pull requests are welcome. Read the
[contributing guide](https://github.com/Stein-Project/.github/blob/main/CONTRIBUTING.md) first; your first pull
request asks you to sign the [CLA](https://github.com/Stein-Project/.github/blob/main/CLA.md). For a change to this
mod, also check that:

- an existing `config/steinkeybind.json` still loads (the JSON keys `binds`, `key`, `modifier`, `text` and
  `delayTicks` are what players already have);
- binds with and without a modifier on the same key still fire as described above;
- the text is translated in `en_US`, `pt_BR` and `es_ES` (`src/main/resources/assets/steinkeybind/lang`).

Releases are made by the maintainer: a `v<version>` tag, with `changelog/<version>.en_US.md` (and optionally `.pt_BR.md`)
written first (see `.github/workflows/release.yml`).

## Licence

Keybind is licensed under the [Mozilla Public License 2.0](LICENSE). Third-party notices are in [NOTICE](NOTICE).
The Stein name and logo are not covered by the licence: see the
[trademark policy](https://github.com/Stein-Project/.github/blob/main/TRADEMARK.md).

Stein is not an official Minecraft product. It is not approved by or associated with Mojang or Microsoft.

Created by [Breno Xavier](https://github.com/brenoxavier).
