package siropuTelegram;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

public class Logger {
    private static java.util.logging.Logger logger;

    public Logger() {
        if (Properties.logging.equals("1")) {
            logger = java.util.logging.Logger.getLogger("Telegram bot logs");
            initFileHandler();
        }
    }

    public static void logInfo(String text) {
        if (Properties.logging.equals("1")) {
            logger.log(Level.INFO, text);
        }
    }

    public static void logSevere(String text) {
        if (Properties.logging.equals("1")) {
            logger.log(Level.SEVERE, text);
        }
    }

    public static void logException(Exception e) {
        if (Properties.logging.equals("1")) {
            logger.log(Level.SEVERE, e.toString(), e);
        }
    }

    private static void initFileHandler() {
        try {
            FileHandler fileHandler = new FileHandler("bot.log");
            logger.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
        } catch (IOException e) {
            System.out.println("Unable to create log file.");
        }
    }
}
