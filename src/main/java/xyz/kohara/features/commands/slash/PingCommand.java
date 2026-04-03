package xyz.kohara.features.commands.slash;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import xyz.kohara.Aroki;

public class PingCommand extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		if (event.getName().equals("ping")) {
			String TEXT =
					"**Hello {MEMBER}!** I'm {BOT_NAME}! :wave:" +
					"\n* Current ping from your client to my server: {PING}ms" +
					"\n* Member count of **{GUILD}**: {MEMBER_COUNT}";
			event.reply(Aroki.Placeholders.parse(TEXT, event.getMember())).setEphemeral(true).queue();
		}
	}
}
