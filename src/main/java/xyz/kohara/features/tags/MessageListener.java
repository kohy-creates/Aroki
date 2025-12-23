package xyz.kohara.features.tags;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import xyz.kohara.Aroki;
import xyz.kohara.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class MessageListener extends ListenerAdapter {

    private static final Map<Member, String> tagMessagesTemp = new HashMap<>();
    private static final Map<String, String> tagValuesTemp = new HashMap<>();

    private static final String PREFIX = Config.get(Config.Option.TAG_PREFIX);

    private static final double REQUIRED_SIMILARITY = 0.75;

    private static String TAGS_COMMAND;
    private static String REPLY_INVALID_TAG;
    static {
        Aroki.getServer().retrieveCommands().queue(
                commands -> {
                    for (Command command : commands) {
                        if (command.getName().equals("tags")) {
                            TAGS_COMMAND = "</tags:" + command.getId() + ">";
                             REPLY_INVALID_TAG = ":x: **Tag not found.**\n> You can do " + TAGS_COMMAND + " for a full list of tags";
                            break;
                        }
                    }
                }
        );
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        Message eventMessage = event.getMessage();
        String content = eventMessage.getContentRaw();
        if (content.indexOf(PREFIX) == 0) {
            String invokedTag = content.substring(PREFIX.length());
            MessageChannel channel = event.getChannel();
            String tag = Tags.TAGS.get(invokedTag);
            String reply;
            boolean wasSimilar = false;
            String bestMatch = null;
            if (tag != null) {
                reply = Tags.getTag(invokedTag);
            } else {
                double highestSimilarity = 0.0;

                for (String key : Tags.TAGS.keySet()) {
                    JaroWinklerSimilarity jaro = new JaroWinklerSimilarity();
                    double similarity = jaro.apply(key, invokedTag);
                    if (similarity > highestSimilarity) {
                        highestSimilarity = similarity;
                        bestMatch = key;
                    }
                }
                wasSimilar = (highestSimilarity > REQUIRED_SIMILARITY);
                reply = (wasSimilar) ? ":x: **Unknown tag!** Did you mean *`" + bestMatch + "`*? (" + (int) (highestSimilarity * 100) + "%)" : REPLY_INVALID_TAG;

            }
            Message message = channel.sendMessage(reply).complete();
            if (wasSimilar) {
                message.addReaction(Emoji.fromFormatted("✅")).queue();
                message.addReaction(Emoji.fromFormatted("❌")).queue();
                tagMessagesTemp.put(event.getMember(), message.getId());
                tagValuesTemp.put(message.getId(), Tags.getTag(bestMatch));

                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                executor.schedule(() -> {
                    String eventId = tagMessagesTemp.get(event.getMember());
                    if (eventId == null) return;

                    message.removeReaction(Emoji.fromFormatted("✅")).queue();
                    message.removeReaction(Emoji.fromFormatted("❌")).queue();

                    message.editMessage(REPLY_INVALID_TAG).queue();

                    tagValuesTemp.remove(message.getId());
                    tagMessagesTemp.remove(event.getMember());
                }, 10, TimeUnit.SECONDS);
            }
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        if (Objects.requireNonNull(event.getUser()).isBot()) return;
        String id = event.getMessageId();
        Member member = event.getMember();
        if (!tagMessagesTemp.containsValue(id)) return;
        if (!tagMessagesTemp.containsKey(member)) {
            event.getReaction().removeReaction(event.getUser()).queue();
            return;
        }
        String eventId = tagMessagesTemp.get(event.getMember());
        if (eventId == null) return;
        if (eventId.equals(id)) {
            event.getReaction().removeReaction(event.getUser()).queue();
            String newContet = (event.getReaction().getEmoji().equals(Emoji.fromFormatted("✅"))) ? tagValuesTemp.get(id) : REPLY_INVALID_TAG;

            event.getChannel().retrieveMessageById(id).queue(
                    message -> {
                        message.removeReaction(Emoji.fromFormatted("✅")).queue();
                        message.removeReaction(Emoji.fromFormatted("❌")).queue();
                        message.editMessage(newContet).queue();
                    }
            );

            tagValuesTemp.remove(id);
            tagMessagesTemp.remove(event.getMember());

        }

    }

}
