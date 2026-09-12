# 67 Hit Discord

A RuneLite Plugin Hub plugin that captures a screenshot when the local player deals **exactly 67 damage**.

The plugin can optionally post the screenshot to a Discord channel using a Discord webhook.

## Features

- Triggers only on a **67-damage hitsplat** dealt by the local player.
- The trigger value is fixed and cannot be changed in the plugin settings.
- Captures the next RuneLite frame after the qualifying hit.
- Can include the full RuneLite client frame in the screenshot.
- Can save a local copy through RuneLite's screenshot system.
- Can optionally upload the screenshot to Discord.
- Discord messages include:
  - local RuneScape player name
  - target NPC or player name
  - damage amount
  - Discord-native timestamp
- RuneLite formatting tags are removed from actor names before they are posted.
- A configurable cooldown prevents one multi-hit or repeated-splat attack from creating several posts.

## Default settings

| Setting | Default |
| --- | --- |
| Screenshot cooldown | 1200 ms |
| Include RuneLite frame | Enabled |
| Save locally | Enabled |
| Post to Discord | Disabled |
| Discord webhook URL | Empty |

Discord posting is **opt-in** and is disabled by default.

## Discord setup

1. Create a webhook in the Discord channel where you want 67-hit screenshots to appear.
2. Copy the webhook URL.
3. Open the **67 Hit Discord** settings in RuneLite.
4. Paste the URL into **Discord webhook URL**.
5. Enable **Post to Discord**.

Treat the webhook URL like a password. Anyone with the URL may be able to post through that webhook. If it is exposed, delete or regenerate it in Discord.

## What triggers a screenshot?

A screenshot is requested when all of the following are true:

- the hitsplat amount is exactly **67**
- RuneLite reports the hitsplat as belonging to the local player
- the hitsplat is applied to an actor other than the local player
- the screenshot cooldown is not active

The damage value is intentionally hard-coded to 67.

## Discord message

A typical NPC post looks like:

```text
Saint Mina hit Vardorvis for 67 damage.
<Discord-local timestamp>
```

A PvP post identifies the target as a player:

```text
Saint Mina hit player SomeName for 67 damage.
<Discord-local timestamp>
```

The actual Discord message uses Markdown formatting and a native Discord timestamp, so the timestamp is rendered in each viewer's local time zone.

## Privacy and third-party communication

When **Post to Discord** is enabled, the plugin makes an HTTPS request to the Discord webhook configured by the user.

That request can transmit:

- your RuneScape display name
- the target NPC or player name
- the 67-damage hit information
- the captured RuneLite screenshot
- your IP address as part of the connection to Discord

Discord is a third-party service and is not controlled or verified by the RuneLite Developers.

If **Post to Discord** is disabled, the plugin does not send the screenshot to Discord.

## Local screenshots

When **Save locally** is enabled, triggered screenshots are saved using RuneLite's screenshot system in the `Damage Hits` screenshot subdirectory.

## Development

This repository follows the RuneLite Plugin Hub project layout.

To run the development client, use the Gradle `run` task.

The project targets Java 11 bytecode as required by Plugin Hub plugins.

## License

This project is licensed under the BSD 2-Clause License. See [LICENSE](LICENSE).
