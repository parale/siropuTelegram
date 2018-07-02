package siropuTelegram;

import siropuTelegram.XenForo.Post;
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

            if (Properties.lastPostId == 0) forum.updateLastPostId();

            newPosts(forum);
            newThreads(forum);

            forum.close();
            try {
                sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void newThreads(XenForo forum) {
        ArrayList<Thread> threads = forum.getNewThreads();

        if (threads != null) {
            for (Thread thread : threads) {
                sendToClients(0, String.format(
                        "%s %s \"%s\": %s",
                        thread.getAuthor(),
                        Properties.strings.getProperty("newThread"),
                        thread.getTitle(),
                        thread.getUrl()
                ));
            }
        }
    }

    private void newPosts(XenForo forum) {
        ArrayList<User> users = forum.getFollowers();

        if (!users.isEmpty()) {
            for (User user : users) {
                ArrayList<Post> posts = forum.getNewMessages(user);
                if (!posts.isEmpty()) {
                    for (Post post : posts) {
                        if (post.getAuthorId() != user.getXfUserId()) {
                            String message = String.format("\uD83C\uDD95 %s «%s», %s: %s",
                                    Properties.strings.getProperty("newPost"),
                                    post.getThreadTitle(),
                                    Properties.strings.getProperty("link").toLowerCase(),
                                    post.getUrl()
                            );

                            bot.replyTo(
                                    user.getTelegramChatId(),
                                    message,
                                    true
                            );
                        }
                    }
                }
            }
        }

        forum.updateLastPostId();
    }
}
