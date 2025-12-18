package xyz.kohara.features.commands.slash;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import xyz.kohara.Aroki;
import xyz.kohara.features.linking.LinkManager;

import java.awt.*;
import java.io.IOException;
import java.net.URISyntaxException;

public class LinkCommands extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("link")) {
            Member member = event.getMember();

            int code = event.getOption("code", 0, OptionMapping::getAsInt);
            if (LinkManager.isActiveCode(code)) {
                if (LinkManager.linkMember(member, code)) {
                    try {
                        event.reply(":white_check_mark: **Linked your account to Minecraft account `" + Aroki.getPlayerFromUUID(LinkManager.getLinked(member)) + "`**").setEphemeral(true).queue();
                    } catch (IOException | URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    event.reply(":x: **An internal error occured trying to run that command**").setEphemeral(true).queue();
                }
            } else {
                event.reply(":x: **Invalid code**").setEphemeral(true).queue();
            }
        } else if (event.getName().equals("unlink")) {
            Member member = event.getMember();

            if (LinkManager.isLinked(member)) {
                if (LinkManager.unlinkMember(member)) {
                    event.reply(":white_check_mark: **Unlinked!**").setEphemeral(true).queue();
                } else {
                    event.reply(":x: **An internal error occured trying to run that command**").setEphemeral(true).queue();
                }
            } else {
                event.reply(":x: **Your Discord and MC accounts aren't linked**").setEphemeral(true).queue();
            }
        } else if (event.getName().equals("getlinked")) {
            Member member = event.getOption("member", event.getMember(), OptionMapping::getAsMember);
            if (member.getUser().isBot()) return;

            String uuid = LinkManager.getLinked(member);
            if (uuid == null) {
                event.reply(":x: **" + member.getAsMention() + " has no linked Minecraft account**").setEphemeral(true).queue();
            }

            String player;
            try {
                player = Aroki.getPlayerFromUUID(uuid);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }

            EmbedBuilder builder = new EmbedBuilder()
                    .setDescription(member.getAsMention() + " is linked to account `" + player + "`\n" +
                            "[Click to view on __NameMC__](https://namemc.com/profile/" + player + ")")
                    .setThumbnail("https://mineskin.eu/helm/" + player + "/512.png")
                    .setColor(new Color(255, 92, 61));

            event.reply("").addEmbeds(builder.build()).setEphemeral(true).queue();
        }
    }
}
