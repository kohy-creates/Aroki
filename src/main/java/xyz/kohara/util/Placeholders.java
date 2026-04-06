package xyz.kohara.util;

import net.dv8tion.jda.api.entities.Member;
import xyz.kohara.Aroki;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum Placeholders {
	MEMBER_COUNT((member) -> String.valueOf(Aroki.getServer().getMemberCount())),
	GUILD((member) -> Aroki.getServer().getName()),
	BOT_NAME((member) -> Aroki.getBot().getSelfUser().getAsMention()),
	MEMBER((member) -> {
		if (member == null) {
			Aroki.Logger.error("Error parsing placeholder MEMBER, member is null");
			return "ERROR";
		}
		return member.getAsMention();
	}),
	PING((member) -> String.valueOf(Aroki.getBot().getGatewayPing()));

	private final Function<Member, String> replaceWith;

	Placeholders(Function<Member, String> replaceWith) {
		this.replaceWith = replaceWith;
	}

	private Function<Member, String> getSelfReplacement() {
		return this.replaceWith;
	}

	private static final Pattern PATTERN = Pattern.compile("\\{([^{}]+)}");

	public static String parse(String text, Member member) {
		Matcher m = PATTERN.matcher(text);
		Aroki.Logger.debug("Parsing placeholders...");

		StringBuilder result = new StringBuilder();

		while (m.find()) {
			String placeholder = m.group(1); // PING, MEMBER, etc.
			Aroki.Logger.debug("Found placeholder: {}", placeholder);

			try {
				var pl = Placeholders.valueOf(placeholder);
				String replacement = pl.getSelfReplacement().apply(member);

				m.appendReplacement(result, Matcher.quoteReplacement(replacement));
			} catch (IllegalArgumentException e) {
				Aroki.Logger.error("Unknown placeholder: {}", placeholder);
				m.appendReplacement(result, m.group(0)); // keep original
			}
		}

		m.appendTail(result);
		return result.toString();
	}

	public static String parse(String text) {
		return parse(text, null);
	}
}
