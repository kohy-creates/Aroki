package xyz.kohara.features.mclogs;

import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import xyz.kohara.Aroki;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class LogAnalyzerTools {

	public static final Pattern MCLOGS = Pattern.compile("https://mclo\\.gs/[A-Za-z0-9]+");

	private static final String TIPS_PATH = "data/log_analyzer/";
	private static final List<String> DISCONTINUED_VERSIONS = List.of(
			"1.20.4", "1.20.6",
			"1.21.4", "1.21.5", "1.21.7", "1.21.8", "1.21.11",
			"26.1", "26.1.1"
	);

	public static boolean isDiscontinued(String version) {
		for (String discontinuedPattern : DISCONTINUED_VERSIONS) {
			String regex = discontinuedPattern.replace("*", ".*");
			Pattern pattern = Pattern.compile(regex);
			if (pattern.matcher(version).find()) {
				return true;
			}
		}
		return false;
	}

	public static class Tips {
		public static final Map<String, String> LOG_TIPS = new HashMap<>();

		static {
			try {
				var folder = new File(TIPS_PATH);
				File[] files = folder.listFiles();

				if (files != null) {
					for (File file : files) {
						if (file.isFile()) {
							BufferedReader reader = new BufferedReader(new FileReader(file));
							var arr = reader.lines().toList();
							var l1 = arr.get(0);
							var l2 = arr.get(1);
							LOG_TIPS.put(l1, l2);
						}
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		public static List<String> getFor(List<String> text) {
			Aroki.Logger.debug("Analyzing logs for tips...");
			List<String> tips = new ArrayList<>();
			List<String> appearsAlready = new ArrayList<>();
			for (String line : text) {
				var tip = getTipFor(line);
				if (tip != null && !appearsAlready.contains(tip)) {
					Aroki.Logger.debug("Found tip: {}", tip);
					tips.add(LOG_TIPS.get(tip));
					appearsAlready.add(tip);
				}
			}
			return tips;
		}

		private static @Nullable String getTipFor(String line) {
			for (String s : LOG_TIPS.keySet()) {
				if (line.contains(s)) {
					return s;
				}
			}
			return null;
		}

		public static MessageCreateBuilder createTipMessage(List<String> tips, boolean useSingular) {
			MessageCreateBuilder builder = new MessageCreateBuilder();
			builder.setContent("");

			StringBuilder stringBuilder = new StringBuilder();

			stringBuilder.append(":warning: **I have briefly analyzed your log file").append((useSingular) ? "" : "s").append(", here's what I can advise:**\n");
			tips.forEach(tip -> stringBuilder.append("* ").append(tip).append("\n"));

			builder.setContent(String.valueOf(stringBuilder));

			return builder;
		}
	}

}
