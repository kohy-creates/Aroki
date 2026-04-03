package xyz.kohara.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

public class MinecraftUtils {

	public static String getPlayerFromUUID(String uuid) throws IOException, URISyntaxException {
		return getPlayerFromUUID(UUID.fromString(uuid));
	}

	public static String getPlayerFromUUID(UUID uuid) throws IOException, URISyntaxException {
		String apiUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "");
		URL url = new URI(apiUrl).toURL();
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		con.setConnectTimeout(5000);
		con.setReadTimeout(5000);

		int status = con.getResponseCode();

		if (status != 200) {
			return null;
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		StringBuilder response = new StringBuilder();
		String line;

		while ((line = in.readLine()) != null) {
			response.append(line);
		}

		in.close();
		con.disconnect();

		JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
		return json.get("name").getAsString();
	}

}
