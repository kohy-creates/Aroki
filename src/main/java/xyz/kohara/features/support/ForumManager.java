package xyz.kohara.features.support;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageType;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.entities.channel.forums.ForumTagSnowflake;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.managers.channel.concrete.ThreadChannelManager;
import xyz.kohara.Config;
import xyz.kohara.Aroki;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

public class ForumManager extends ListenerAdapter {

    private static final ForumChannel SUPPORT_CHANNEL = Aroki.getServer().getForumChannelById(Config.get(Config.Option.SUPPORT_CHANNEL));
    private static final int INTERVAL_MINUTES = 15;
    private static final int INTERVAL = INTERVAL_MINUTES * 60 * 1000;

    private static final Map<String, String> replyCache = new HashMap<>();

    static {
        File dir = new File("data/forum/");
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            try {
                for (File child : directoryListing) {
                    String name = child.getName().split("\\.")[0];
                    replyCache.put(name, Files.readString(child.toPath()).trim());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String CLOSE_COMMAND;

    static {
        Aroki.getServer().retrieveCommands().queue(
                commands -> {
                    for (Command command : commands) {
                        if (command.getName().equals("close")) {
                            CLOSE_COMMAND = command.getAsMention();
                            break;
                        }
                    }
                }
        );
    }

    private static boolean hasThreadManagementPerms(Member member) {
        return (Aroki.isDev(member) || Aroki.isStaff(member));
    }

    private static String REMINDER_MESSAGE(String member, boolean addReminder) {
        String text = replyCache.get("reminder_message");
        text = text.replace("{MEMBER}", "<@" + member + ">");
        text = text.replace("{CLOSE}", CLOSE_COMMAND);

        int splitIndex = text.lastIndexOf(">");
        String[] parts = new String[]{text.substring(0, splitIndex - 1), text.substring(splitIndex - 1)};
        return (addReminder) ? parts[0] + "\n" + parts[1] : parts[0];
    }

    private static String DUPLICATE_MESSAGE(boolean DM) {
        return replyCache.get((DM) ? "duplicate_dm" : "duplicate");
    }

    private static String INVALID_MESSAGE(boolean DM) {
        return replyCache.get((DM) ? "invalid_dm" : "invalid");
    }

    private static EmbedBuilder SUPPORT_EMBED(Member member) {
        EmbedBuilder embed = new EmbedBuilder();
        String iconUrl;
        iconUrl = (Aroki.getServer().getIcon() != null) ? Aroki.getServer().getIcon().getUrl() : null;
        embed.setAuthor(Aroki.getServer().getName(), null, iconUrl);

        String content = replyCache.get("embed");
        content = content.replace("{CLOSE}", CLOSE_COMMAND);
        content = content.replace("{MEMBER}", member.getAsMention());

        int firstSectionIndex = content.indexOf("###");
        String description = content.substring(0, firstSectionIndex).trim();
        embed.setDescription(description);

        Pattern sectionPattern = Pattern.compile("(?=^### )", Pattern.MULTILINE);
        String[] sections = sectionPattern.split(content.substring(firstSectionIndex));
        for (String section : sections) {
            String[] lines = section.split("\n", 2);
            String title = lines[0].replace("### ", "").trim();
            String body = lines.length > 1 ? lines[1].trim() : "";
            embed.addField(title, body, false);
        }
        return embed;
    }

    private enum ResolutionType {
        DUPLICATE,
        INVALID,
        RESOLVED;

        ResolutionType() {
        }
    }

    private static void closePost(ThreadChannel thread, ResolutionType reason) throws IOException {
        List<Long> currentTagIds = new ArrayList<>(thread.getAppliedTags()
                .stream()
                .map(ForumTag::getIdLong)
                .toList());
        switch (reason) {
            case ResolutionType.DUPLICATE -> currentTagIds.add(ForumTags.DUPLICATE.getId());
            case ResolutionType.INVALID -> currentTagIds.add(ForumTags.INVALID.getId());
            case ResolutionType.RESOLVED -> currentTagIds.add(ForumTags.RESOLVED.getId());
        }
        currentTagIds.remove(ForumTags.OPEN.getId());
        currentTagIds.remove(ForumTags.TO_DO.getId());

        ArrayList<ForumTagSnowflake> snowflakes = new ArrayList<>();
        for (Long id : currentTagIds) {
            snowflakes.add(ForumTagSnowflake.fromId(id));
        }

        thread.getManager()
                .setAppliedTags(snowflakes)
                .setLocked(true)
                .setArchived(true)
                .queue();

        ForumData.removeEntry(thread.getId());
    }

    private boolean isSupportThread(ThreadChannel thread) {
        List<ThreadChannel> supportThreads = SUPPORT_CHANNEL.getThreadChannels();
        return supportThreads.contains(thread);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getChannelType() != ChannelType.GUILD_PUBLIC_THREAD) return;
        ThreadChannel channel = event.getChannel().asThreadChannel();
        if (event.getAuthor().isBot() || !isSupportThread(channel)) return;
        try {
            if (channel.isLocked()) {
                if (channel.isPinned()) return;
                Thread.sleep(500);
                channel.getManager().setArchived(true).queue();
            } else {
                String id = channel.getId();
                String content = event.getMessage().getContentRaw();
                if (content.equals("✅") || content.equals("🎗️")) {
                    String op = ForumData.getEntryValue(id, ForumData.Entry.OP);
                    switch (content) {
                        case "✅" -> {
                            if (op.equals(Objects.requireNonNull(event.getMember()).getId()) || hasThreadManagementPerms(event.getMember())) {
                                event.getMessage().addReaction(Emoji.fromFormatted("✅")).queue();
                                event.getChannel().sendMessage("✅").queue();
                                closePost(channel, ResolutionType.RESOLVED);
                            }
                        }
                        case "🎗️" -> {
                            long currentTime = System.currentTimeMillis();
                            long lastReminded = Long.parseLong(ForumData.getEntryValue(id, ForumData.Entry.LAST_REMINDED));
                            if (currentTime - lastReminded >= 30 * 1000) {
                                event.getMessage().addReaction(Emoji.fromFormatted("🎗️")).queue();
                                ForumData.setEntryValue(id, ForumData.Entry.LAST_REMINDED, currentTime);
                                channel.sendMessage(REMINDER_MESSAGE(op, false)).queue();
                            }
                        }
                    }
                } else if (!ForumData.entryExists(id)) {
                    ForumData.addEntry(id, Objects.requireNonNull(event.getMember()).getId(), System.currentTimeMillis());
                    channel.sendMessage("").addEmbeds(SUPPORT_EMBED(event.getMember()).build()).queue(
                            message -> message.pin().queue(
                                    success -> channel.getHistory().retrievePast(1).queue(messages -> {
                                        Message lastMessage = messages.getFirst();
                                        if (lastMessage.getType() == MessageType.CHANNEL_PINNED_ADD) {
                                            lastMessage.delete().queue();
                                        }
                                    })
                            )
                    );
                    List<Long> currentTagIds = new ArrayList<>(channel.getAppliedTags()
                            .stream()
                            .map(ForumTag::getIdLong)
                            .toList());
                    currentTagIds.add(ForumTags.OPEN.getId());
                    ArrayList<ForumTagSnowflake> snowflakes = new ArrayList<>();
                    for (Long tagid : currentTagIds) {
                        snowflakes.add(ForumTagSnowflake.fromId(tagid));
                    }

                    ThreadChannelManager manager = channel.getManager();
                    manager.setAppliedTags(snowflakes).queue();
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onChannelDelete(ChannelDeleteEvent event) {
        String channelId = event.getChannel().asThreadChannel().getId();
        try {
            if (ForumData.entryExists(channelId)) {
                ForumData.removeEntry(channelId);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        if (event.getUser().isBot()) return;
        try {
            List<String> threadList = ForumData.findThreads(event.getUser());
            for (String id : threadList) {
                ThreadChannel thread = Aroki.getServer().getChannelById(ThreadChannel.class, id);
                if (thread != null) closePost(thread, ResolutionType.INVALID);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("close")) {
            if (event.getChannel().getType() != ChannelType.GUILD_PUBLIC_THREAD && !isSupportThread(event.getChannel().asThreadChannel())) {
                event.reply(":x: **This command can only be used in <#" + Config.get(Config.Option.SUPPORT_CHANNEL) + "> threads!**").setEphemeral(true).queue();
                return;
            }
            try {
                ThreadChannel thread = event.getChannel().asThreadChannel();
                String id = thread.getId();
                OptionMapping option = event.getOption("action");
                String action = option != null ? option.getAsString() : "resolve";
                String op = ForumData.getEntryValue(id, ForumData.Entry.OP);
                boolean isOP = op.equals(Objects.requireNonNull(event.getMember()).getId());
                boolean staff = hasThreadManagementPerms(event.getMember());
                if ((action.equals("resolve") && isOP) || ((action.equals("resolve") && staff))) {
                    event.reply(":white_check_mark:").queue();
                    closePost(thread, ResolutionType.RESOLVED);
                } else if (action.equals("invalid") && staff) {
                    OptionMapping note = event.getOption("note");
                    Aroki.getBot().retrieveUserById(op).queue(
                            user -> Aroki.sendDM(user, INVALID_MESSAGE(true)
                                    .replace("{THREAD}", thread.getAsMention())
                                    .replace(
                                            "{NOTE}",
                                            (note != null) ?
                                                    "*" + note.getAsString() + " ~" + event.getMember().getEffectiveName() + "*"
                                                    : "*No notes provided*")
                            )
                    );
                    event.reply(
                            INVALID_MESSAGE(false)
                                    .replace(
                                            "{NOTE}",
                                            (note != null) ?
                                                    "*" + note.getAsString() + "*"
                                                    : "*No notes provided*")
                    ).queue();
                    closePost(thread, ResolutionType.INVALID);
                } else if (action.equals("duplicate") && staff) {
                    OptionMapping od = event.getOption("duplicate_of");
                    if (od == null) {
                        event.reply(":x: **You need to set what thread this duplicates!**").setEphemeral(true).queue();
                        return;
                    }
                    OptionMapping note = event.getOption("note");
                    GuildChannelUnion ad = od.getAsChannel();
                    Aroki.getBot().retrieveUserById(op).queue(
                            user -> Aroki.sendDM(user, DUPLICATE_MESSAGE(true)
                                    .replace("{THREAD}", thread.getAsMention())
                                    .replace("{DUPLICATE}", ad.getAsMention())
                                    .replace(
                                            "{NOTE}",
                                            (note != null) ?
                                                    "*" + note.getAsString() + " ~" + event.getMember().getEffectiveName() + "*"
                                                    : "*No notes provided*")
                            )
                    );
                    String text = DUPLICATE_MESSAGE(false)
                            .replace("{DUPLICATE}", ad.getAsMention())
                            .replace(
                                    "{NOTE}",
                                    (note != null) ?
                                            "*" + note.getAsString() + "*"
                                            : "*No notes provided*");
                    event.reply(text).queue();
                    closePost(thread, ResolutionType.DUPLICATE);
                } else {
                    event.reply(":x: **This is not your support thread or you don't have permission to do that**").setEphemeral(true).queue();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void scheduleReminderCheck() {
        Aroki.Logger.info("Starting automatic reminder timed task...");
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            static {
                Aroki.Logger.info("Initialized forum auto reminder timed task, scheduled to run every " + INTERVAL_MINUTES + " minutes");
            }

            @Override
            public void run() {
                List<String> threads;
                try {
                    threads = ForumData.getAllThreads();
                    for (String entry : threads) {
                        long lastReminded;
                        ThreadChannel thread = Aroki.getServer().getThreadChannelById(entry);
                        if (thread == null) {
                            ForumData.removeEntry(entry);
                            continue;
                        }
                        List<Long> tags = new ArrayList<>(thread.getAppliedTags()
                                .stream()
                                .map(ForumTag::getIdLong)
                                .toList());
                        if (tags.contains(ForumTags.TO_DO.getId()) || thread.isPinned()) continue;
                        lastReminded = Long.parseLong(ForumData.getEntryValue(entry, ForumData.Entry.LAST_REMINDED));
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastReminded >= 72 * 60 * 60 * 1000) {
                            String op = ForumData.getEntryValue(entry, ForumData.Entry.OP);

                            int autoRemindTime = Integer.parseInt(ForumData.getEntryValue(entry, ForumData.Entry.REMIND_AMOUNT));
                            autoRemindTime++;
                            if (autoRemindTime > 3) {
                                Aroki.getBot().retrieveUserById(op).queue(
                                        user -> Aroki.sendDM(user, INVALID_MESSAGE(true)
                                                .replace("{THREAD}", thread.getAsMention())
                                                .replace(
                                                        "{NOTE}",
                                                        "*Closed automatically due to inactivity  ~Aroki*"
                                                )
                                        )
                                );
                                thread.sendMessage(
                                        INVALID_MESSAGE(false)
                                                .replace(
                                                        "{NOTE}",
                                                        "*Closed automatically due to inactivity*"
                                                )
                                ).queue();
                                closePost(thread, ResolutionType.INVALID);
                                return;
                            }
                            ForumData.setEntryValue(entry, ForumData.Entry.REMIND_AMOUNT, autoRemindTime);
                            ForumData.setEntryValue(entry, ForumData.Entry.LAST_REMINDED, currentTime);
                            thread.sendMessage(REMINDER_MESSAGE(op, true)).queue();
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        }, 5000, INTERVAL);
    }
}
