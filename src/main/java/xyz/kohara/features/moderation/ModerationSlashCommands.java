package xyz.kohara.features.moderation;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import xyz.kohara.features.commands.SlashCommands;

import java.util.ArrayList;

public class ModerationSlashCommands {

    private static SlashCommandData slashCommand(String name, String description) {
        return Commands.slash(name, description);
    }

    public static final ArrayList<CommandData> MODERATION_COMMANDS = new ArrayList<>();

    static {
        MODERATION_COMMANDS.add(
                slashCommand("warn", "Warn a user")
                        .addOption(OptionType.USER, "member", "Member to warn", true)
                        .addOption(OptionType.STRING, "reason", "Warning reason")
        );

        MODERATION_COMMANDS.add(
                slashCommand("kick", "Kicks a user")
                        .addOption(OptionType.USER, "member", "Member to kick", true)
                        .addOption(OptionType.STRING, "reason", "Kick reason")
        );

        MODERATION_COMMANDS.add(
                slashCommand("delwarn", "Warn a user. Self-explanatory")
                        .addOption(OptionType.USER, "member", "Member to warn", true)
                        .addOption(OptionType.INTEGER, "warning", "Warning number (see /warnings)", true)
        );

        MODERATION_COMMANDS.add(
                slashCommand("warnings", "Get all warnings of a given member")
                        .addOption(OptionType.USER, "member", "Target member", true)
                        .addOptions(SlashCommands.SEND_PUBLICLY)
        );

        MODERATION_COMMANDS.add(
                slashCommand("modhistory", "Get moderation history of a given member")
                        .addOption(OptionType.USER, "member", "Target member", true)
                        .addOptions(SlashCommands.SEND_PUBLICLY)
        );
        MODERATION_COMMANDS.add(
                slashCommand("unepic", "!warn :scared_emoji:")
                        .addOption(OptionType.USER, "member", "Target member", true)
                        .addOption(OptionType.STRING, "reason", "Reason, will be blank if empty", false)
                        .addOption(OptionType.BOOLEAN, "final", "Is final?", false)
        );
    }
}
