package xyz.kohara;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class Config {

    private static final Map<String, String> CONFIG;
    private static final String configPath = "data/config.json";

    public enum Option {
        TOKEN("CHANGE_ME"),
        SERVER_ID(0),
        STAFF_ROLE_ID(0),
        DEV_ROLE_ID(0),
        BOT_NAME("Aroki"),
        SUPPORT_CHANNEL(0),
        INVALID_TAG_ID(0),
        OPEN_TAG_ID(0),
        RESOLVED_TAG_ID(0),
        TO_DO_TAG_ID(0),
        DUPLICATE_TAG_ID(0),
        TAG_PREFIX("!"),
        MORTALS_ROLE(0),
        BOT_ROLE(0),
        INVITE("https://example.com/"),
        ADJ_INFO_CHANNEL(0),
        UNEPIC_ROLE_ID(1205080790006759424L),
        FINAL_UNEPIC_ROLE_ID(1383909149301014648L),
        STELLARITY_SUPPORT_TAG_ID(1185567778191712257L);

        private final String defaultValue;

        Option(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        Option(long defaultValue) {
            this.defaultValue = String.valueOf(defaultValue);
        }

        public String getDefaultValue() {
            return this.defaultValue;
        }

        public String getConfigEntryName() {
            return this.name().toLowerCase();
        }

    }

    static {
        tryCreateConfigFile();

        // Load existing config (or empty if new file)
        Map<String, String> loaded;
        try (FileReader reader = new FileReader(configPath)) {
            Type type = new TypeToken<HashMap<String, String>>() {
            }.getType();
            loaded = new Gson().fromJson(reader, type);
            if (loaded == null) loaded = new HashMap<>();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Fill missing entries from configEntries list
        boolean updated = false;
        for (Option option : Option.values()) {
            String name = option.getConfigEntryName();
            if (!loaded.containsKey(name)) {
                loaded.put(name, option.getDefaultValue());
                updated = true;
            }
        }

        // Save back if new entries were added
        if (updated) {
            try (FileWriter writer = new FileWriter(configPath)) {
                new GsonBuilder()
                        .setPrettyPrinting().create()
                        .toJson(loaded, writer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        CONFIG = loaded;
    }


    private static void tryCreateConfigFile() {
        File file = new File(configPath);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String get(Option option) {
        return CONFIG.get(option.getConfigEntryName());
    }
}
