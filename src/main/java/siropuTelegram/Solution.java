package siropuTelegram;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.exceptions.TelegramApiRequestException;
import siropuTelegram.XenForo.XenForo;

import java.io.IOException;

public class Solution {
    public static void main(String[] args) {
        try {
            new Properties();
            new Logger();
        } catch (IOException e) {
            System.out.println("Can't read properties file.");
            e.printStackTrace();
            System.exit(1);
        }

        XenForo forum = new XenForo();
        forum.createTables();
        forum.checkUserField();
        forum.close();

        ApiContextInitializer.init();
        TelegramBotsApi botsApi = new TelegramBotsApi();

        try {
            botsApi.registerBot(new TelegramBot());
        } catch (TelegramApiRequestException e) {
            Logger.logException(e);
            System.exit(1);
        }
    }
}
