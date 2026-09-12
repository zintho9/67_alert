package com.hitdiscord;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(HitDiscordConfig.GROUP)
public interface HitDiscordConfig extends Config
{
    String GROUP = "hit-discord";


    @Range(min = 0, max = 10000)
    @ConfigItem(
        keyName = "cooldownMs",
        name = "Screenshot cooldown",
        description = "Ignore additional matching hitsplats for this many milliseconds after a screenshot trigger",
        position = 0
    )
    default int cooldownMs()
    {
        return 1200;
    }

    @ConfigItem(
        keyName = "includeClientFrame",
        name = "Include RuneLite frame",
        description = "Include the full RuneLite window, including side panel and title bar",
        position = 1
    )
    default boolean includeClientFrame()
    {
        return true;
    }

    @ConfigItem(
        keyName = "saveLocally",
        name = "Save locally",
        description = "Also save triggered screenshots to your RuneLite screenshots folder",
        position = 2
    )
    default boolean saveLocally()
    {
        return true;
    }

    @ConfigItem(
        keyName = "postToDiscord",
        name = "Post to Discord",
        description = "Upload triggered screenshots to the configured Discord webhook",
        warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
        position = 3
    )
    default boolean postToDiscord()
    {
        return false;
    }

    @ConfigItem(
        keyName = "discordWebhook",
        name = "Discord webhook URL",
        description = "Discord webhook URL for the channel that should receive screenshots. Treat this URL like a password.",
        secret = true,
        position = 4
    )
    default String discordWebhook()
    {
        return "";
    }
}
