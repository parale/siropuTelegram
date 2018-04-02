package siropuTelegram.XenForo;

import siropuTelegram.Properties;

public class Thread {
    private int id;
    private String title;
    private String author;

    public Thread(int id, String title, String author) {
        this.id = id;
        this.title = title;
        this.author = author;
    }

    private int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getUrl() {
        return String.format("%sthreads/%d", Properties.forumurl, getId());
    }
}
