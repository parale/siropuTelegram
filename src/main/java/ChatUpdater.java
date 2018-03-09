import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatUpdater extends Thread {
    private Logger LOGGER = Solution.LOGGER;
    private TelegramBot bot;

    public ChatUpdater(TelegramBot bot) {
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

    private void updateChat() throws InterruptedException {
        XenForo forum;
        HashMap<Integer, ArrayList<Serializable>> messages;
        HashMap<Integer, Long> clients;
        while (true) {
            forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);
            messages = forum.getAllMessages();

            if (messages != null) {
                clients = forum.getClientsList();
                for (HashMap.Entry<Integer, ArrayList<Serializable>> message : messages.entrySet()) {
                    for (HashMap.Entry<Integer, Long> client : clients.entrySet()) {
                        if (client.getKey() != (int) message.getValue().get(3)) {
                            bot.replyTo(client.getValue(), String.format("%s: %s", message.getValue().get(0), message.getValue().get(1)));
                        }
                    }
                }

                messages = null;
                clients = null;
            }

            forum.close();
            Thread.sleep(1000);
        }
    }
}
