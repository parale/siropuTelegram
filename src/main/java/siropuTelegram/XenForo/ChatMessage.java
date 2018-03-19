package siropuTelegram.XenForo;

public class ChatMessage {
    private int id;
    private String authorName;
    private String message;
    private int date;
    private int authorId;

    public ChatMessage(int id, String message, String authorName, int authorId, int date) {
        this.id = id;
        this.authorName = authorName;
        this.message = message;
        this.date = date;
        this.authorId = authorId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public int getAuthorId() {
        return authorId;
    }

    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }
}
