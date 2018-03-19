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

    public String getAuthorName() {
        return authorName;
    }

    public String getMessage() {
        return message;
    }

    public int getAuthorId() {
        return authorId;
    }
}
