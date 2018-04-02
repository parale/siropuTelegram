package siropuTelegram;

import java.io.*;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Properties {
    private static final java.util.Properties properties = new java.util.Properties();

    public static String db_host, db_user, db_password, settings_table, users_table, xf_prefix;
    public static String bot_token, bot_username;
    public static String saveto, mediaurl, dev, sqlconnections, forumurl;
    public static String ffmpeg;
    private static String version;

    public static int lastMessageId = 0;
    public static int lastThreadId = 0;

    public static ResourceBundle res;

    Properties() throws IOException {
        try {
            setProperties();
        } catch (FileNotFoundException e) {
            Logger LOGGER = Solution.LOGGER;
            LOGGER.log(Level.SEVERE, e.toString(), e);
            initProperties();
        }
    }

    private static void setProperties() throws IOException {
        FileInputStream fileInputStream = new FileInputStream("bot.properties");
        properties.load(fileInputStream);
        db_host = properties.getProperty("db_host");
        db_user = properties.getProperty("db_user");
        db_password = properties.getProperty("db_password");
        settings_table = properties.getProperty("settings_table");
        users_table = properties.getProperty("users_table");
        bot_token = properties.getProperty("bot_token");
        bot_username = properties.getProperty("bot_username");
        dev = properties.getProperty("dev");
        saveto = properties.getProperty("saveto");
        mediaurl = properties.getProperty("mediaurl");
        ffmpeg = properties.getProperty("ffmpeg");
        sqlconnections = properties.getProperty("sqlconnections");
        xf_prefix = properties.getProperty("xf_prefix");
        res = ResourceBundle.getBundle("locale." + properties.getProperty("lang"));
        forumurl = properties.getProperty("forumurl");
        version = properties.getProperty("version");
        fileInputStream.close();

        checkPropertiesVersion();
    }

    private static void initProperties() throws IOException {
        File file = new File("bot.properties");
        if (file.createNewFile()) {
            FileOutputStream fileOutputStream = new FileOutputStream("bot.properties");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Database address (example: hostname:port/db_name):");
            properties.setProperty("db_host", bufferedReader.readLine());
            System.out.println("Database username:");
            properties.setProperty("db_user", bufferedReader.readLine());
            System.out.println("Database password:");
            properties.setProperty("db_password", bufferedReader.readLine());
            System.out.println("Settings table name (example: transport_settings):");
            properties.setProperty("settings_table", bufferedReader.readLine());
            System.out.println("Clients list table name (example: transport_clients):");
            properties.setProperty("users_table", bufferedReader.readLine());
            System.out.println("siropuTelegram.XenForo.siropuTelegram.XenForo tables prefix (default: xf_):");
            properties.setProperty("xf_prefix", bufferedReader.readLine());
            System.out.println("Telegram bot token api:");
            properties.setProperty("bot_token", bufferedReader.readLine());
            System.out.println("Telegram bot username:");
            properties.setProperty("bot_username", bufferedReader.readLine());
            System.out.println("Forum url (example: https://forum.com/):");
            properties.setProperty("forumurl", bufferedReader.readLine());
            System.out.println("Media folder (should be visible from web):");
            properties.setProperty("saveto", bufferedReader.readLine());
            System.out.println("Url to the media folder (https://hostname/media/):");
            properties.setProperty("mediaurl", bufferedReader.readLine());
            System.out.println("Ffmpeg binary path (to convert webp stickers to png):");
            properties.setProperty("ffmpeg", bufferedReader.readLine());

            bufferedReader.close();

            properties.setProperty("sqlconnections", "0");
            properties.setProperty("dev", "0");
            properties.setProperty("lang", "en");
            properties.setProperty("version", "1");

            properties.store(fileOutputStream, null);
            fileOutputStream.close();

            setProperties();
        } else {
            throw new IOException();
        }
    }


    private static void checkPropertiesVersion() throws IOException {
        int ver;

        try {
            ver = Integer.parseInt(version);
        } catch (NumberFormatException e) {
            ver = 0;
        }

        if (ver < 1) {
            FileOutputStream fileOutputStream = new FileOutputStream("bot.properties");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Forum url (example: https://forum.com/):");
            properties.setProperty("forumurl", bufferedReader.readLine());

            bufferedReader.close();

            properties.setProperty("version", "1");

            properties.store(fileOutputStream, null);
            fileOutputStream.close();

            setProperties();
        }
    }
}
