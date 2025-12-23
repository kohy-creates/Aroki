package xyz.kohara.features.commands;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import xyz.kohara.features.commands.slash.ServerCommand;
import xyz.kohara.features.moderation.ModerationSlashCommands;
import xyz.kohara.features.music.MusicSlashCommands;

import java.util.ArrayList;
import java.util.Map;

public class SlashCommands {

    public static final ArrayList<CommandData> COMMANDS = new ArrayList<>();
    private static final String SEND_PUBLICLY_TIP = "Share the output with everyone?";

    public static final OptionData SEND_PUBLICLY = new OptionData(OptionType.BOOLEAN, "public", SlashCommands.SEND_PUBLICLY_TIP, false);

    static {
        COMMANDS.add(Commands.slash("ping", "Pong!"));

        OptionData serverOption = new OptionData(OptionType.STRING, "server", "Discord server", true);
        for (Map.Entry<String, String> entry : ServerCommand.SERVER_LIST.entrySet()) {
            serverOption.addChoice(entry.getKey(), entry.getValue());
        }

        COMMANDS.add(Commands.slash("discord", "Get links for other Discord servers")
                .addOptions(serverOption, SEND_PUBLICLY)
        );

        COMMANDS.add(Commands.slash("tags", "List all registered tags")
                .addOptions(SEND_PUBLICLY)
        );

        COMMANDS.add(Commands.slash("avatar", "Returns the avatar (profile picture) of the chosen user")
                .addOption(OptionType.USER, "user", "User", true)
                .addOptions(SEND_PUBLICLY)
        );

        COMMANDS.add(Commands.slash("close", "Closes the support thread")
                .addOptions(
                        new OptionData(OptionType.STRING, "action", "Resolution type", false)
                                .addChoice("Resolve", "resolve")
                                .addChoice("Invalidate", "invalid")
                                .addChoice("Duplicate", "duplicate")
                )
                .addOptions(
                        new OptionData(OptionType.CHANNEL, "duplicate_of", "What thread does this duplicate? Only applies if resolution type is 'Duplicate'", false),
                        new OptionData(OptionType.STRING, "note", "Note on closing from staff. Can be empty", false)
                )
        );

        COMMANDS.add(Commands.slash("link", "Links your MC account and Discord for some cool shenanigans")
                .addOption(OptionType.INTEGER, "code", "4 digit code you got from /link in-game", true)
        );

        COMMANDS.add(Commands.slash("unlink", "Unlinks your MC and Discord accounts"));

        COMMANDS.add(Commands.slash("getlinked", "Get specified member's linked Minecraft account")
                .addOption(OptionType.USER, "target", "Target member"));

        COMMANDS.addAll(MusicSlashCommands.MUSIC_COMMANDS);
        COMMANDS.addAll(ModerationSlashCommands.MODERATION_COMMANDS);
    }
}
