package siropuTelegram.XenForo;

public class ChatMessage {

    private String authorName;
    private String message;
    private int authorId;

    ChatMessage(String message, String authorName, int authorId) {
        this.authorName = authorName;
        this.message = message;
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
