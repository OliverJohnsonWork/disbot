package com.multiheaded.vladikbot.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.multiheaded.vladikbot.Bot;
import com.multiheaded.vladikbot.services.audio.AudioHandler;
import com.multiheaded.vladikbot.models.queue.QueuedTrack;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;

/**
 * @author Oliver Johnson
 * Changes from original source:
 * - Reformating code
 * @author John Grosh
 */
public class RemoveCommand extends MusicCommand {
    public RemoveCommand(Bot bot) {
        super(bot);
        this.name = "remove";
        this.aliases = new String[]{"delete"};
        this.help = "removes a song from the queue";
        this.arguments = "<position|all>";
        this.beListening = true;
        this.bePlaying = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        AudioHandler audioHandler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        if (audioHandler.getQueue().isEmpty()) {
            event.replyError("There is nothing in the queue!");
            return;
        }

        if (event.getArgs().equalsIgnoreCase("all")) {
            int count = audioHandler.getQueue().removeAll(event.getAuthor().getIdLong());
            if (count == 0) {
                event.replyWarning("You don't have any songs in the queue!");
            } else {
                event.replySuccess(String.format("Successfully removed your %1$s entries.", count));
            }
            return;
        }

        int pos;
        try {
            pos = Integer.parseInt(event.getArgs());
        } catch (NumberFormatException e) {
            pos = 0;
        }

        if ((pos < 1) || (pos > audioHandler.getQueue().size())) {
            event.replyError(String.format("Position must be a valid integer between 1 and %1$s!",
                    audioHandler.getQueue().size()));
            return;
        }

        boolean isDJ = event.getMember().hasPermission(Permission.MANAGE_SERVER);
        if (!isDJ) {
            isDJ = event.getMember().getRoles().contains(bot.getGuildSettings(event.getGuild()).getDjRole(event.getGuild()));
        }

        QueuedTrack queuedTrack = audioHandler.getQueue().get(pos - 1);
        if (queuedTrack.getIdentifier() == event.getAuthor().getIdLong()) {
            audioHandler.getQueue().remove(pos - 1);
            event.replySuccess(String.format("Removed **%1$s** from the queue", queuedTrack.getTrack().getInfo().title));
        } else if (isDJ) {
            audioHandler.getQueue().remove(pos - 1);
            User user;
            try {
                user = event.getJDA().getUserById(queuedTrack.getIdentifier());
            } catch (Exception e) {
                user = null;
            }
            event.replySuccess(String.format(
                    "Removed **%1$s** from the queue (requested by *%2$s*)",
                    queuedTrack.getTrack().getInfo().title,
                    ((user == null) ? "someone" : user.getName()))
            );
        } else {
            event.replyError(String.format("You cannot remove **%1$s** because you didn't add it!",
                    queuedTrack.getTrack().getInfo().title));
        }
    }
}
