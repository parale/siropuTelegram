import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.File;
import org.telegram.telegrambots.api.objects.PhotoSize;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.stickers.Sticker;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class TelegramBot extends TelegramLongPollingBot {
    private Logger LOGGER = Solution.LOGGER;
    private ChatUpdater chatUpdater;
    private ResourceBundle res = ResourceBundle.getBundle("locale." + Properties.lang);

    public synchronized void replyTo(long chat_id, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chat_id);
        message.setText(cutBbTag(text));
        try {
            execute(message);
        } catch (TelegramApiException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
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

    public TelegramBot() {
        LOGGER.log(Level.INFO, "Telegram bot started.");
        chatUpdater = new ChatUpdater(this);
        chatUpdater.start();
    }

    public void onUpdateReceived(Update update) {
        if (!chatUpdater.isAlive()) {
            chatUpdater.start();
        }

        // text messages
        if (update.hasMessage() && update.getMessage().hasText()) {
            textMessage(update);
        }
        // photos
        if (update.hasMessage() && update.getMessage().hasPhoto()) {
            photoMessage(update);
        }
        // other
        if (update.getMessage().getSticker() != null) {
            stickerMessage(update);
        }
    }

    private void stickerMessage(Update update) {
        String telegram_user_id = update.getMessage().getChat().getUserName();
        XenForo forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);
        if (forum.isUserActive(telegram_user_id)) {
            // stickers
            Sticker sticker = update.getMessage().getSticker();
            if (sticker != null) {
                int xf_user_id = forum.getUserIdByTelegram(telegram_user_id);
                stickerMessage(xf_user_id, sticker);
            }
        }

        forum.close();
    }

    private void textMessage(Update update) {
        String message = update.getMessage().getText();
        if (message.contentEquals("/start")) {
            startMessage(update);
        } else if (message.contentEquals("/stop")) {
            stopMessage(update);
        } else if (!message.isEmpty()) {
            String telegram_user_id = update.getMessage().getChat().getUserName();
            XenForo forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);
            if (forum.isUserActive(telegram_user_id)) {
                int xf_user_id = forum.getUserIdByTelegram(telegram_user_id);
                if (update.getMessage().isReply()) {
                    forum.sendMessage(xf_user_id, prepareMessage("[i]\"" + update.getMessage().getReplyToMessage().getText() + "\"[/i] "));
                }
                forum.sendMessage(xf_user_id, prepareMessage(message));
            }

            forum.close();
        }
    }

    private String prepareMessage(String message) {
        return convertLinks(message);
    }

    private String convertLinks(String message) {
        if (message.toLowerCase().contains("http")) {
            return message.replaceAll("(http|ftp|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?", "[url]$1://$2$3[/url]");
        }
        return message;
    }

    public String getBotUsername() {
        return Properties.bot_username;
    }

    @Override
    public String getBotToken() {
        return Properties.bot_token;
    }

    private void startMessage(Update update) {
        String telegram_user_id = update.getMessage().getChat().getUserName();
        long chat_id = update.getMessage().getChatId();
        XenForo forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);
        int xf_user_id = forum.getUserIdByTelegram(telegram_user_id);
        if (!forum.isUserActive(telegram_user_id) && xf_user_id > 0) {
            if (forum.createUser(xf_user_id, telegram_user_id, chat_id)) {
                SendMessage reply = new SendMessage();
                reply.setChatId(update.getMessage().getChatId());
                reply.setText(res.getString("subscribe"));
                try {
                    execute(reply);
                } catch (TelegramApiException e) {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                }
            }
        }

        forum.close();
    }

    private void stopMessage(Update update) {
        String telegram_user_id = update.getMessage().getChat().getUserName();
        XenForo forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);
        if (forum.isUserActive(telegram_user_id)) {
            if (forum.deleteUser(telegram_user_id)) {
                SendMessage reply = new SendMessage();
                reply.setChatId(update.getMessage().getChatId());
                reply.setText(res.getString("unsubscribe"));
                try {
                    execute(reply);
                } catch (TelegramApiException e) {
                    LOGGER.log(Level.SEVERE, e.toString(), e);
                }
            }
        }

        forum.close();
    }

    private void photoMessage(Update update) {
        String telegram_user_id = update.getMessage().getChat().getUserName();
        XenForo forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);
        if (forum.isUserActive(telegram_user_id)) {
            int xf_user_id = forum.getUserIdByTelegram(telegram_user_id);
            PhotoSize photoSize = getPhoto(update);
            if (photoSize != null) {
                String filePath = getFilePath(photoSize);
                if (filePath != null) {
                    String file = downloadPhotoByFilePath(filePath);
                    if (file != null) {
                        String caption;
                        try {
                            if ((caption = update.getMessage().getCaption()).length() > 0) {
                                forum.sendMessage(xf_user_id, caption);
                            }
                        } catch (NullPointerException e) {

                        }

                        forum.sendMessage(xf_user_id, String.format("[url]%s%s[/url]", Properties.mediaurl, file));
                        SendMessage reply = new SendMessage();
                        reply.setChatId(update.getMessage().getChatId());
                        reply.setText(res.getString("photoSuccess"));
                        try {
                            execute(reply);
                        } catch (TelegramApiException e) {
                            LOGGER.log(Level.SEVERE, e.toString(), e);
                        }
                    } else {
                        SendMessage reply = new SendMessage();
                        reply.setChatId(update.getMessage().getChatId());
                        reply.setText(res.getString("photoFail"));
                        try {
                            execute(reply);
                        } catch (TelegramApiException e) {
                            LOGGER.log(Level.SEVERE, e.toString(), e);
                        }
                    }
                }
            }
        }

        forum.close();
    }

    private void stickerMessage(int xf_user_id, Sticker sticker) {
        GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(sticker.getFileId());
        try {
            File file = execute(getFileMethod);
            String filePath = file.getFilePath();
            String fileName = filePath.split("/")[1];
            int timestamp = (int) (System.currentTimeMillis() / 1000L);
            URL website = new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            String fileNameStamp = timestamp + fileName;
            FileOutputStream fos = new FileOutputStream(Properties.saveto + "stickers/" + fileNameStamp);
            Runtime.getRuntime().exec(String.format("%s -i %s %s", Properties.ffmpeg, Properties.saveto + "stickers/" + fileNameStamp, Properties.saveto + "stickers/" + fileName + ".png"));
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            XenForo forum = new XenForo(Properties.db_host, Properties.db_user, Properties.db_password);
            forum.sendMessage(xf_user_id, String.format("[sticker]%sstickers/%s[/sticker]", Properties.mediaurl, fileName + ".png"));
            forum.close();
        } catch (TelegramApiException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        } catch (MalformedURLException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }
    }

    private PhotoSize getPhoto(Update update) {
        // Check that the update contains a message and the message has a photo
        if (update.hasMessage() && update.getMessage().hasPhoto()) {
            // When receiving a photo, you usually get different sizes of it
            List<PhotoSize> photos = update.getMessage().getPhoto();

            // We fetch the bigger photo
            return photos.stream()
                    .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                    .findFirst()
                    .orElse(null);
        }

        // Return null if not found
        return null;
    }

    private String getFilePath(PhotoSize photo) {
        Objects.requireNonNull(photo);

        if (photo.hasFilePath()) { // If the file_path is already present, we are done!
            return photo.getFilePath();
        } else { // If not, let find it
            // We create a GetFile method and set the file_id from the photo
            GetFile getFileMethod = new GetFile();
            getFileMethod.setFileId(photo.getFileId());
            try {
                // We execute the method using AbsSender::execute method.
                File file = execute(getFileMethod);
                // We now have the file_path
                return file.getFilePath();
            } catch (TelegramApiException e) {
                LOGGER.log(Level.SEVERE, e.toString(), e);
            }
        }

        return null;
    }

    private String downloadPhotoByFilePath(String filePath) {
        try {
            String fileName = filePath.split("/")[1];
            int timestamp = (int) (System.currentTimeMillis() / 1000L);
            URL website = new URL("https://api.telegram.org/file/bot" + getBotToken() + "/" + filePath);
            ReadableByteChannel rbc = Channels.newChannel(website.openStream());
            fileName = timestamp + fileName;
            FileOutputStream fos = new FileOutputStream(Properties.saveto + fileName);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            return fileName;
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        } catch (MalformedURLException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.toString(), e);
        }

        return null;
    }
}
