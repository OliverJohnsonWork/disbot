package l1.multiheaded.vladikbot;

import l1.multiheaded.vladikbot.settings.BotSettings;
import l1.multiheaded.vladikbot.settings.Constants;
import l1.multiheaded.vladikbot.utils.OtherUtils;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author Oliver Johnson
 * Changes from original source:
 * - Reformating code
 * - Removal of update
 * - Addition of moderation Listener
 * - Addition of permission handler
 * @author John Grosh
 */
class Listener extends ListenerAdapter {
    private static final Logger log = LoggerFactory.getLogger(Listener.class);

    private final Bot bot;
    private final BotSettings botSettings;

    Listener(Bot bot) {
        this.bot = bot;
        this.botSettings = bot.getBotSettings();
    }

    @Override
    public void onReady(ReadyEvent event) {
        if (event.getJDA().getGuilds().isEmpty()) {
            log.warn("This bot is not on any guilds! Use the following link to add the bot to your guilds!");
            log.warn(event.getJDA().asBot().getInviteUrl(Constants.RECOMMENDED_PERMS));
        }

        event.getJDA().getGuilds().forEach((guild) ->
        {
            try {
                String defaultPlaylist = bot.getGuildSettings(guild).getDefaultPlaylist();
                VoiceChannel vc = bot.getGuildSettings(guild).getVoiceChannel(guild);
                if (defaultPlaylist != null && vc != null && bot.getPlayerManager().setUpHandler(guild).playFromDefault()) {
                    guild.getAudioManager().openAudioConnection(vc);
                }
            } catch (Exception ignore) { /* Ignore */ }

            List<Permission> missingPermissions =
                    OtherUtils.getMissingPermissions(guild.getSelfMember().getPermissions(), Constants.RECOMMENDED_PERMS);
            if (missingPermissions != null) {
                log.warn("Bot in guild '{}' doesn't have following recommended permissions {}.",
                        guild.getName(), Arrays.toString(missingPermissions.toArray()));
            }
        });

        if (botSettings.shouldRotateTextBackup() && botSettings.shouldRotateMediaBackup()) {
            int timeDifference = Math.abs(botSettings.getTargetHourForTextBackup()
                    - botSettings.getTargetHourForMediaBackup());
            if (timeDifference < 1) {
                log.warn("Rotation backups should have at least 1 hour difference");
                botSettings.setTargetHourForTextBackup(botSettings.getTargetHourForTextBackup() - 1);
                botSettings.setTargetHourForMediaBackup(botSettings.getTargetHourForMediaBackup() + 2);
            }
        }

        if (botSettings.shouldRotateMediaBackup()) {
            log.info("Enabling Rotation media backup service...");
            bot.getRotatingBackupMediaService().enableExecution();
        }

        if (botSettings.shouldRotateTextBackup()) {
            log.info("Enabling Rotation text backup service...");
            bot.getRotatingBackupChannelService().enableExecution();
        }
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        bot.getNowPlayingHandler().onMessageDelete(event.getGuild(), event.getMessageIdLong());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message message = event.getMessage();

        if (!message.getAuthor().isBot()) {
            bot.getAutoModerationManager().performAutomod(message);
        }
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        bot.shutdown();
    }
}
