package com.l1sk1sh.vladikbot.commands.owner;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.l1sk1sh.vladikbot.utils.BotUtils;
import com.l1sk1sh.vladikbot.utils.FormatUtils;
import net.dv8tion.jda.api.entities.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author l1sk1sh
 * Changes from original source:
 * - Reformatted code
 * - DI Spring
 * @author John Grosh
 */
@Service
public class SetAvatarCommand extends OwnerCommand {
    private static final Logger log = LoggerFactory.getLogger(SetAvatarCommand.class);

    @Autowired
    public SetAvatarCommand() {
        this.name = "setavatar";
        this.arguments = "<url>";
        this.help = "sets the avatar of the bot";
        this.guildOnly = false;
    }

    @Override
    protected void execute(CommandEvent event) {
        String url;
        if (event.getArgs().isEmpty()) {
            if (!event.getMessage().getAttachments().isEmpty() && event.getMessage().getAttachments().get(0).isImage()) {
                url = event.getMessage().getAttachments().get(0).getUrl();
            } else {
                url = null;
            }
        } else {
            url = event.getArgs();
        }

        try (InputStream inputStream = BotUtils.imageFromUrl(url)) {
            if (inputStream == null) {
                event.replyError("Invalid or missing URL.");

                return;
            }

            try {
                event.getSelfUser().getManager().setAvatar(Icon.from(inputStream)).queue(
                        v -> event.reply(event.getClient().getSuccess() + " Successfully changed avatar."),
                        t -> event.reply(event.getClient().getError() + " Failed to set avatar."));
                log.info("Avatar was changed by {}", FormatUtils.formatAuthor(event));
            } catch (IOException e) {
                event.replyError("Could not load from provided URL.");
            }

        } catch (IOException e) {
            log.error("Failed to download image from URL for avatar.", e);
            event.replyError("Unable to download image from specified URL.");
        }
    }
}
