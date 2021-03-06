package com.l1sk1sh.vladikbot.commands.dj;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.l1sk1sh.vladikbot.data.repository.GuildSettingsRepository;
import com.l1sk1sh.vladikbot.models.queue.QueuedTrack;
import com.l1sk1sh.vladikbot.services.audio.AudioHandler;
import com.l1sk1sh.vladikbot.services.audio.PlayerManager;
import com.l1sk1sh.vladikbot.settings.BotSettingsManager;
import com.l1sk1sh.vladikbot.settings.Const;
import com.l1sk1sh.vladikbot.utils.FormatUtils;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * @author l1sk1sh
 * Changes from original source:
 * - Reformatted code
 * - DI Spring
 * @author John Grosh
 */
@Service
public class PlayNextCommand extends DJCommand {

    private final BotSettingsManager settings;

    @Autowired
    public PlayNextCommand(BotSettingsManager settings, GuildSettingsRepository guildSettingsRepository, PlayerManager playerManager) {
        super(guildSettingsRepository, playerManager);
        this.settings = settings;
        this.name = "playnext";
        this.help = "plays a single song next";
        this.arguments = "<title|URL>";
        this.beListening = true;
        this.bePlaying = false;
    }

    @Override
    public void doCommand(CommandEvent event) {
        if (event.getArgs().isEmpty() && event.getMessage().getAttachments().isEmpty()) {
            event.replyWarning("Please include a song title or URL!");
            return;
        }
        String args = event.getArgs().startsWith("<") && event.getArgs().endsWith(">")
                ? event.getArgs().substring(1, event.getArgs().length() - 1)
                : event.getArgs().isEmpty() ? event.getMessage().getAttachments().get(0).getUrl() : event.getArgs();
        event.reply(String.format("%1$s Loading... `[%2$s]`", settings.get().getLoadingEmoji(), args),
                m -> super.playerManager.loadItemOrdered(event.getGuild(), args, new ResultHandler(m, event, false)));
    }

    private final class ResultHandler implements AudioLoadResultHandler {
        private final Message message;
        private final CommandEvent event;
        private final boolean ytsearch;

        private ResultHandler(Message message, CommandEvent event, boolean ytsearch) {
            this.message = message;
            this.event = event;
            this.ytsearch = ytsearch;
        }

        @SuppressWarnings("DuplicatedCode")
        private void loadSingle(AudioTrack track) {
            if (settings.get().isTooLong(track)) {
                message.editMessage(FormatUtils.filter(String.format(
                        "%1$s This track (**%2$s**) is longer than the allowed maximum: `%3$s` > `%4$s`.",
                        event.getClient().getWarning(),
                        track.getInfo().title,
                        FormatUtils.formatTimeTillHours(track.getDuration()),
                        FormatUtils.formatTimeTillHours(settings.get().getMaxSeconds() * 1000)))
                ).queue();
                return;
            }
            AudioHandler audioHandler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
            int pos = Objects.requireNonNull(audioHandler).addTrackToFront(new QueuedTrack(track, event.getAuthor())) + 1;

            String addMessage = FormatUtils.filter(String.format(
                    "%1$s Added **%2$s** (`%3$s`) %4$s.",
                    event.getClient().getSuccess(),
                    track.getInfo().title,
                    FormatUtils.formatTimeTillHours(track.getDuration()),
                    ((pos == 0) ? "to begin playing" : " to the queue at position " + pos))
            );
            message.editMessage(addMessage).queue();
        }

        @Override
        public void trackLoaded(AudioTrack track) {
            loadSingle(track);
        }

        @Override
        public void playlistLoaded(AudioPlaylist playlist) {
            AudioTrack single;
            if (playlist.getTracks().size() == 1 || playlist.isSearchResult()) {
                single = (playlist.getSelectedTrack() == null)
                        ? playlist.getTracks().get(0) : playlist.getSelectedTrack();
            } else if (playlist.getSelectedTrack() != null) {
                single = playlist.getSelectedTrack();
            } else {
                single = playlist.getTracks().get(0);
            }
            loadSingle(single);
        }

        @Override
        public void noMatches() {
            if (ytsearch) {
                message.editMessage(FormatUtils.filter(
                        String.format("%1$s No results found for `%2$s1`.",
                                event.getClient().getWarning(),
                                event.getArgs()))
                ).queue();
            } else {
                playerManager.loadItemOrdered(event.getGuild(),
                        Const.YT_SEARCH_PREFIX + event.getArgs(), new ResultHandler(message, event, true));
            }
        }

        @Override
        public void loadFailed(FriendlyException throwable) {
            if (throwable.severity == FriendlyException.Severity.COMMON) {
                message.editMessage(String.format("%1$s Error loading: %2$s.",
                        event.getClient().getError(),
                        throwable.getLocalizedMessage())
                ).queue();
            } else {
                message.editMessage(String.format("%1$s Error loading track.", event.getClient().getError())).queue();
            }
        }
    }
}
