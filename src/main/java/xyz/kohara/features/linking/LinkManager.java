package xyz.kohara.features.linking;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Member;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class LinkManager {

    private static final Gson GSON = new Gson();
    private static final Map<UUID, Integer> uuidCodeMap = new HashMap<>();
    private static final File savePath = new File("data/linked_accounts.json");

    static {
        if (!savePath.exists()) {
            try {
                if (savePath.createNewFile())
                    try (FileWriter writer = new FileWriter(savePath)) {
                        writer.write("{}");
                    }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static int tryGenerateLinkCode(String uuid) {
        UUID uuid1 = UUID.fromString(uuid);
        if (isLinked(uuid1)) return 0;
        int code;
        do {
            code = new Random().nextInt(9000) + 1000;
        } while (uuidCodeMap.containsValue(code));
        uuidCodeMap.put(uuid1, code);
        return code;
    }

    // Will I ever actually need that?
    public static Map<UUID, Integer> getUuidCodeMap() {
        return uuidCodeMap;
    }

    public static boolean isActiveCode(int i) {
        return uuidCodeMap.containsValue(i);
    }

    private static UUID getFromCode(int code) {
        for (Map.Entry<UUID, Integer> entry : uuidCodeMap.entrySet()) {
            if (entry.getValue() == code) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static boolean isLinked(UUID uuid) {
        String uuidStr = uuid.toString();

        try (Reader reader = new FileReader(savePath)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (entry.getValue().getAsString().equals(uuidStr)) {
                    return true;
                }
            }

            return false;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isLinked(Member member) {
        return (getLinked(member) != null);
    }

    public static String getLinked(Member member) {
        try (Reader reader = new FileReader(savePath)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            return json.get(member.getId()).getAsString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean linkMember(Member member, int code) {
        UUID uuid = getFromCode(code);
        if (uuid == null) {
            throw new IllegalArgumentException("Invalid code: " + code);
        }

        try (Reader reader = new FileReader(savePath)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            json.addProperty(member.getId(), uuid.toString());
            try (Writer writer = new FileWriter(savePath)) {
                GSON.toJson(json, writer);
            }
            uuidCodeMap.remove(uuid);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean unlinkMember(Member member) {
        try (Reader reader = new FileReader(savePath)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            json.remove(member.getId());
            try (Writer writer = new FileWriter(savePath)) {
                GSON.toJson(json, writer);
            }
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
