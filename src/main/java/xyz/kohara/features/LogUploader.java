// By SkyKing_PX
// Thankss uωu <3
//
// Edited by kohy
package xyz.kohara.features;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import xyz.kohara.Aroki;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class LogUploader extends ListenerAdapter {

    private static final Map<String, String> LOG_TIPS = Map.of(
            "java.lang.OutOfMemoryError",
            "`OutOfMemoryError` - JVM ran out of memory! Consider increasing your heap size.",

            "java.lang.AbstractMethodError",
            "`AbstractMethodError` - Sinytra Connector likely did something wrong while translating Fabric mods over to Forge. Consider clearing Connector mod cache (delete folder `.connector` inside `mods` folder and relaunch the game).",

            "- Essential",
            "`Essential` mod is redundant as ADJ has built-in world hosting feature, couretsy of the E4MC mod."
    );

    private static final List<String> DISCONTINUED_VERSIONS = List.of(
            "1.20.4",
            "1.20.6",
            "1.21.1",
            "1.21.4",
            "1.21.7",
            "1.21.8",
            "1.21.6"
    );

    private static final List<String> STORED_IDS = new ArrayList<>();
    private static final File LOGS_FOLDER = new File("logs_temp");

    static {
        if (!LOGS_FOLDER.exists()) {
            LOGS_FOLDER.mkdir();
        }
        for (File file : Objects.requireNonNull(LOGS_FOLDER.listFiles())) {
            file.delete();
        }
    }

    private static void deleteLastUploadMessage(MessageChannelUnion channel) {
        String id = STORED_IDS.getFirst();
        STORED_IDS.remove(id);

        channel.deleteMessageById(id).queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        MessageChannelUnion channel = event.getChannel();

        Map<File, String> fileMap = new HashMap<>();
        AtomicInteger iter = new AtomicInteger();

        List<?> validAttachments = event.getMessage().getAttachments().stream()
                .filter(attachment -> attachment.getFileName().matches(".*\\.(log|txt|gz)$"))
                .toList();

        if (!validAttachments.isEmpty()) {
            channel.sendMessage("*Uploading to [mclo.gs](https://mclo.gs)...*").queue(msg -> {
                STORED_IDS.add(msg.getId());
            });
        }

        validAttachments.forEach(att -> {
            var attachment = (Message.Attachment) att;
            String extension = attachment.getFileExtension();
            extension = extension.equals("gz") ? "info" : extension;

            File tempFile = new File(LOGS_FOLDER, "temp-" + UUID.randomUUID() + "." + extension);
            fileMap.put(tempFile, attachment.getFileName());

            attachment.getProxy().download().thenAccept(inputStream -> {
                try {
                    if (attachment.getFileExtension().equals("gz")) {
                        File gz = new File(tempFile + ".gz");
                        saveInputStreamToFile(inputStream, gz);
                        decompressGzipFile(gz.getPath(), tempFile.getPath());
                    } else {
                        saveInputStreamToFile(inputStream, tempFile);
                    }

                    if (Files.size(tempFile.toPath()) > 10 * 1024 * 1024 /* 10 megabytes size limit */) {
                        tempFile.delete();
                        channel.sendMessage(":x: `" + attachment.getFileName() + "` is too large to upload to mclo.gs automatically").queue();
                        deleteLastUploadMessage(channel);
                        return;
                    }

                    if (iter.incrementAndGet() == validAttachments.size()) {
                        CompletableFuture.runAsync(() -> uploadAndSendLinks(fileMap, event));
                    }
                } catch (Exception e) {
                    tempFile.delete();
                    channel.sendMessage(":x: Error saving file `" + attachment.getFileName() + "`").queue();
                    deleteLastUploadMessage(channel);
                }
            });
        });
    }

    private void saveInputStreamToFile(InputStream inputStream, File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private void uploadAndSendLinks(Map<File, String> fileMap, MessageReceivedEvent event) {
        Map<String, List<String>> uploads = new HashMap<>();
        Map<String, List<String>> tips = new HashMap<>();

		for (Map.Entry<File, String> entry : fileMap.entrySet()) {
            File tempFile = entry.getKey();
            String originalName = entry.getValue();

            try {
                UploadResult result = uploadToMclogs(tempFile);
                uploads.put(originalName, result.apiResult);

                if (!result.tips.isEmpty()) {
                    tips.put(originalName, result.tips);
                }

                tempFile.delete();
            } catch (IOException e) {
                event.getChannel().sendMessage(":warning: Error uploading file `" + originalName + "`").queue();
            }
        }


        if (!uploads.isEmpty()) {

            MessageCreateBuilder builder = new MessageCreateBuilder();
            builder.setContent("");

            if (!tips.isEmpty()) {
                StringBuilder builder1 = new StringBuilder();

                if (uploads.size() == 1) {
                    builder1.append(":warning: **I have briefly analyzed your log file, here's what I can advise:**\n");

                    tips.forEach((s, strings) -> strings.forEach(s1 -> builder1.append("* ").append(s1).append("\n")));
                }
                else {
                    builder1.append(":warning: **I have briefly analyzed your log files, here's what I can advise:**\n");
                    tips.forEach((s, strings) -> {
                        builder1.append("* `").append(s).append("`:\n");
                        strings.forEach(s1 -> builder1.append("  * ").append(s1).append("\n"));
                    });
                }

                builder.setContent(String.valueOf(builder1));
            }

            // 'key' is the original file name
            boolean separateRows = false;
            List<Button> buttonsOuter = new ArrayList<>();
            for (String key : uploads.keySet()) {
                List<Button> buttons = new ArrayList<>();
                List<String> data = uploads.get(key);
                String url = data.getFirst();
                buttons.add(Button.link(url, key).withEmoji(Emoji.fromFormatted("<:mclogs:1359506468344299530>")));
                /*
                    If it isn't a crash report or a info, size of the list will be 2 (look at 'uploadToMcLogs')
                    We don't add the 2nd button with quick info if it's a random ass txt file.
                */
                if (data.size() > 2) {
                    separateRows = true;
                    String name = data.get(1), type = data.get(2), version = data.get(3);
                    // This could also be inlined but I left it like this so that it's at least slightly easier to work with
                    String label = name + " " + type + " (" + version + ")";
                    if (isDiscontinued(version)) {
                        buttons.add(Button.danger(key, label)
                                .withEmoji(Emoji.fromFormatted("📜"))
                                .asDisabled()
                        );
                    } else {
                        buttons.add(Button.primary(key, label)
                                .withEmoji(Emoji.fromFormatted("📜"))
                                .asDisabled()
                        );
                    }
                }
                if (separateRows) {
                    builder.addActionRow(buttons);
                }
                else {
                    buttonsOuter.addAll(buttons);
                }
            }
            if (!separateRows) builder.addActionRow(buttonsOuter);
            deleteLastUploadMessage(event.getChannel());
            event.getMessage().reply(builder.build()).mentionRepliedUser(false).queue();
        }
    }

    private UploadResult uploadToMclogs(File file) throws IOException {
        StringBuilder sb = new StringBuilder();
        List<String> tips = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');

                for (String key : LOG_TIPS.keySet()) {
                    if (line.contains(key) && !tips.contains(LOG_TIPS.get(key))) {
                        tips.add(LOG_TIPS.get(key));
                    }
                }

                if (sb.length() > 10_000_000) throw new IOException("Log too large");
            }
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost("https://api.mclo.gs/1/log");
            post.setEntity(new UrlEncodedFormEntity(
                    List.of(new BasicNameValuePair("content", sb.toString())),
                    StandardCharsets.UTF_8
            ));

            try (CloseableHttpResponse response = client.execute(post)) {
                String result = new String(response.getEntity().getContent().readAllBytes());
                JsonNode jsonNode = new ObjectMapper().readTree(result);

                if (!jsonNode.path("success").asBoolean(false)) {
                    throw new IOException(jsonNode.path("error").asText("Upload failed"));
                }

                String url = jsonNode.get("url").asText();
                String id = jsonNode.get("id").asText();
                List<String> apiResult = new ArrayList<>();
                apiResult.add(url);
                apiResult.addAll(getLogTitle(id));

                return new UploadResult(tips, apiResult);
            }
        }
    }

    private List<String> getLogTitle(String id) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet get = new HttpGet("https://api.mclo.gs/1/insights/" + id);
            try (CloseableHttpResponse response = client.execute(get)) {
                String result = new String(response.getEntity().getContent().readAllBytes());
                JsonNode jsonNode = new ObjectMapper().readTree(result);

                if (!jsonNode.path("success").asBoolean(false)) {
                    return List.of("null");
                }

                String name = jsonNode.path("name").asText("Unknown");
                String type = jsonNode.path("type").asText("Log");
                String version = jsonNode.path("version").asText("Unknown");

                return List.of(name, type, version);
            }
        }
    }

    private String jsonNodeField(JsonNode jsonNode, String name) {
        return jsonNode.get(name).asText();
    }

    private void decompressGzipFile(String gzipFile, String outputFile) {
        File gzip = new File(gzipFile);
        try (
                FileInputStream fileInputStream = new FileInputStream(gzip);
                GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
                FileOutputStream fileOutputStream = new FileOutputStream(outputFile)
        ) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        gzip.delete();
    }

    private static boolean isDiscontinued(String version) {
        for (String discontinuedPattern : DISCONTINUED_VERSIONS) {
            String regex = discontinuedPattern.replace("*", ".*");
            Pattern pattern = Pattern.compile(regex);
            if (pattern.matcher(version).find()) {
                return true;
            }
        }
        return false;
    }

    private static class UploadResult {
        public List<String> tips;
        public List<String> apiResult;

        public UploadResult(List<String> tips, List<String> apiResult) {
            this.tips = tips;
            this.apiResult = apiResult;
        }
    }
}
