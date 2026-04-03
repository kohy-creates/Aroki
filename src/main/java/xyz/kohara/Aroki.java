package xyz.kohara;

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
import org.apache.commons.logging.Log;
import org.reflections.Reflections;
import org.slf4j.LoggerFactory;
import xyz.kohara.features.commands.SlashCommands;
import xyz.kohara.features.support.ForumManager;
import xyz.kohara.status.BotActivity;
import xyz.kohara.web.WebServer;

import javax.annotation.Nullable;
import java.net.*;
import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            Aroki.Logger.debug("Registered listener for {}", listenerAdapter.toString().split("@")[0]);
        });

        Aroki.BASEMENT.updateCommands().addCommands(SlashCommands.COMMANDS).queue();
        ForumManager.scheduleReminderCheck();
        BotActivity.schedule();

        Aroki.Logger.success(BOT_NAME + " has successfully finished startup");

        WebServer.start();
    }

    private static List<ListenerAdapter> getAllListeners() {
        Aroki.Logger.info("Adding listeners...");

        List<ListenerAdapter> listeners = new ArrayList<>();

        Reflections reflections = new Reflections("xyz.kohara.features");
        Set<Class<? extends ListenerAdapter>> classes = reflections.getSubTypesOf(ListenerAdapter.class);

        for (Class<? extends ListenerAdapter> clazz : classes) {
            try {
                listeners.add(clazz.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                Aroki.Logger.error("Failed to load listener: {}", clazz.getName());
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
                                    "sent_from", Placeholders.parse("Sent from {GUILD}"), Emoji.fromFormatted("<:paper_plane:1358007565614710785>")
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

        static {
            info("Debug level logging is currently " + (LOGGER.isDebugEnabled() ? "ENABLED" : "DISABLED"));
        }

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

        public static void error(String text, Object... args) {
            LOGGER.error(text, args);
        }

        public static void warn(String text) {
            LOGGER.warn(text);
        }

        public static void warn(String text, Object... args) {
            LOGGER.warn(text, args);
        }

        public static void debug(String text) {
            LOGGER.debug(text);
        }

        public static void debug(String text, Object... args) {
            LOGGER.debug(text, args);
        }
    }

    public enum Placeholders {
        MEMBER_COUNT(() -> String.valueOf(getServer().getMemberCount())),
        GUILD(() -> getServer().getName()),
        BOT_NAME(() -> getBot().getSelfUser().getAsMention()),
        MEMBER(() -> {
			if (getMember() == null) {
                Logger.error("Error parsing placeholder MEMBER, getMember() is null");
                return "ERROR";
            }
			return getMember().getAsMention();
		}, true),
        PING(() -> String.valueOf(getBot().getGatewayPing()));

        private final Supplier<String> replaceWith;
        private final boolean requiresMemberArgument;

        private static @Nullable Member targetMember = null;
        private static Member getMember() {
            return targetMember;
        }

        public Placeholders setMember(Member m) {
            targetMember = m;
            return this;
        }

        Placeholders(Supplier<String> replaceWith, boolean requiresMemberArgument) {
            this.replaceWith = replaceWith;
            this.requiresMemberArgument = requiresMemberArgument;
        }

        Placeholders(Supplier<String> replaceWith) {
            this.replaceWith = replaceWith;
            this.requiresMemberArgument = false;
        }

        private String getSelfReplacement() {
            return this.replaceWith.get();
        }

        private static final Pattern PATTERN = Pattern.compile("\\{([^{}]+)}");

        public static String parse(String text) {
            Matcher m = PATTERN.matcher(text);
            Logger.debug("Parsing placeholders...");
            if (!m.find()) {
                Logger.debug("No placeholders found!");
                return text;
            }

            StringBuilder result = new StringBuilder();

            while (m.find()) {
                String placeholder = m.group(1); // PING, MEMBER, etc.
                Logger.debug("Found placeholder: {}", placeholder);

                try {
                    var pl = Placeholders.valueOf(placeholder);
                    String replacement = pl.getSelfReplacement();

                    m.appendReplacement(result, Matcher.quoteReplacement(replacement));
                } catch (IllegalArgumentException e) {
                    Logger.error("Unknown placeholder: {}", placeholder);
                    m.appendReplacement(result, m.group(0)); // keep original
                }
            }

            m.appendTail(result);
            return result.toString();
        }

        public static String parse(String text, Member member) {
            targetMember = member;
            return parse(text);
        }
    }
}
