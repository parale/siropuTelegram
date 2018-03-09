import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Properties {
    private Logger LOGGER = Solution.LOGGER;
    static java.util.Properties properties = new java.util.Properties();

    static String db_host, db_user, db_password, settings_table, users_table, xf_prefix;
    static String bot_token, bot_username;
    static String saveto, mediaurl, dev, sqlconnections;
    static String ffmpeg, lang;

    Properties() throws IOException {
        try {
            setProperties();
        } catch (FileNotFoundException e) {
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
        lang = properties.getProperty("lang");
    }

    private static void initProperties() throws IOException {
        File file = new File("bot.properties");
        file.createNewFile();
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
        System.out.println("XenForo tables prefix (default: xf_):");
        properties.setProperty("xf_prefix", bufferedReader.readLine());
        System.out.println("Telegram bot token api:");
        properties.setProperty("bot_token", bufferedReader.readLine());
        System.out.println("Telegram bot username:");
        properties.setProperty("bot_username", bufferedReader.readLine());
        System.out.println("Media folder (should be visible from web):");
        properties.setProperty("saveto", bufferedReader.readLine());
        System.out.println("Url to media folder (https://hostname/media/):");
        properties.setProperty("mediaurl", bufferedReader.readLine());
        System.out.println("Ffmpeg binary path (to convert webp stickers to png):");
        properties.setProperty("ffmpeg", bufferedReader.readLine());

        properties.setProperty("sqlconnections", "0");
        properties.setProperty("dev", "0");
        properties.setProperty("lang", "en");

        properties.store(fileOutputStream, null);

        setProperties();
    }
}
