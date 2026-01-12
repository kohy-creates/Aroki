package xyz.kohara.features.moderation.commands;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.jetbrains.annotations.NotNull;
import xyz.kohara.Aroki;
import xyz.kohara.features.moderation.ModerationSaveData;
import xyz.kohara.features.moderation.ModerationUtils;

import java.util.ArrayList;
import java.util.List;

public class UnepicCommand extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Member responsible = event.getMember();
        if (ModerationUtils.shouldStop(responsible, event, "unepic")) return;

        Member member = event.getOption("member", null, OptionMapping::getAsMember);
        if (member.getUser().isBot()) {
            event.reply(":x: **While not all bots are epic, they *cannot* be unepic**").setEphemeral(true).queue();
            return;
        }
        boolean isFinal = event.getOption("final", false, OptionMapping::getAsBoolean);
        String reason = event.getOption("reason", null, OptionMapping::getAsString);

        ModerationSaveData.saveModerationAction(member, ModerationSaveData.ActionType.UNEPIC, (reason == null) ? "No reason provided" : reason, responsible);

        WarnCommand.warnUser(member, responsible,
                (isFinal) ? "final unepic" : "unepic" +
                        ((reason != null) ? " - *" + reason + "*" : "")
        );
        Guild guild = event.getGuild();

        List<Role> roles = new ArrayList<>(member.getRoles());
        roles.add(Aroki.UNEPIC_ROLE);
        if (isFinal) {
            roles.add(Aroki.FINAL_UNEPIC_ROLE);
        }
        guild.modifyMemberRoles(member, roles).queue();

        ModerationSaveData.addUnepicMember(member, isFinal);

        event.reply("https://tenor.com/view/discord-meme-spooked-scared-mod-gif-18361254").queue();
    }
}
