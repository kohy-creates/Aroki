package xyz.kohara.features;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.text.WordUtils;
import xyz.kohara.Aroki;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Quote extends ListenerAdapter {

    private final int MAX_LENGTH = 300;
    private final boolean SHOULD_TRUNCATE = true;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String content = event.getMessage().getContentRaw().toLowerCase();

        Pattern pattern = Pattern.compile("https://(?:([a-zA-Z0-9-]+)\\.)?discord\\.com/channels/(\\d+)/(\\d+)/(\\d+)");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            //                Server ID         Channel ID        Message ID
            String[] id = {matcher.group(2), matcher.group(3), matcher.group(4)};

            System.out.println(Arrays.toString(id));

            GuildMessageChannel channel = Aroki.getServer().getChannelById(GuildMessageChannel.class, id[1]);
            if (channel != null) {
                channel.retrieveMessageById(id[2]).queue(
                        message -> {
                            User msgAuthor = message.getAuthor();
                            User author = event.getAuthor();
                            EmbedBuilder embedBuilder = new EmbedBuilder()
                                    .setColor(Color.darkGray)
                                    .setDescription((SHOULD_TRUNCATE) ?
                                            truncateMessage(message.getContentDisplay(), MAX_LENGTH)
                                            : message.getContentDisplay()
                                    )
                                    //.setTimestamp(Instant.now())
                                    .setAuthor("➤ " + msgAuthor.getName(), message.getJumpUrl(), msgAuthor.getAvatarUrl())
                                    .setFooter(author.getName() + " quoted", author.getAvatarUrl());

                            List<Message.Attachment> att = message.getAttachments();
                            if (!att.isEmpty()) {
                                for (Message.Attachment file : att) {
                                    if (file.isImage()) {
                                        embedBuilder.setImage(file.getUrl());
                                        break;
                                    }
                                }
                            }
                            event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                        }
                );
            }
        }
    }

    public static String truncateMessage(String text, int maxLength) {
        if (text.length() <= maxLength) {
            System.out.println("Words removed: 0");
            return text;
        }

        String truncated = WordUtils.abbreviate(text, (int) (maxLength * 0.8), maxLength, "...");
        int originalWords = text.trim().split("\\s+").length;
        int keptWords = truncated.replace("...", "").trim().split("\\s+").length;
        int removed = originalWords - keptWords;

        return truncated + " *(" + removed + " more words)*";
    }
}
