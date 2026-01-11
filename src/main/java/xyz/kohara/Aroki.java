package xyz.kohara;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.reflections.Reflections;
import org.slf4j.LoggerFactory;
import xyz.kohara.features.commands.SlashCommands;
import xyz.kohara.features.support.ForumManager;
import xyz.kohara.status.BotActivity;
import xyz.kohara.web.WebServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.*;

public class Aroki {

    private static final String token = Config.get(Config.Option.TOKEN);
    private static final GatewayIntent[] intents = {GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MEMBERS};

    private static JDA BOT;
    private static final String BOT_NAME = Config.get(Config.Option.BOT_NAME);
    private static Guild BASEMENT;
    public static Role STAFF_ROLE, DEV_ROLE, UNEPIC_ROLE, FINAL_UNEPIC_ROLE;

    public static void main(String[] args) throws Exception {

        BOT = JDABuilder
                .createDefault(token)
                .enableIntents(Arrays.asList(intents))
                .build();
        BOT.awaitReady();

        BASEMENT = BOT.getGuildById(Config.get(Config.Option.SERVER_ID));

        STAFF_ROLE = BOT.getRoleById(Config.get(Config.Option.STAFF_ROLE_ID));
        DEV_ROLE = BOT.getRoleById(Config.get(Config.Option.DEV_ROLE_ID));
        UNEPIC_ROLE = BOT.getRoleById(Config.get(Config.Option.UNEPIC_ROLE_ID));
        FINAL_UNEPIC_ROLE = BOT.getRoleById(Config.get(Config.Option.FINAL_UNEPIC_ROLE_ID));

        // Add all listeners dynamically through a reflection
        getAllListeners().forEach(listenerAdapter -> {
            BOT.addEventListener(listenerAdapter);
            Aroki.Logger.info("Registered listener for " + listenerAdapter.toString().split("@")[0]);
        });

        Aroki.BASEMENT.updateCommands().addCommands(SlashCommands.COMMANDS).queue();
        ForumManager.scheduleReminderCheck();
        BotActivity.schedule();

        Aroki.Logger.success(BOT_NAME + " has successfully finished startup");

        WebServer.start();
    }

    private static List<ListenerAdapter> getAllListeners() {
        Aroki.Logger.info("Automatically adding listeners...");

        List<ListenerAdapter> listeners = new ArrayList<>();

        Reflections reflections = new Reflections("xyz.kohara.features");
        Set<Class<? extends ListenerAdapter>> classes = reflections.getSubTypesOf(ListenerAdapter.class);

        for (Class<? extends ListenerAdapter> clazz : classes) {
            try {
                listeners.add(clazz.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                Aroki.Logger.error("Failed to load listener: " + clazz.getName());
                throw new RuntimeException(e);
            }
        }

        return listeners;
    }

    public static Guild getServer() {
        return BASEMENT;
    }

    public static JDA getBot() {
        return BOT;
    }

    public static boolean isStaff(Member member) {
        return member.getRoles().contains(STAFF_ROLE);
    }

    public static boolean isDev(Member member) {
        return member.getRoles().contains(DEV_ROLE);
    }

    public static void sendDM(User member, String text) {
        member.openPrivateChannel().queue(privateChannel -> {
            privateChannel.sendMessage(text)
                    .setActionRow(
                            Button.of(
                                    ButtonStyle.PRIMARY,
                                    "sent_from", "Sent from " + Aroki.getServer().getName(), Emoji.fromFormatted("<:paper_plane:1358007565614710785>")
                            ).asDisabled()
                    )
                    .queue();
        });
    }

    public static void sendDM(Member member, String text) {
        sendDM(member.getUser(), text);
    }

    public static String ordinal(int i) {
        String[] suffixes = new String[]{"th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th"};
        return switch (i % 100) {
            case 11, 12, 13 -> i + "th";
            default -> i + suffixes[i % 10];
        };
    }

    public static String smallUnicode(String s) {
        Map<Character, Character> map = new HashMap<>();
        String[] mappings = {"aᴀ", "bʙ", "cᴄ", "dᴅ", "eᴇ", "fꜰ", "gɢ", "hʜ", "iɪ", "jᴊ", "kᴋ", "lʟ", "mᴍ", "nɴ", "oᴏ", "pᴘ", "rʀ", "sѕ", "tᴛ", "uᴜ", "wᴡ", "xх", "yʏ", "zᴢ"};
        for (String pair : mappings) {
            map.put(pair.charAt(0), pair.charAt(1));
        }
        StringBuilder result = new StringBuilder();
        for (char c : s.toCharArray()) {
            result.append(map.getOrDefault(c, c));
        }
        return result.toString();
    }

    public static class Logger {

        private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Aroki.Logger.class);

        private static final String GREEN = "\u001B[32m";
        private static final String RESET = "\u001B[0m";

        public static void info(String text) {
            LOGGER.info(text);
        }

        public static void success(String text) {
            LOGGER.info(GREEN + "{}" + RESET, text);
        }

        public static void error(String text) {
            LOGGER.error(text);
        }

        public static void warn(String text) {
            LOGGER.warn(text);
        }

    }

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
