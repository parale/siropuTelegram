package siropuTelegram;

import java.io.*;

public class Properties {
    private static java.util.Properties properties = new java.util.Properties();

    public static String db_host, db_user, db_password, settings_table, users_table, follow_table, xf_prefix;
    public static String bot_token, bot_username, logging;
    public static String saveto, mediaurl, dev, forumurl, exclude_nodes;
    public static String ffmpeg;

    public static int lastMessageId = 0;
    public static int lastThreadId = 0;
    public static int lastPostId = 0;

    public static java.util.Properties strings = new java.util.Properties();

    private static BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

    Properties() throws IOException {
        setProperties();
        saveProperties();
    }

    private static String getProperty(String key, String description) throws IOException {
        String value = properties.getProperty(key, "");
        if (value.isEmpty()) {
            System.out.println(description);
            String result = bufferedReader.readLine();
            properties.setProperty(key, result);
            return result;
        } else {
            return value;
        }
    }

    private static void setProperties() throws IOException {
        FileInputStream fileInputStream;

        try {
            fileInputStream = new FileInputStream("bot.properties");
        } catch (FileNotFoundException e) {
            new File("bot.properties").createNewFile();
            fileInputStream = new FileInputStream("bot.properties");
        }

        properties.load(fileInputStream);

        db_host = getProperty("db_host", "Database address (example: hostname:port/db_name)");
        db_user = getProperty("db_user", "Database username");
        db_password = getProperty("db_password", "Database password");
        xf_prefix = getProperty("xf_prefix", "XenForo tables prefix (default: xf_)");

        settings_table = properties.getProperty("settings_table", "stchat_settings");
        users_table = properties.getProperty("users_table", "stchat_users");
        follow_table = properties.getProperty("follow_table", "stchat_follow");

        bot_token = getProperty("bot_token", "Telegram bot token api");
        bot_username = getProperty("bot_username", "Telegram bot username");

        dev = properties.getProperty("dev", "0");

        forumurl = getProperty("forumurl", "Forum url (example: https://forum.com/)");
        saveto = getProperty("saveto", "Media folder (should be visible from web, e.g. /home/forum/ww/media/)");
        mediaurl = getProperty("mediaurl", "Url to the media folder (https://hostname/media/)");
        ffmpeg = getProperty("ffmpeg", "Ffmpeg binary path (to convert webp stickers to png)");

        Reader reader = new InputStreamReader(Properties.class.getClassLoader().getResourceAsStream("locale/" + properties.getProperty("lang", "en") + ".properties"), "UTF-8");
        strings.load(reader);
        reader.close();

        exclude_nodes = properties.getProperty("exclude_nodes", "");
        logging = properties.getProperty("logging", "0");

        fileInputStream.close();
        bufferedReader.close();
    }

    private void saveProperties() throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream("bot.properties");

        properties.setProperty("db_host", db_host);
        properties.setProperty("db_user", db_user);
        properties.setProperty("db_password", db_password);
        properties.setProperty("xf_prefix", xf_prefix);
        properties.setProperty("settings_table", settings_table);
        properties.setProperty("users_table", users_table);
        properties.setProperty("follow_table", follow_table);
        properties.setProperty("bot_token", bot_token);
        properties.setProperty("bot_username", bot_username);
        properties.setProperty("dev", dev);
        properties.setProperty("forumurl", forumurl);
        properties.setProperty("saveto", saveto);
        properties.setProperty("mediaurl", mediaurl);
        properties.setProperty("ffmpeg", ffmpeg);
        properties.setProperty("exclude_nodes", exclude_nodes);
        properties.setProperty("logging", logging);

        properties.store(fileOutputStream, null);
        fileOutputStream.close();
    }
}
