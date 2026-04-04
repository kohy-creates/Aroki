package xyz.kohara.features.mclogs;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.jetbrains.annotations.NotNull;
import xyz.kohara.Aroki;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;

public class LogAnalyzer extends ListenerAdapter {

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		var content = event.getMessage().getContentRaw();
		Matcher m = LogAnalyzerTools.MCLOGS.matcher(content);

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
						var msgBuilder = LogAnalyzerTools.Tips.createTipMessage(tips, false);
						event.getMessage().reply(msgBuilder.build()).queue();
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
