package xyz.kohara.features.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import xyz.kohara.Aroki;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

public class TrackScheduler extends AudioEventAdapter {

    private final AudioPlayer PLAYER;
    private static final BlockingQueue<AudioTrack> QUEUE = new LinkedBlockingQueue<>();  // Using a static QUEUE

    private static final LinkedList<AudioTrack> HISTORY = new LinkedList<>();
    private static final int HISTORY_LIMIT = 10;

    public TrackScheduler(AudioPlayer player) {
        this.PLAYER = player;
    }

    public void queue(AudioTrack track) {
        if (!PLAYER.startTrack(track, true)) {
            if (!QUEUE.offer(track)) {
                Aroki.Logger.error("Failed to add track to the queue!");
            };
        }
    }

    public void shuffleQueue() {
        List<AudioTrack> tracks = new ArrayList<>(QUEUE);
        Collections.shuffle(tracks);
        replaceQueue(tracks);
    }

    public void replaceQueue(List<AudioTrack> tracks) {
        QUEUE.clear();
        QUEUE.addAll(tracks);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            AudioTrack nextTrack = QUEUE.poll();

            if (nextTrack != null) {
                if (HISTORY.size() >= HISTORY_LIMIT) {
                    HISTORY.removeFirst();
                }
                HISTORY.add(track);
                player.startTrack(nextTrack, false);
            } else {
                player.stopTrack();
            }
        }
    }

    // Returns the song queue for access outside of this class
    public static BlockingQueue<AudioTrack> getQueue() {
        return QUEUE;
    }

    // Same for play history
    public static LinkedList<AudioTrack> getHistory() {
        return HISTORY;
    }
}