package siropuTelegram.XenForo;

import siropuTelegram.Properties;

public class Post {
    private int postId;
    private int threadId;
    private String author;
    private String message;
    private String threadTitle;

    public Post(int postId, int threadId, String author, String message, String threadTitle) {
        this.postId = postId;
        this.threadId = threadId;
        this.author = author;
        this.message = message;
        this.threadTitle = threadTitle;
    }

    public int getPostId() {
        return postId;
    }

    public int getThreadId() {
        return threadId;
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

    public String getThreadTitle() {
        return threadTitle;
    }

    public String getUrl() {
        return Properties.forumurl + "threads/" + getThreadId() + "/post-" + getPostId();
    }
}
