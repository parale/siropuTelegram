import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Solution {
    static final Logger LOGGER = Logger.getLogger("XenForo to Telegram bot logs");

    public static void main(String[] args) {
        initFileHandler();

        try {
            new Properties();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Can't read properties file.");
            LOGGER.log(Level.SEVERE, e.toString(), e);
            System.exit(1);
        }

        XenForo forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);
        forum.createTables();
        forum.checkUserField();
        forum.close();

        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();

        try {
            botsApi.registerBot(new TelegramBot());
        } catch (TelegramApiRequestException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
            System.exit(1);
        }
    }

    private static void initFileHandler() {
        try {
            FileHandler fileHandler = new FileHandler("bot.log");
            LOGGER.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
        } catch (IOException e) {
            System.out.println("Unable to create log file.");
        }
    }
}
