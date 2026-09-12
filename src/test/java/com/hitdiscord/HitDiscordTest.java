package com.hitdiscord;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class HitDiscordTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(HitDiscordPlugin.class);
        RuneLite.main(args);
    }
}
