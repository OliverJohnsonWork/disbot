package l1.multiheaded.vladikbot.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import l1.multiheaded.vladikbot.Bot;
import l1.multiheaded.vladikbot.services.audio.AudioHandler;

/**
 * @author Oliver Johnson
 * Changes from original source:
 * - Reformating code
 * @author John Grosh
 */
public class ShuffleCommand extends MusicCommand {
    public ShuffleCommand(Bot bot) {
        super(bot);
        this.name = "shuffle";
        this.help = "shuffles songs you have added";
        this.beListening = true;
        this.bePlaying = true;
    }

    @Override
    public void doCommand(CommandEvent event) {
        AudioHandler audioHandler = (AudioHandler) event.getGuild().getAudioManager().getSendingHandler();
        int shuffle = audioHandler.getQueue().shuffle(event.getAuthor().getIdLong());
        switch (shuffle) {
            case 0:
                event.replyError("You don't have any music in the queue to shuffle!");
                break;
            case 1:
                event.replyWarning("You only have one song in the queue!");
                break;
            default:
                event.replySuccess("You successfully shuffled your entries.");
                break;
        }
    }

}