package xyz.kohara.features.mclogs;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.jetbrains.annotations.NotNull;
import xyz.kohara.Aroki;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

public class LogAnalyzer extends ListenerAdapter {

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		var content = event.getMessage().getContentRaw();
		Matcher m = LogAnalyzerTools.MCLOGS.matcher(content);
		// Continue if it finds any link
		if (m.find()) {
			ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
			executor.schedule(() -> {
				List<String> allTips = new ArrayList<>();

				// Assemble tips
				// There is probably a way more efficient way to sum everything up, but whatever :3
				while (m.find()) {
					var url = m.group();
					Aroki.Logger.debug("Found log URL: {}", url);

					try (CloseableHttpClient client = HttpClients.createDefault()) {
						var id =  url.split(".gs/")[1];
						Aroki.Logger.debug("Getting info for log with id {}", id);
						var get = new HttpGet("https://api.mclo.gs/1/raw/" + id);

						try (CloseableHttpResponse response = client.execute(get)) {
							var body = new String(response.getEntity().getContent().readAllBytes());
							List<String> lines = List.of(body.split("\n"));
							var tips = LogAnalyzerTools.Tips.getFor(lines);
							if (!tips.isEmpty()) {
								allTips.addAll(tips);
							}
						}
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}

				Set<String> set = new HashSet<>(allTips);
				allTips.clear();
				allTips.addAll(set);

				var msgBuilder = LogAnalyzerTools.Tips.createTipMessage(allTips, false);
				event.getMessage().reply(msgBuilder.build()).queue();
			}, 1500, TimeUnit.MILLISECONDS);
		}
	}
}
