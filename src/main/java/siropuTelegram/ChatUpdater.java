package siropuTelegram;

import siropuTelegram.XenForo.ChatMessage;
import siropuTelegram.XenForo.XenForo;

import java.util.ArrayList;

class ChatUpdater extends java.lang.Thread {
    protected TelegramBot bot;

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

    protected void sendToClients(int authorXfId, String message) {
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

            forum.close();
            sleep(1000);
        }
    }
}
