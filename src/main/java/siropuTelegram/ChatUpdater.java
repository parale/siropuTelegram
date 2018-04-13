package siropuTelegram;

import siropuTelegram.XenForo.ChatMessage;
import siropuTelegram.XenForo.Thread;
import siropuTelegram.XenForo.XenForo;

import java.util.ArrayList;

class ChatUpdater extends java.lang.Thread {
    private TelegramBot bot;

    ChatUpdater(TelegramBot bot) {
        this.bot = bot;
    }

    @Override
    public void run() {
        try {
            updateChat();
        } catch (InterruptedException e) {
            Logger.logException(e);
        }
    }

    private void sendToClients(int authorXfId, String message) {
        ArrayList<User> clients;
        XenForo forum = new XenForo();
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
        while (!isInterrupted()) {
            forum = new XenForo();
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
            sleep(1000);
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
