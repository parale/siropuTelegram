package siropuTelegram;

public class User {
    private String telegramUserName;
    private long telegramChatId;
    private int xfUserId;

    public User() {
    }

    public User(int xfUserId, long telegramChatId) {
        this.xfUserId = xfUserId;
        this.telegramChatId = telegramChatId;
    }

    public long getTelegramChatId() {
        return telegramChatId;
    }

    public void setTelegramChatId(long telegramChatId) {
        this.telegramChatId = telegramChatId;
    }

    public int getXfUserId() {
        return xfUserId;
    }

    public void setXfUserId(int xfUserId) {
        this.xfUserId = xfUserId;
    }

    public String getTelegramUserName() {
        return telegramUserName;
    }

    public void setTelegramUserName(String telegramUserName) {
        this.telegramUserName = telegramUserName;
    }
}
