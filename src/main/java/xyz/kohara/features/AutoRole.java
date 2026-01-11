package xyz.kohara.features;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import xyz.kohara.Aroki;
import xyz.kohara.Config;
import xyz.kohara.features.moderation.ModerationSaveData;

import java.util.ArrayList;
import java.util.List;

public class AutoRole extends ListenerAdapter {

    private static final Role MORTALS_ROLE = Aroki.getServer().getRoleById(Config.get(Config.Option.MORTALS_ROLE));
    private static final Role BOT_ROLE = Aroki.getServer().getRoleById(Config.get(Config.Option.BOT_ROLE));

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        List<Role> roles = new ArrayList<>();

        Guild guild = event.getGuild();
        roles.add(MORTALS_ROLE);
        Member member = event.getMember();
        if (event.getUser().isBot()) {
            roles.add(BOT_ROLE);
        }

        if (ModerationSaveData.isMemberUnepic(member, true)) {
            roles.add(Aroki.FINAL_UNEPIC_ROLE);
            roles.add(Aroki.UNEPIC_ROLE);
        } else if (ModerationSaveData.isMemberUnepic(member)) {
            roles.add(Aroki.UNEPIC_ROLE);
        }

        // Failsafe
        roles.remove(null);

        guild.modifyMemberRoles(member, roles).queue();
    }

}
