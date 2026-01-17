package xyz.kohara.features.support;

import xyz.kohara.Config;

public enum ForumTags {
    OPEN(Config.Option.OPEN_TAG_ID),
    RESOLVED(Config.Option.RESOLVED_TAG_ID),
    INVALID(Config.Option.INVALID_TAG_ID),
    TO_DO(Config.Option.TO_DO_TAG_ID),
    DUPLICATE(Config.Option.DUPLICATE_TAG_ID),
    STELLARITY(Config.Option.STELLARITY_SUPPORT_TAG_ID);

    private final Config.Option option;

    ForumTags(Config.Option option) {
        this.option = option;
    }

    public long getId() {
        return Long.parseLong(Config.get(this.option));
    }
}