package xyz.kohara.features.support;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ForumData {

    private static final File FORUM_DATA = new File("data/forum_data.json");
    private static final Gson GSON = new Gson();

    static {
        if (!FORUM_DATA.exists()) {
            try {
                if (FORUM_DATA.createNewFile())
                    try (FileWriter writer = new FileWriter(FORUM_DATA)) {
                        writer.write("{}");
                    }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static JsonObject loadJson() throws IOException {
        try (Reader reader = new FileReader(FORUM_DATA)) {
            return GSON.fromJson(reader, JsonObject.class);
        }
    }

    private static void saveJson(JsonObject jsonObject) throws IOException {
        try (Writer writer = new FileWriter(FORUM_DATA)) {
            GSON.toJson(jsonObject, writer);
        }
    }

    public enum Entry {
        OP("op"),
        REMIND_AMOUNT("remind_amount"),
        CREATION_DATE("creation_date"),
        LAST_REMINDED("last_reminded");

        private final String id;

        Entry(String id) {
            this.id = id;
        }
    }

    public static void addEntry(String key, String op, long creationDate) throws IOException {
        JsonObject json = loadJson();

        JsonObject entry = new JsonObject();
        entry.addProperty(Entry.OP.id, op);
        entry.addProperty(Entry.REMIND_AMOUNT.id, 0);
        entry.addProperty(Entry.CREATION_DATE.id, creationDate);
        entry.addProperty(Entry.LAST_REMINDED.id, creationDate);

        json.add(key, entry);
        saveJson(json);
    }

    public static void removeEntry(String key) throws IOException {
        JsonObject json = loadJson();
        if (json.has(key)) {
            json.remove(key);
            saveJson(json);
        }
    }

    public static boolean entryExists(String key) throws IOException {
        JsonObject json = loadJson();
        return json.has(key);
    }

    public static String getEntryValue(String key, Entry field) throws IOException {
        JsonObject json = loadJson();
        JsonObject entry = json.getAsJsonObject(key);
        String value = "0";
        if (entry.has(field.id)) {
            value = entry.get(field.id).getAsString();
        }
        else {
            entry.addProperty(field.id, value);
        }
        return value;
    }

    public static void setEntryValue(String key, Entry field, long value) throws IOException {
        JsonObject json = loadJson();
        json.getAsJsonObject(key).addProperty(field.id, value);
        saveJson(json);
    }

    public static List<String> getAllThreads() throws IOException {
        JsonObject json = loadJson();
        List<String> entryIds = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            entryIds.add(entry.getKey());
        }
        return entryIds;

    }

    public static List<String> findThreads(User op) throws IOException {
        String id = op.getId();
        List<String> openThreads = new ArrayList<>();

        JsonObject json = loadJson();

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonObject data = (JsonObject) entry.getValue();

            if (data.has("op")) {
                if (data.get("op").getAsString().equals(id)) openThreads.add(key);
            }
        }
        return openThreads;
    }
}
