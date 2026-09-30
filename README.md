# PayNotify

Client-side Fabric mod for **Minecraft Java 1.21.11** that adds three clickable buttons to the
**end of DonutSMP payment lines in chat**:

```
Player123 paid you $50  [2X] [R] [X]
```

* The original message is untouched; the buttons are appended on the same chat line.
* **[2X]** (gold) opens a confirmation dialog: player name, amount, doubled amount, YES / CANCEL.
* **[R]** (aqua) opens a confirmation dialog to refund: player name, amount, YES / CANCEL.
* **[X]** (red) hides that one message from your local chat. The server and all other messages are unaffected.
* **Safety:** this version never sends money and never runs `/pay`. YES only prints a local
  "payment action is currently disabled" message.
* Only active while connected to DonutSMP. Other servers are not affected.

> This project was written without being able to compile or test it in the environment that produced it.
> Build it with `build.bat` and check the result in game.

## Build (Windows)

1. Install a **JDK 21** (for example Temurin 21 from https://adoptium.net) if you do not have one.
2. Double-click **`build.bat`**. It uses the included Gradle wrapper, so no Gradle install is needed.
   The first build downloads Minecraft and Fabric, which takes a few minutes and needs internet.
3. When it says **BUILD SUCCEEDED**, the jar is at `build\libs\paynotify-1.0.0.jar`.

## Install

Copy `paynotify-1.0.0.jar` and **Fabric API** into your `mods` folder and start a Fabric 1.21.11 profile.

## Controls and commands

* Controls menu, category **PayNotify**: "Toggle PayNotify buttons" (default key `Y`).
* `/paynotify toggle` - enable or disable the buttons
* `/paynotify reload` - reload `config/paynotify.json`
* `/paynotify status` - show whether the buttons are active on the current server

These commands run locally and are never sent to the server. (The buttons use
`/paynotify action ...` internally.)

## Configuration: `config/paynotify.json`

Created on first launch. Important settings:

| Setting | Meaning |
| --- | --- |
| `paymentPattern` | Java regex tested against each server message. Must contain the named groups `(?<player>...)` and `(?<amount>...)`. Backslashes must be doubled in JSON. |
| `serverAddressContains` | Buttons are only added when the server address contains one of these fragments. Default `["donutsmp"]`. Use `["*"]` to allow every server (useful for testing). |
| `enabled` | Master switch (also toggled by the keybind). |
| `doubleColor`, `refundColor`, `closeColor`, `bracketColor` | Hex colors. |
| `doubleLabel`, `refundLabel`, `closeLabel` | Button text. |
| `leftBracket`, `rightBracket`, `boldLabels` | Button look. |
| `spaceBeforeButtons`, `spaceBetweenButtons` | Spacing. |
| `showHoverText` | Tooltip on hover. |
| `maxTrackedPayments` | How many payments keep working buttons (default 200). |

The default pattern matches `<Name> paid you $<amount>` at the start of a message, for example
`Steve paid you $2.5M`. Amounts like `1,500`, `2.5K`, `3M`, `1.2B` are understood. If DonutSMP ever
changes its wording, edit `paymentPattern` and run `/paynotify reload`. No rebuild is needed.

## Notes

* Only server (system) messages are modified. If a server sends payment lines as signed player chat,
  they will not get buttons.
* [X] only edits your local chat history for this session; it never changes what the server sent.
