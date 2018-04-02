package siropuTelegram;

import siropuTelegram.XenForo.ChatMessage;
import siropuTelegram.XenForo.Thread;
import siropuTelegram.XenForo.XenForo;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

class ChatUpdater extends java.lang.Thread {
    private Logger LOGGER = Solution.LOGGER;
    private TelegramBot bot;

    ChatUpdater(TelegramBot bot) {
        this.bot = bot;
    }

    @Override
    public void run() {
        try {
            updateChat();
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
    }

    private void sendToClients(int authorXfId, String message) {
        ArrayList<User> clients;
        XenForo forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);
        clients = forum.getClientsList();

        for (User user : clients) {
            if (user.getXfUserId() != authorXfId) {
                bot.replyTo(user.getTelegramChatId(), message);
            }
        }

        forum.close();
    }

    private void updateChat() throws InterruptedException {
        XenForo forum;
        ArrayList<ChatMessage> messages;
        int counter = 0;
        while (true) {
            forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);
            messages = forum.getAllMessages();

            if (messages != null) {
                for (ChatMessage message : messages) {
                    sendToClients(
                            message.getAuthorId(),
                            String.format("%s: %s", message.getAuthorName(), message.getMessage())
                    );
                }
            }

            counter++;

            if (counter == 60) {
                updateThreads(forum);
                counter = 0;
            }

            forum.close();
            java.lang.Thread.sleep(1000);
        }
    }

    private void updateThreads(XenForo forum) {
        ArrayList<Thread> threads = forum.getNewThreads();

        if (threads != null) {
            for (Thread thread : threads) {
                sendToClients(0, String.format(
                        "%s %s \"%s\": %s",
                        thread.getAuthor(),
                        Properties.res.getString("newThread"),
                        thread.getTitle(),
                        thread.getUrl()
                ));
            }
        }
    }
}
