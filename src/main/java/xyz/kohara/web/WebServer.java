package xyz.kohara.web;


import com.google.gson.Gson;
import xyz.kohara.Aroki;
import xyz.kohara.Config;
import xyz.kohara.features.linking.LinkManager;

import static spark.Spark.*;

public class WebServer {

    private static final Gson GSON = new Gson();

    public static void start() {
        Aroki.log("Starting web server");

        port(26980);

        get("/hello", (req, res) -> "Well hi there");

        post("/adj_finish", (request, response) -> {
            String body = request.body();
            BodyTypes.ADJFinish bodyParsed = GSON.fromJson(body, BodyTypes.ADJFinish.class);

            Aroki.getServer().getTextChannelById(Config.get(Config.Option.ADJ_INFO_CHANNEL)).sendMessage(
                    ":tada: **`" + bodyParsed.name + "` has just finished ADJ!**" +
                            "\n> `" + bodyParsed.uuid + "`"
            ).queue();

            return "";
        });

        post("/adj_link", ((request, response) -> {
            String body = request.body();
            BodyTypes.ADJLink bodyParsed = GSON.fromJson(body, BodyTypes.ADJLink.class);

            if (bodyParsed.uuid == null) {
                return "No UUID field";
            }

            int code = LinkManager.tryGenerateLinkCode(bodyParsed.uuid);
            return (code == 0) ? "UUID already linked" : code;
        }));
    }

    private static class BodyTypes {

        static class ADJFinish {
            String name;
            String uuid;
        }

        static class ADJLink {
            String uuid;
        }
    }
}
