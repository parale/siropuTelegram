package siropuTelegram.XenForo;

public class Post {
    private int post_id;
    private int thread_id;
    private String author;
    private String message;

    public Post(int post_id, int thread_id, String author, String message) {
        this.post_id = post_id;
        this.thread_id = thread_id;
        this.author = author;
        this.message = message;
    }

    public int getPost_id() {
        return post_id;
    }

    public int getThread_id() {
        return thread_id;
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }
}
