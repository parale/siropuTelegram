package siropuTelegram;

import siropuTelegram.XenForo.Thread;
import siropuTelegram.XenForo.XenForo;

import java.util.ArrayList;

public class ThreadUpdater extends ChatUpdater {
    public ThreadUpdater(TelegramBot bot) {
        super(bot);
    }

    @Override
    public void run() {
        while (!interrupted()) {
            XenForo forum = new XenForo();

            updateThreads(forum);

            forum.close();
            try {
                sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
