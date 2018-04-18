package siropuTelegram;

import siropuTelegram.XenForo.XenForo;

import java.io.*;
import java.util.ResourceBundle;

public class Properties {
    private static final String LATEST_VERSION = "4";

    private static java.util.Properties properties = new java.util.Properties();

    public static String db_host, db_user, db_password, settings_table, users_table, follow_table, xf_prefix;
    public static String bot_token, bot_username, logging;
    public static String saveto, mediaurl, dev, forumurl, exclude_nodes;
    public static String ffmpeg;
    private static String version;

    public static int lastMessageId = 0;
    public static int lastThreadId = 0;

    public static ResourceBundle res;

    Properties() throws IOException {
        setProperties();
    }

    private static String getProperty(String key, String description) throws IOException {
        String value = properties.getProperty(key);
        if (value.isEmpty()) {
            System.out.println(description);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String result = bufferedReader.readLine();
            bufferedReader.close();
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
            new File("bot.properties");
            fileInputStream = new FileInputStream("bot.properties");
        }

        properties.load(fileInputStream);

        version = properties.getProperty("version", LATEST_VERSION);
        checkPropertiesVersion();

        db_host = getProperty("db_host", "Database address (example: hostname:port/db_name)");
        db_user = getProperty("db_user", "Database username");
        db_password = getProperty("db_password", "Database password");
        xf_prefix = getProperty("xf_prefix", "XenForo tables prefix (default: xf_)");

        settings_table = getProperty("settings_table", "stchat_settings");
        users_table = getProperty("users_table", "stchat_users");
        follow_table = getProperty("follow_table", "stchat_follow");

        bot_token = getProperty("bot_token", "Telegram bot token api");
        bot_username = getProperty("bot_username", "Telegram bot username");

        dev = getProperty("dev", "0");

        forumurl = getProperty("forumurl", "Forum url (example: https://forum.com/)");
        saveto = getProperty("saveto", "Media folder (should be visible from web, e.g. /home/forum/ww/media/)");
        mediaurl = getProperty("mediaurl", "Url to the media folder (https://hostname/media/)");
        ffmpeg = getProperty("ffmpeg", "Ffmpeg binary path (to convert webp stickers to png)");

        res = ResourceBundle.getBundle("locale." + properties.getProperty("lang", "en"));

        exclude_nodes = properties.getProperty("exclude_nodes", "");
        logging = properties.getProperty("logging", "0");

        fileInputStream.close();

        FileOutputStream fileOutputStream = new FileOutputStream("bot.properties");
        properties.store(fileOutputStream, null);
        fileInputStream.close();
    }

    private static void checkPropertiesVersion() throws IOException {
        int ver;

        try {
            ver = Integer.parseInt(version);
        } catch (NumberFormatException e) {
            ver = 0;
        }

        if (ver < 4) {
            FileOutputStream fileOutputStream = new FileOutputStream("bot.properties");

            if (ver < 1) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

                System.out.println("Forum url (example: https://forum.com/):");
                properties.setProperty("forumurl", bufferedReader.readLine());

                bufferedReader.close();

                properties.setProperty("version", "1");
            }

            if (ver < 2) {
                XenForo forum = new XenForo();
                forum.updateTables(2);
                forum.close();

                properties.setProperty("version", "2");
            }

            if (ver < 3) {
                properties.setProperty("exclude_nodes", "");
                properties.setProperty("version", "3");
            }

            properties.setProperty("follow_table", "stchat_follow");
            properties.setProperty("version", "4");

            XenForo forum = new XenForo();
            forum.updateTables(4);
            forum.close();

            properties.store(fileOutputStream, null);
            setProperties();
            fileOutputStream.close();
        }
    }
}
