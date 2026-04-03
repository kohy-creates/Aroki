package xyz.kohara.status;

import net.dv8tion.jda.api.entities.Activity;
import xyz.kohara.Aroki;
import xyz.kohara.Config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class BotActivity {

    private static final String LOCATION = "data/status.txt";
    private static final int INTERVAL;
    private static final List<String> ACTIVITIES = new ArrayList<>();

    static {
        var intervals = Config.get(Config.Option.STATUS_CHANGE_INTERVAL).split(":");
        INTERVAL = ((Integer.parseInt(intervals[0])  * 60 * 60) /* hours */
                    + (Integer.parseInt(intervals[1])  * 60) /* minutes */
                    + (Integer.parseInt(intervals[2]))) /* seconds */
                    * 1000 /* all to milliseconds */;

        try {
            File statusTXT = new File(LOCATION);
            if (statusTXT.exists()) {
				ACTIVITIES.addAll(Files.readAllLines(statusTXT.toPath()));
            }
            else {
                Aroki.Logger.error("File not found: \"{}\"", LOCATION);
            }
        }
        catch (IOException e) {
            Aroki.Logger.error("Something went wrong trying to read \"{}\"!", LOCATION);
            throw new RuntimeException(e);
        }
    }

    public static void schedule() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new ChangeStatusTask(), 0, INTERVAL);
    }

    private static class ChangeStatusTask extends TimerTask {

        private final Random random = new Random();

        @Override
        public void run() {
            Aroki.Logger.debug("Choosing new status...");
            Activity randomActivity;
            Activity current = Aroki.getBot().getPresence().getActivity();
            do {
                randomActivity = getRandomActivity();
            } while (randomActivity == current);
            Aroki.Logger.debug("New status: {}", randomActivity.getName());
            Aroki.getBot().getPresence().setActivity(randomActivity);
        }

        private Activity getRandomActivity() {
            var str = ACTIVITIES.get(random.nextInt(ACTIVITIES.size()));
            return Activity.customStatus(Aroki.Placeholders.parse(str));
        }
    }
}
