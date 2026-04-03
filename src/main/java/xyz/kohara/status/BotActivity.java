package xyz.kohara.status;

import com.google.gson.Gson;
import net.dv8tion.jda.api.entities.Activity;
import xyz.kohara.Aroki;
import xyz.kohara.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class BotActivity {

    private static final List<Activity> STATUS_LIST = new ArrayList<>();
    private static final int INTERVAL;
    private static final List<Activity> ACTIVITIES = new ArrayList<>();

    static {
        var intervals = Config.get(Config.Option.STATUS_CHANGE_INTERVAL).split(":");
        INTERVAL = ((Integer.parseInt(intervals[0])  * 60 * 60) /* hours */
                    + (Integer.parseInt(intervals[1])  * 60) /* minutes */
                    + (Integer.parseInt(intervals[2]))) /* seconds */
                    * 1000 /* all to milliseconds */;

        try {
            File statusTXT = new File("data/status.txt");
            if (statusTXT.exists()) {
                var allStatuses = Files.readAllLines(statusTXT.toPath());
                allStatuses.forEach(s -> {
                    ACTIVITIES.add(Activity.customStatus(Aroki.Placeholders.parse(s)));
                });
            }
        }
        catch (IOException e) {
            Aroki.Logger.error("Something went wrong trying to read \"data/status.txt\"!");
            throw new RuntimeException(e);
        }
    }

    public static void schedule() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Activity randomActivity;
                Activity current = Aroki.getBot().getPresence().getActivity();
                do {
                    randomActivity = STATUS_LIST.get(new Random().nextInt(STATUS_LIST.size()));
                } while (randomActivity == current);
                Aroki.getBot().getPresence().setActivity(randomActivity);
            }
        }, 0, INTERVAL);
    }
}
