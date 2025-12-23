package xyz.kohara.features.support;

import xyz.kohara.Config;

public enum ForumTags {
    OPEN("open_tag_id"),
    RESOLVED("resolved_tag_id"),
    INVALID("invalid_tag_id"),
    TO_DO("to_do_tag_id"),
    DUPLICATE("duplicate_tag_id");

    private final long id;
    ForumTags(String id) {
        this.id = parseConfig(id);
    }

    public long getId() {
        return this.id;
    }

    private static Long parseConfig(String key) {
        String value = Config.get(Config.Option.valueOf(key.toUpperCase()));
        if (value == null) {
            throw new RuntimeException("Config key '" + key + "' is missing or null!");
        }
        return Long.parseLong(value);
    }
}