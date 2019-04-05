package com.multiheaded.vladikbot.commands.music;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.multiheaded.vladikbot.VladikBot;

import java.util.List;

/**
 * @author Oliver Johnson
 * Changes from original source:
 * - Reformating code
 * @author John Grosh
 */
public class PlaylistsCommand extends MusicCommand {
    public PlaylistsCommand(VladikBot bot) {
        super(bot);
        this.name = "playlists";
        this.help = "shows the available playlists";
        this.aliases = new String[]{"pls"};
        this.guildOnly = true;
        this.beListening = false;
        this.beListening = false;
    }

    @Override
    public void doCommand(CommandEvent event) {
        List<String> list = bot.getPlaylistLoader().getPlaylistNames();
        if (list == null) {
            event.reply(event.getClient().getError() + " Failed to load available playlists!");
        } else if (list.isEmpty()) {
            event.reply(event.getClient().getWarning() + " There are no playlists in the Playlists folder!");
        } else {
            StringBuilder builder = new StringBuilder(event.getClient().getSuccess() + " Available playlists:\n");
            list.forEach(str -> builder.append("`").append(str).append("` "));
            builder.append("\nType `").append(event.getClient().getTextualPrefix()).append("play playlist <name>` to play a playlist");
            event.reply(builder.toString());
        }
    }
}
