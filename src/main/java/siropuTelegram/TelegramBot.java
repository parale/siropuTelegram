package siropuTelegram;

import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.File;
import org.telegram.telegrambots.api.objects.PhotoSize;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.stickers.Sticker;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import siropuTelegram.XenForo.XenForo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class TelegramBot extends TelegramLongPollingBot {
    private Logger LOGGER = Solution.LOGGER;
    private ChatUpdater chatUpdater;

    public TelegramBot() {
        LOGGER.log(Level.INFO, "Telegram bot started.");
        chatUpdater = new ChatUpdater(this);
        chatUpdater.start();
    }

    public synchronized void replyTo(long chat_id, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chat_id);
        message.setText(cutBbTag(text));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logException(e);
        }
    }

    public void onUpdateReceived(Update update) {
        if (!chatUpdater.isAlive()) {
            chatUpdater.start();
        }

        XenForo forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);

        User user = new User();
        user.setTelegramUserName(update.getMessage().getChat().getUserName());

        if (forum.isUserActive(user.getTelegramUserName())) {
            user.setXfUserId(forum.getUserIdByTelegram(user.getTelegramUserName()));
            // photos
            if (update.hasMessage() && update.getMessage().hasPhoto()) {
                photoMessage(update, user);
            }

            // stickers
            Sticker sticker;
            if ((sticker = update.getMessage().getSticker()) != null) {
                stickerMessage(sticker, user);
            }
        }

        // text messages
        if (update.hasMessage() && update.getMessage().hasText()) {
            if (update.getMessage().getText().contentEquals("/start")) {
                startMessage(update, user);
            } else if (update.getMessage().getText().contentEquals("/stop")) {
                stopMessage(update, user);
            } else if (!update.getMessage().getText().isEmpty() && forum.isUserActive(user.getTelegramUserName())) {
                textMessage(update, user);
            }
        }

        forum.close();
    }

    private String prepareMessage(String message) {
        return convertLinks(message);
    }

    private void textMessage(Update update, User user) {
        XenForo forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);

        if (update.getMessage().isReply()) {
            forum.sendMessage(user.getXfUserId(), prepareMessage("[i]\"" + update.getMessage().getReplyToMessage().getText() + "\"[/i] "));
        }

        forum.sendMessage(user.getXfUserId(), prepareMessage(update.getMessage().getText()));

        forum.close();
    }

    private void startMessage(Update update, User user) {
        XenForo forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);
        if (!forum.isUserActive(user.getTelegramUserName()) && forum.getUserIdByTelegram(user.getTelegramUserName()) > 0) {
            if (forum.createUser(user.getXfUserId(), user.getTelegramUserName(), update.getMessage().getChatId())) {
                SendMessage reply = new SendMessage();
                reply.setChatId(update.getMessage().getChatId());
                reply.setText(Properties.res.getString("subscribe"));
                try {
                    execute(reply);
                } catch (TelegramApiException e) {
                    logException(e);
                }
            }
        }

        forum.close();
    }

    private void stopMessage(Update update, User user) {
        XenForo forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);
        if (forum.isUserActive(user.getTelegramUserName())) {
            if (forum.deleteUser(user.getTelegramUserName())) {
                SendMessage reply = new SendMessage();
                reply.setChatId(update.getMessage().getChatId());
                reply.setText(Properties.res.getString("unsubscribe"));
                try {
                    execute(reply);
                } catch (TelegramApiException e) {
                    logException(e);
                }
            }
        }

        forum.close();
    }

    private void photoMessage(Update update, User user) {
        PhotoSize photoSize = update.getMessage().getPhoto().stream()
                .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                .findFirst()
                .orElse(null);

        if (photoSize != null) {
            String filePath = photoSize.getFilePath();

            if (filePath == null) {
                GetFile getFileMethod = new GetFile();
                getFileMethod.setFileId(photoSize.getFileId());
                try {
                    File file = execute(getFileMethod);
                    filePath = file.getFilePath();
                } catch (TelegramApiException e) {
                    logException(e);
                }

            }

            if (filePath != null) {
                try {
                    String fileName = filePath.split("/")[1];
                    int timestamp = (int) (System.currentTimeMillis() / 1000L);
                    URL website = new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath);
                    ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                    fileName = timestamp + fileName;
                    FileOutputStream fos = new FileOutputStream(Properties.saveto + fileName);
                    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

                    XenForo forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);
                    try {
                        String caption = update.getMessage().getCaption();
                        if (caption.length() > 0)
                            forum.sendMessage(user.getXfUserId(), update.getMessage().getCaption());
                    } catch (NullPointerException e) {

                    }

                    forum.sendMessage(user.getXfUserId(), String.format("[url]%s%s[/url]", Properties.mediaurl, fileName));
                    SendMessage reply = new SendMessage();
                    reply.setChatId(update.getMessage().getChatId());
                    reply.setText(Properties.res.getString("photoSuccess"));
                    try {
                        execute(reply);
                    } catch (TelegramApiException e) {
                        logException(e);
                    }
                } catch (FileNotFoundException e) {
                    photoFail(update);
                    logException(e);
                } catch (MalformedURLException e) {
                    photoFail(update);
                    logException(e);
                } catch (IOException e) {
                    photoFail(update);
                    logException(e);
                }
            } else {
                photoFail(update);
            }
        }
    }

    private void photoFail(Update update) {
        SendMessage reply = new SendMessage();
        reply.setChatId(update.getMessage().getChatId());
        reply.setText(Properties.res.getString("photoFail"));
        try {
            execute(reply);
        } catch (TelegramApiException e) {
            logException(e);
        }
    }

    private void stickerMessage(Sticker sticker, User user) {
        GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(sticker.getFileId());
        try {
            File file = execute(getFileMethod);
            String filePath = file.getFilePath();
            String originalFileName = filePath.split("/")[1];
            int timestamp = (int) (System.currentTimeMillis() / 1000L);
            URL website = new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            String fullFileName = timestamp + originalFileName;
            FileOutputStream fos = new FileOutputStream(Properties.saveto + "stickers/" + fullFileName);
            Runtime.getRuntime().exec(String.format("%s -i %s %s", Properties.ffmpeg, Properties.saveto + "stickers/" + fullFileName, Properties.saveto + "stickers/" + originalFileName + ".png"));
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            XenForo forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);
            forum.sendMessage(user.getXfUserId(), String.format("[sticker]%sstickers/%s[/sticker]", Properties.mediaurl, originalFileName + ".png"));
            forum.close();
        } catch (TelegramApiException e) {
            logException(e);
        } catch (FileNotFoundException e) {
            logException(e);
        } catch (MalformedURLException e) {
            logException(e);
        } catch (IOException e) {
            logException(e);
        }
    }

    private String convertLinks(String message) {
        if (message.toLowerCase().contains("http")) {
            return message.replaceAll("(http|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?", "[url]$1://$2$3[/url]");
        }
        return message;
    }

    private String cutBbTag(String s) {
        if (s.toLowerCase().contains("[url]") && s.toLowerCase().contains("[/url]")) {
            s = s.replaceAll("(?i)" + Pattern.quote("[url]"), "");
            s = s.replaceAll("(?i)" + Pattern.quote("[/url]"), "");
        }

        if (s.toLowerCase().contains("[sticker]") && s.toLowerCase().contains("[/sticker]")) {
            s = s.replaceAll("(?i)" + Pattern.quote("[sticker]"), "");
            s = s.replaceAll("(?i)" + Pattern.quote("[/sticker]"), "");
        }

        if (s.toLowerCase().contains("[i]") && s.toLowerCase().contains("[/i]")) {
            s = s.replaceAll("(?i)" + Pattern.quote("[i]"), "");
            s = s.replaceAll("(?i)" + Pattern.quote("[/i]"), "");
        }

        if (s.toLowerCase().contains("[b]") && s.toLowerCase().contains("[/b]")) {
            s = s.replaceAll("(?i)" + Pattern.quote("[b]"), "");
            s = s.replaceAll("(?i)" + Pattern.quote("[/b]"), "");
        }

        if (s.toLowerCase().contains("[media=youtube]") && s.toLowerCase().contains("[/media]")) {
            s = s.replaceAll("(?i)\\[media=youtube\\](id=)?([A-Za-z0-9]+);?(.*?)\\[/media\\]", "https://www.youtube.com/watch?v=$2");
        }


        return s;
    }

    @Override
    public String getBotUsername() {
        return Properties.bot_username;
    }

    @Override
    public String getBotToken() {
        return Properties.bot_token;
    }

    private void logException(Exception e) {
        LOGGER.log(Level.SEVERE, e.toString(), e);
    }
}