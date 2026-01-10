package xyz.kohara.features.music;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import xyz.kohara.Aroki;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

public class MusicPlayer extends ListenerAdapter {

    private static final AudioPlayerManager PLAYER_MANAGER = new DefaultAudioPlayerManager();

    private static final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private static final Map<Guild, ScheduledFuture<?>> leaveTasks = new ConcurrentHashMap<>();

    static {
        PLAYER_MANAGER.registerSourceManager(new YoutubeAudioSourceManager());
        PLAYER_MANAGER.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
        PLAYER_MANAGER.registerSourceManager(new HttpAudioSourceManager());

        AudioSourceManagers.registerRemoteSources(PLAYER_MANAGER);
        AudioSourceManagers.registerLocalSource(PLAYER_MANAGER);

        PLAYER_MANAGER.getConfiguration().setResamplingQuality(AudioConfiguration.ResamplingQuality.HIGH);
        PLAYER_MANAGER.getConfiguration().setOpusEncodingQuality(10);

    }

    private static final AudioPlayer PLAYER = PLAYER_MANAGER.createPlayer();
    private static final AudioManager MANAGER = Aroki.getServer().getAudioManager();
    private static final TrackScheduler QUEUE = new TrackScheduler(PLAYER);


    static {
        PLAYER.setVolume(80);
    }

    private static boolean PAUSED = false;
    private static boolean LOOP_TRACK = false;
    private static boolean LOOP_QUEUE = false;

    private static boolean connectToAVoiceChannel(Member member) {
        if (Objects.requireNonNull(member.getVoiceState()).inAudioChannel()) {
            VoiceChannel channel = Objects.requireNonNull(member.getVoiceState().getChannel()).asVoiceChannel();
            MANAGER.setSendingHandler(new AudioPlayerSendHandler(PLAYER));
            MANAGER.openAudioConnection(channel);

            PLAYER.addListener(QUEUE);

            return true;
        }
        return false;
    }

    public boolean botIsInAudioChannel() {
        return Objects.requireNonNull(Aroki.getServer().getSelfMember().getVoiceState()).inAudioChannel();
    }

    public boolean memberIsInUsedAudioChannel(Member member) {
        return (Objects.requireNonNull(Aroki.getServer().getSelfMember().getVoiceState()).inAudioChannel() == Objects.requireNonNull(member.getVoiceState()).inAudioChannel());
    }

