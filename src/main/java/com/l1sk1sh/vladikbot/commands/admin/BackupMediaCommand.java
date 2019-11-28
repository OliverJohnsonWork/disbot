package com.l1sk1sh.vladikbot.commands.admin;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.l1sk1sh.vladikbot.Bot;
import com.l1sk1sh.vladikbot.services.BackupTextChannelService;
import com.l1sk1sh.vladikbot.services.BackupMediaService;
import com.l1sk1sh.vladikbot.settings.Const;

import java.io.File;

/**
 * @author Oliver Johnson
 */
// TODO Process arguments and work with zip from here
public class BackupMediaCommand extends AdminCommand {
    private final Bot bot;

    public BackupMediaCommand(Bot bot) {
        this.bot = bot;
        this.name = "savemedia";
        this.help = "exports all attachments of the current channel\r\n"
                + "\t\t `-b, --before <mm/dd/yyyy>` - specifies date till which export would be done\r\n"
                + "\t\t `-a, --after  <mm/dd/yyyy>` - specifies date from which export would be done\r\n"
                + "\t\t `-f` - creates new backup ignoring existing files\r\n"
                + "\t\t `-z, --zip` - zip flag that creates local copy of files from media links";
        this.arguments = "-a, -b, -f, -a, -z";
        this.guildOnly = true;
    }

    @Override
    public void execute(CommandEvent event) {
        if (bot.isLockedBackup()) {
            event.replyWarning("Can't backup media - another backup is in progress!");
            return;
        }
        event.reply("Getting attachments. Be patient...");

        String fileName = String.format("%s - %s [%s] - media list",
                event.getGuild().getName(),
                event.getChannel().getName(),
                event.getChannel().getId());

        BackupTextChannelService backupTextChannelService = new BackupTextChannelService(
                bot,
                event.getChannel().getId(),
                Const.BACKUP_HTML_DARK,
                bot.getBotSettings().getLocalTmpPath(),
                event.getArgs().split(" ")
        );

        /* Creating separate thread to allow users to work with the Bot while backup is running */
        new Thread(() -> {

            /* Creating new thread from text backup service and waiting for it to finish */
            Thread backupTextChannelServiceThread = new Thread(backupTextChannelService);
            backupTextChannelServiceThread.start();
            try {
                backupTextChannelServiceThread.join();
            } catch (InterruptedException e) {
                event.replyError("Text channel backup process was interrupted!");
                return;
            }

            if (backupTextChannelService.hasFailed()) {
                event.replyError(String.format("Text channel backup has failed: `[%s]`", backupTextChannelService.getFailMessage()));
                return;
            }

            File exportedTextFile = backupTextChannelService.getBackupFile();

            BackupMediaService backupMediaService = new BackupMediaService(
                    bot,
                    event.getChannel().getId(),
                    exportedTextFile,
                    bot.getBotSettings().getLocalTmpPath(),
                    event.getArgs().split(" ")
            );

            /* Creating new thread from media backup service and waiting for it to finish */
            Thread backupMediaServiceThread = new Thread(backupMediaService);
            backupMediaServiceThread.start();
            try {
                backupMediaServiceThread.join();
            } catch (InterruptedException e) {
                event.replyError("Media backup process was interrupted!");
                return;
            }

            if (backupMediaService.hasFailed()) {
                event.replyError(String.format("Media backup has filed: `[%s]`", backupMediaService.getFailMessage()));
                return;
            }

            File attachmentHtmlFile = backupMediaService.getAttachmentHtmlFile();
            File attachmentTxtFile = backupMediaService.getAttachmentsTxtFile();

            if (!attachmentHtmlFile.exists() || !attachmentTxtFile.exists()) {
                event.replyError("Failed to find media files!");
                return;
            }

            if (attachmentHtmlFile.length() < Const.EIGHT_MEGABYTES_IN_BYTES) {
                event.getTextChannel().sendFile(attachmentHtmlFile, attachmentHtmlFile.getName()).queue();
            } else if (attachmentTxtFile.length() < Const.EIGHT_MEGABYTES_IN_BYTES) {
                event.getTextChannel().sendFile(attachmentTxtFile, attachmentTxtFile.getName()).queue();
            } else {
                event.replyWarning("File is too big! Max file-size is 8 MiB for normal and 50 MiB for nitro users!\r\n" +
                        "Limit executed command with period: --before <mm/dd/yy> --after <mm/dd/yy>");
            }

            if (backupMediaService.doZip()) {
                event.replySuccess("Zip with uploaded media files could now be downloaded from local storage.");
            }
        }).start();
    }
}