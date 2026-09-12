# 67 Hit Discord

A RuneLite Plugin Hub-style development project that captures the RuneLite client when the local player deals an exact configured damage hitsplat.

## Default behavior

- Trigger damage: **67 (fixed; not configurable)**
- Screenshot cooldown: **1200 ms**
- Include the full RuneLite client frame: **enabled**
- Save a local copy: **enabled**
- Discord uploads: **disabled by default**
- Discord webhook URL: configurable secret field
- Discord message includes the target name and hit damage, e.g. **Hit Vardorvis for 67.**
- PvP hits explicitly include the other player's RuneScape name, e.g. **Hit player SomeName for 67.**
- Discord messages use Discord's native timestamp markup so each viewer sees the hit time in their own local time zone.
- Discord messages include the local RuneScape player's name captured when the qualifying hit is detected.

## Discord setup

1. In Discord, create a webhook for the channel you want to use.
2. Copy the webhook URL.
3. In RuneLite, open **67 Hit Discord** settings.
4. Paste the URL into **Discord webhook URL**.
5. Enable **Post to Discord**.

The webhook URL contains a secret token. Do not post it publicly or commit it into source code. If it is exposed, delete/regenerate the webhook in Discord.

## What counts as a trigger?

The plugin subscribes to RuneLite's `HitsplatApplied` event and triggers when:

- `hitsplat.getAmount()` exactly equals the configured value (67 by default)
- `hitsplat.isMine()` is true
- the hitsplat is not being applied to your own player
- the screenshot cooldown is not currently active

The default cooldown is **1200 ms**. Once one qualifying hit triggers a screenshot, any additional matching hitsplats during that window are ignored. This prevents a multi-hit or repeated-splat attack from generating several screenshots and Discord posts.

## Screenshots

If **Include RuneLite frame** is enabled, RuneLite's `ImageCapture.addClientFrame(...)` is used so the screenshot contains the game canvas plus RuneLite UI/sidebar.

Local copies are stored through RuneLite's screenshot system under the `Damage Hits` screenshot subdirectory.

## Testing

The trigger is intentionally fixed at exactly **67 damage** and is not exposed as a RuneLite setting.

First test with **Post to Discord** disabled and confirm a local screenshot appears. Then configure the webhook, enable Discord posting, and repeat the test.

## Plugin Hub note

This plugin communicates with Discord when the user explicitly enables that feature. RuneLite requires third-party-server features to be opt-in and display a warning about data being sent outside RuneLite.


## Discord message formatting

RuneLite formatting tags such as `<col=00ffff>...</col>` are stripped from actor names before posting.
The local player name, target name, and damage are emphasized, and the Discord-native timestamp is placed on its own line.

Example:

**Saint Mina** hit **Undead Combat dummy** for **67** damage.  
`<t:UNIX_TIMESTAMP:F>`