    public boolean isPlaying() {
        return (PLAYER.getPlayingTrack() != null);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        Member member = event.getMember();
        assert member != null;
        switch (event.getName()) {
            case "play" -> {
                boolean canConnect = connectToAVoiceChannel(member);
                if (!canConnect) {
                    event.reply("You're not in a voice channel").setEphemeral(true).queue();
                    return;
                }
                ;
                String track = Objects.requireNonNull(event.getOption("query")).getAsString();
                PLAYER_MANAGER.loadItem(track, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack audioTrack) {
                        event.reply("Now playing: " + audioTrack.getInfo().title).queue();
                        QUEUE.queue(audioTrack);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist audioPlaylist) {
                        AudioTrack track = audioPlaylist.getSelectedTrack() != null
                                ? audioPlaylist.getSelectedTrack()
                                : audioPlaylist.getTracks().getFirst();
                        event.reply("Added " + audioPlaylist.getTracks().size() + " tracks to the queue.\nNow playing: " + track.getInfo().title).queue();
                        QUEUE.queue(track);
                        for (AudioTrack nextTrack : audioPlaylist.getTracks()) {
                            if (nextTrack != track) {  // Avoid adding the same track (which is already added)
                                QUEUE.queue(nextTrack);
                            }
                        }
                    }

                    @Override
                    public void noMatches() {
                        event.reply("No results.").queue();
                    }

                    @Override
                    public void loadFailed(FriendlyException e) {
                        event.reply("Failed to load track.").queue();
                    }
                });
            }
            case "join" -> {
                boolean canConnect = connectToAVoiceChannel(member);
                if (!canConnect) {
                    event.reply("You're not in a voice channel").setEphemeral(true).queue();
                    return;
                }
                event.reply("Joining " + Objects.requireNonNull(Objects.requireNonNull(member.getVoiceState()).getChannel()).asVoiceChannel().getAsMention()).setEphemeral(true).queue();
            }
            case "volume" -> {
                boolean canConnect = connectToAVoiceChannel(member);
                if (!canConnect) {
                    event.reply("You're not in a voice channel").setEphemeral(true).queue();
                    return;
                }
                if (memberIsInUsedAudioChannel(member)) {
                    int volume = Objects.requireNonNull(event.getOption("volume")).getAsInt();
                    String warning = "";

                    // Normalize volume
                    if (volume < 0) volume = 0;
                    else if (volume > 200) volume = 200;

                    if (volume > 100) {
                        warning = "Volume above 100 is not recommended as it might cause the audio quality to worsen";
                    }

                    event.reply("Set the volume to " + volume + "%\n" + warning).queue();
                    PLAYER.setVolume(volume);

                } else {
                    event.reply("I'm not connected to your voice channel.").setEphemeral(true).queue();
                }
            }
            case "pause" -> {
                boolean canConnect = connectToAVoiceChannel(member);
                if (!canConnect) {
                    event.reply("You're not in a voice channel").setEphemeral(true).queue();
                    return;
                }
                boolean playing = isPlaying();
                if (!playing) {
                    event.reply("The queue is empty").setEphemeral(true).queue();
                    return;
                }
                String reply;
                PAUSED = !PAUSED;
                if (PAUSED) {
                    reply = "▶️ Resumed";
                } else {
                    reply = "⏸️ Paused";
                }
                PLAYER.setPaused(PAUSED);
                event.reply(reply + " the queue").queue();
            }
            case "nowplaying" -> {
                boolean canConnect = connectToAVoiceChannel(member);
                if (!canConnect) {
                    event.reply("You're not in a voice channel").setEphemeral(true).queue();
                    return;
                }
                boolean playing = isPlaying();
                if (!playing) {
                    event.reply("The queue is empty").setEphemeral(true).queue();
                    return;
                }
                AudioTrack track = PLAYER.getPlayingTrack();
                AudioTrackInfo trackInfo = track.getInfo();
                long trackDuration = trackInfo.length;
                long trackPosition = track.getPosition();

                String imageUrl = trackInfo.artworkUrl != null ? trackInfo.artworkUrl : getYouTubeArtworkUrl(trackInfo.uri);

                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Now playing '" + trackInfo.title + "'")
                        .setDescription(durationBar((double) trackPosition / (double) trackDuration))
                        .setThumbnail(imageUrl)
                        .addField("Artist", trackInfo.author, true)
                        .addField("Duration", formatDuration(trackDuration), true)
                        .addField("Progress", formatDuration(trackPosition) + " / " + formatDuration(trackDuration), true)
                        .setColor(0x1DB954);

                System.out.println(imageUrl);

                event.reply("").addEmbeds(embed.build()).queue();
            }
            case "queue" -> {
                boolean canConnect = connectToAVoiceChannel(member);
                if (!canConnect) {
                    event.reply("You're not in a voice channel").setEphemeral(true).queue();
                    return;
                }
                boolean playing = isPlaying();
                if (!playing) {
                    event.reply("The queue is empty").setEphemeral(true).queue();
                    return;
                }
                EmbedBuilder embed = new EmbedBuilder();
                embed.setTitle("Current Queue");
                embed.setColor(0x1DB954);

                AudioTrack currentTrack = PLAYER.getPlayingTrack();
                AudioTrackInfo currentTrackInfo = currentTrack.getInfo();
                embed.addField("Now Playing", "**" + currentTrackInfo.title + "** by " + currentTrackInfo.author, false);

                List<AudioTrack> queue = new ArrayList<>(TrackScheduler.getQueue());
                int queueSize = queue.size();
                if (queueSize == 0) {
                    embed.addField("Next Tracks", "No more tracks in the queue.", false);
                } else {
                    StringBuilder queueMessage = new StringBuilder();
                    for (int i = 0; i < Math.min(5, queueSize); i++) {
                        AudioTrack nextTrack = queue.get(i);
                        AudioTrackInfo nextTrackInfo = nextTrack.getInfo();
                        queueMessage.append("**").append(nextTrackInfo.title).append("** by ").append(nextTrackInfo.author).append("\n");
                    }
                    embed.addField("Next Tracks", queueMessage.toString(), false);
                }
                event.reply("").addEmbeds(embed.build()).queue();
            }
            case "skip" -> {
                boolean canConnect = connectToAVoiceChannel(member);
                if (!canConnect) {
                    event.reply("You're not in a voice channel").setEphemeral(true).queue();
                    return;
                }
                boolean playing = isPlaying();
                if (!playing) {
                    event.reply("The queue is empty").setEphemeral(true).queue();
                    return;
                }
                int amount = (event.getOption("amount") != null) ? Objects.requireNonNull(event.getOption("amount")).getAsInt() : 1;
                BlockingQueue<AudioTrack> queue = TrackScheduler.getQueue();
                if (queue.size() < amount) amount = queue.size();
                for (int i = 0; i < amount; i++) {
                    AudioTrack nextTrack = queue.poll();
                    if (nextTrack != null) {
                        PLAYER.playTrack(nextTrack);
                    }
                }
                String reply;
                if (amount == 1) reply = "Skipped to the next track";
                else reply = "Skipped " + amount + " tracks";
                event.reply(reply).queue();
            }
            case "shuffle" -> {
                boolean canConnect = connectToAVoiceChannel(member);
                if (!canConnect) {
                    event.reply("You're not in a voice channel").setEphemeral(true).queue();
                    return;
                }
                BlockingQueue<AudioTrack> queue = TrackScheduler.getQueue();
                if (queue.isEmpty()) {
                    event.reply("Nothing in the queue!").setEphemeral(true).queue();
                    return;
                }
                QUEUE.shuffleQueue();
                event.reply("🔀 Shuffled the queue!").queue();
            }
            case "replay" -> {
                boolean canConnect = connectToAVoiceChannel(member);
                if (!canConnect) {
                    event.reply("You're not in a voice channel").setEphemeral(true).queue();
                    return;
                }
                boolean playing = isPlaying();
                if (!playing) {
                    event.reply("The queue is empty").setEphemeral(true).queue();
                    return;
                }
                PLAYER.playTrack(PLAYER.getPlayingTrack().makeClone());
                event.reply("Replaying").queue();
            }
            case "history" -> {
                if (!connectToAVoiceChannel(member)) {
                    event.reply("You're not in a voice channel").setEphemeral(true).queue();
                    return;
                }

                List<AudioTrack> HISTORY = TrackScheduler.getHistory();
                if (HISTORY.isEmpty()) {
                    event.reply("No songs have been played yet.").setEphemeral(true).queue();
                    return;
                }

                StringBuilder historyMessage = new StringBuilder("**Played Songs History**:\n");
                for (int i = 0; i < HISTORY.size(); i++) {
                    AudioTrack track = HISTORY.get(i);
                    historyMessage.append(i + 1).append(". **").append(track.getInfo().title)
                            .append("** by ").append(track.getInfo().author).append("\n");
                }

                event.reply(historyMessage.toString()).queue();
            }
        }
    }

    private String formatDuration(long duration) {
        long minutes = (duration / 1000) / 60;
        long seconds = (duration / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String durationBar(double percentage) {
        String progress = "▬";
        String now = "\uD83D\uDD18";
        StringBuilder bar = new StringBuilder();
        int p = (int) (percentage * 100) / 20;
        for (int i = 0; i < 20; i++) {
            if (i != p) {
                bar.append(progress);
            } else {
                bar.append(now);
            }
        }

        return bar.toString();
    }

    private String getYouTubeArtworkUrl(String trackUri) {
        String videoId = trackUri.split("v=")[1];
        if (videoId.contains("&")) {
            videoId = videoId.split("&")[0];
        }
        return "https://img.youtube.com/vi/" + videoId + "/sddefault.jpg";
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        VoiceChannel channel;

        AudioChannelUnion joinedChannel = event.getChannelJoined();
        AudioChannelUnion leftChannel = event.getChannelLeft();

        Guild guild = event.getGuild();

        // On join
        if (joinedChannel != null) {
            channel = joinedChannel.asVoiceChannel();

            Member aroki = guild.getSelfMember();
            List<Member> connectedMembers = channel.getMembers();
            if (connectedMembers.contains(aroki)) {
                if (connectedMembers.size() == 2 && !isPlaying()) {
                    PLAYER.setPaused(false);

                    ScheduledFuture<?> task = leaveTasks.remove(guild);
                    if (task != null) {
                        task.cancel(false);
                    }
                }
            }
        }
        // On leave
        if (leftChannel != null) {
            channel = leftChannel.asVoiceChannel();
            Member aroki = guild.getSelfMember();
            List<Member> connectedMembers = channel.getMembers();
            if (connectedMembers.contains(aroki)) {
                if (connectedMembers.size() == 1 && isPlaying()) {
                    PLAYER.setPaused(true);

                    leaveTasks.computeIfAbsent(guild, id ->
                            scheduler.schedule(() -> {
                                GuildVoiceState currentState = guild.getSelfMember().getVoiceState();
                                if (currentState != null && currentState.inAudioChannel()) {
                                    AudioChannel currentChannel = currentState.getChannel();
                                    boolean stillAlone = currentChannel.getMembers().stream()
                                            .allMatch(m -> m.getUser().isBot());

                                    if (stillAlone) {
                                        guild.getAudioManager().closeAudioConnection();
                                    }
                                }
                                leaveTasks.remove(id);
                            }, 5, TimeUnit.MINUTES)
                    );
                }
            }
        }
    }

}
