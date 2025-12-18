package xyz.kohara.features.moderation.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;
import xyz.kohara.Aroki;
import xyz.kohara.Config;
import xyz.kohara.features.moderation.ModerationSaveData;
import xyz.kohara.features.moderation.ModerationUtils;

import java.awt.*;

public class KickCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        Member responsible = event.getMember();
        if (ModerationUtils.shouldStop(responsible, event, "kick")) return;

        Member member = event.getOption("member", null, OptionMapping::getAsMember);
        String reason = event.getOption("reason", "No reason provided", OptionMapping::getAsString);

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(Color.GREEN)
                .setDescription(
                        "***<@" + member.getId() + "> was kicked.*** | " + reason + "\n"
                );

        event.reply("").addEmbeds(builder.build()).queue();

        ModerationSaveData.saveModerationAction(member, ModerationSaveData.ActionType.KICK, reason, responsible);

        StringBuilder text = new StringBuilder().append(
                "**You were kicked from " + Aroki.getServer().getName() + "**\n> Reason: *" + reason + "* ~*" + responsible.getEffectiveName() + "*"
        );
        text.append("\nYou can still [join back](" + Config.get(Config.Option.INVITE) + "), just *don't* do whatever got you kicked again");

        member.getUser().openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessage(text)
                    .setActionRow(
                            Button.of(
                                    ButtonStyle.PRIMARY,
                                    "sent_from", "Sent from " + Aroki.getServer().getName(), Emoji.fromFormatted("<:paper_plane:1358007565614710785>")
                            ).asDisabled()
                    )
                    .queue(message -> member.kick().reason(reason).queue());
        });


    }
}
