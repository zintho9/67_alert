package com.hitdiscord;

import com.google.inject.Provides;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.Player;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.ImageCapture;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.Text;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@PluginDescriptor(
    name = "67 Hit Discord",
    description = "Screenshots exact-damage hits and can post them to Discord",
    tags = {"screenshot", "discord", "hitsplat", "damage"}
)
@Slf4j
public class HitDiscordPlugin extends Plugin
{
    private static final int TRIGGER_DAMAGE = 67;
    private static final MediaType PNG = MediaType.parse("image/png");
    private static final MediaType JSON = MediaType.parse("application/json");

    private long lastTriggerTimeMs = Long.MIN_VALUE;

    @Inject
    private Client client;

    @Inject
    private HitDiscordConfig config;

    @Inject
    private DrawManager drawManager;

    @Inject
    private ImageCapture imageCapture;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private OkHttpClient httpClient;

    @Provides
    HitDiscordConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(HitDiscordConfig.class);
    }

    @Override
    protected void startUp()
    {
        lastTriggerTimeMs = Long.MIN_VALUE;
    }

    @Override
    protected void shutDown()
    {
        lastTriggerTimeMs = Long.MIN_VALUE;
    }

    @Subscribe
    public void onHitsplatApplied(HitsplatApplied event)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        Hitsplat hitsplat = event.getHitsplat();
        if (hitsplat == null
            || hitsplat.getAmount() != TRIGGER_DAMAGE
            || !hitsplat.isMine())
        {
            return;
        }

        // A "mine" hitsplat is damage attributed to the local player.
        // Ignore a splat applied to the local player itself.
        if (event.getActor() == client.getLocalPlayer())
        {
            return;
        }

        long now = System.currentTimeMillis();
        int cooldownMs = config.cooldownMs();

        if (cooldownMs > 0
            && lastTriggerTimeMs != Long.MIN_VALUE
            && now - lastTriggerTimeMs < cooldownMs)
        {
            log.debug(
                "Ignoring {} damage hitsplat because screenshot cooldown is active",
                hitsplat.getAmount()
            );
            return;
        }

        lastTriggerTimeMs = now;
        Instant detectedAt = Instant.now();

        String localPlayerName = client.getLocalPlayer() != null
            ? client.getLocalPlayer().getName()
            : null;

        if (localPlayerName != null)
        {
            localPlayerName = Text.removeTags(localPlayerName).trim();
        }

        if (localPlayerName == null || localPlayerName.isEmpty())
        {
            localPlayerName = "Unknown player";
        }

        String targetName = event.getActor().getName();
        boolean playerTarget = event.getActor() instanceof Player;

        if (targetName != null)
        {
            targetName = Text.removeTags(targetName).trim();
        }

        if (targetName == null || targetName.isEmpty())
        {
            targetName = playerTarget ? "player" : "target";
        }

        captureTriggeredHit(
            hitsplat.getAmount(),
            targetName,
            playerTarget,
            detectedAt,
            localPlayerName
        );
    }

    private void captureTriggeredHit(
        int damage,
        String targetName,
        boolean playerTarget,
        Instant detectedAt,
        String localPlayerName
    )
    {
        final String capturedTargetName = targetName;
        final boolean capturedPlayerTarget = playerTarget;
        final Instant capturedDetectedAt = detectedAt;
        final String capturedLocalPlayerName = localPlayerName;

        drawManager.requestNextFrameListener((Image image) ->
            executor.submit(() -> processScreenshot(
                image,
                damage,
                capturedTargetName,
                capturedPlayerTarget,
                capturedDetectedAt,
                capturedLocalPlayerName
            )));
    }

    private void processScreenshot(
        Image image,
        int damage,
        String targetName,
        boolean playerTarget,
        Instant detectedAt,
        String localPlayerName
    )
    {
        BufferedImage screenshot = config.includeClientFrame()
            ? imageCapture.addClientFrame(image)
            : ImageUtil.bufferedImageFromImage(image);

        if (config.saveLocally())
        {
            imageCapture.saveScreenshot(
                screenshot,
                damage + " damage hit",
                "Damage Hits",
                false,
                false
            );
        }

        if (!config.postToDiscord())
        {
            return;
        }

        String webhook = config.discordWebhook().trim();
        if (!isDiscordWebhook(webhook))
        {
            log.warn("Discord upload is enabled, but the webhook URL is missing or invalid");
            return;
        }

        try
        {
            uploadToDiscord(
                webhook,
                screenshot,
                damage,
                targetName,
                playerTarget,
                detectedAt,
                localPlayerName
            );
        }
        catch (IOException e)
        {
            log.warn("Unable to encode {} damage screenshot", damage, e);
        }
    }

    private void uploadToDiscord(
        String webhook,
        BufferedImage screenshot,
        int damage,
        String targetName,
        boolean playerTarget,
        Instant detectedAt,
        String localPlayerName
    ) throws IOException
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        if (!ImageIO.write(screenshot, "PNG", output))
        {
            throw new IOException("No PNG image writer is available");
        }

        byte[] png = output.toByteArray();
        long discordTimestamp = detectedAt.getEpochSecond();
        String timestamp = "<t:" + discordTimestamp + ":F>";

        String hitMessage = playerTarget
            ? "**" + escapeDiscordMarkdown(localPlayerName) + "** hit player **"
                + escapeDiscordMarkdown(targetName) + "** for **" + damage + "** damage."
            : "**" + escapeDiscordMarkdown(localPlayerName) + "** hit **"
                + escapeDiscordMarkdown(targetName) + "** for **" + damage + "** damage.";

        String message = hitMessage + "\n" + timestamp;
        String payload = "{\"content\":\"" + escapeJson(message) + "\"}";

        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("payload_json", null, RequestBody.create(JSON, payload))
            .addFormDataPart(
                "files[0]",
                damage + "-damage-hit.png",
                RequestBody.create(PNG, png)
            )
            .build();

        Request request = new Request.Builder()
            .url(webhook)
            .post(body)
            .build();

        // Do not perform network I/O on the RuneLite client thread.
        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.warn("Failed to post damage screenshot to Discord", e);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (Response ignored = response)
                {
                    if (!response.isSuccessful())
                    {
                        log.warn("Discord webhook returned HTTP {}", response.code());
                    }
                    else
                    {
                        log.debug("Posted {} damage screenshot to Discord", damage);
                    }
                }
            }
        });
    }

    private static String escapeDiscordMarkdown(String value)
    {
        return value
            .replace("\\", "\\\\")
            .replace("*", "\\*")
            .replace("_", "\\_")
            .replace("~", "\\~")
            .replace("`", "\\`")
            .replace("|", "\\|");
    }

    private static String escapeJson(String value)
    {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    private static boolean isDiscordWebhook(String url)
    {
        return url.startsWith("https://discord.com/api/webhooks/")
            || url.startsWith("https://discordapp.com/api/webhooks/");
    }
}
