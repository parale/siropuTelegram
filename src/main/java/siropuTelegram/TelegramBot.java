package siropuTelegram;

import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.File;
import org.telegram.telegrambots.api.objects.PhotoSize;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.stickers.Sticker;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import siropuTelegram.XenForo.Post;
import siropuTelegram.XenForo.Thread;
import siropuTelegram.XenForo.XenForo;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TelegramBot extends AbstractBot {
    private ChatUpdater chatUpdater;
    private ThreadUpdater threadUpdater;

    TelegramBot() {
        Logger.logInfo("Telegram bot started.");
        chatUpdater = new ChatUpdater(this);
        chatUpdater.start();

        threadUpdater = new ThreadUpdater(this);
        threadUpdater.start();
    }

    synchronized void replyTo(long chat_id, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chat_id);

        String message = cutTags(text);
        try {
            if (message.getBytes(StandardCharsets.UTF_8).length < 4096) {
                sendMessage.setText(message);
                execute(sendMessage);
            }
        } catch (TelegramApiException e) {
            Logger.logException(e);
        }
    }

    public void onUpdateReceived(Update update) {
        if (!chatUpdater.isAlive()) {
            chatUpdater = new ChatUpdater(this);
            chatUpdater.start();
        }

        if (!threadUpdater.isAlive()) {
            threadUpdater = new ThreadUpdater(this);
            threadUpdater.start();
        }

        XenForo forum = new XenForo();

        User user = new User();
        try {
            user.setTelegramUserName(update.getMessage().getChat().getUserName());
        } catch (NullPointerException e) {
            return;
        }

        user.setTelegramChatId(update.getMessage().getChatId());

        if (forum.isUserActive(user.getTelegramUserName())) {
            user.setXfUserId(forum.getActiveUserId(user.getTelegramUserName()));

            if (user.getXfUserId() > 0) {
                if (update.hasMessage() && update.getMessage().hasPhoto()) {
                    photoMessage(update, user);
                }

                Sticker sticker;
                if ((sticker = update.getMessage().getSticker()) != null) {
                    stickerMessage(sticker, user);
                }
            }
        }

        if (update.hasMessage() && update.getMessage().hasText()) {
            if (update.getMessage().getText().startsWith("/start")) {
                startMessage(update, user);
            } else if (update.getMessage().getText().startsWith("/stop")) {
                stopMessage(update, user);
            } else if (!update.getMessage().getText().isEmpty() && forum.isUserActive(user.getTelegramUserName())) {
                if (update.getMessage().getText().startsWith("/new")) {
                    whatsNew(update, user);
                } else if (update.getMessage().getText().startsWith("/follow")) {
                    try {
                        follow(update, user);
                    } catch (NumberFormatException e) {
                        replyTo(user.getTelegramChatId(), Properties.res.getString("unknownError"));
                    }
                } else if (update.getMessage().getText().startsWith("/unfollow")) {
                    unfollow(update, user);
                } else {
                    textMessage(update, user);
                }
            }
        }

        forum.close();
    }

    private String prepareMessage(String message) {
        return convertLinks(message);
    }

    private void textMessage(Update update, User user) {
        XenForo forum = new XenForo();

        if (update.getMessage().isReply()) {
            forum.sendMessage(user.getXfUserId(), prepareMessage("[i]\"" + update.getMessage().getReplyToMessage().getText() + "\"[/i] "));
        }

        forum.sendMessage(user.getXfUserId(), prepareMessage(update.getMessage().getText()));

        forum.close();
    }

    private void startMessage(Update update, User user) {
        XenForo forum = new XenForo();
        user.setXfUserId(forum.getUserIdByTelegram(user.getTelegramUserName()));
        if (!forum.isUserActive(user.getTelegramUserName())) {
            SendMessage reply = new SendMessage();
            reply.setChatId(update.getMessage().getChatId());

            if (user.getXfUserId() > 0) {
                if (forum.createUser(user.getXfUserId(), user.getTelegramUserName(), update.getMessage().getChatId())) {
                    reply.setText(Properties.res.getString("subscribe"));
                } else {
                    reply.setText(Properties.res.getString("unknownError"));
                }
            } else if (user.getXfUserId() == -1) {
                reply.setText(Properties.res.getString("doubleId"));
            } else {
                reply.setText(Properties.res.getString("unknownError"));
            }

            try {
                execute(reply);
            } catch (TelegramApiException e) {
                Logger.logException(e);
            }
        }

        forum.close();
    }

    private void stopMessage(Update update, User user) {
        XenForo forum = new XenForo();
        if (forum.isUserActive(user.getTelegramUserName())) {
            if (forum.deleteUser(user.getTelegramUserName())) {
                SendMessage reply = new SendMessage();
                reply.setChatId(update.getMessage().getChatId());
                reply.setText(Properties.res.getString("unsubscribe"));
                try {
                    execute(reply);
                } catch (TelegramApiException e) {
                    Logger.logException(e);
                }
            }
        }

        forum.close();
    }

    private void photoMessage(Update update, User user) {
        PhotoSize photoSize = update.getMessage().getPhoto().stream().max(Comparator.comparing(PhotoSize::getFileSize))
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
                    Logger.logException(e);
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

                    XenForo forum = new XenForo();
                    try {
                        String caption = update.getMessage().getCaption();
                        if (caption.length() > 0)
                            forum.sendMessage(user.getXfUserId(), update.getMessage().getCaption());
                    } catch (NullPointerException ignored) {
                    }

                    forum.sendMessage(user.getXfUserId(), String.format("[url]%s%s[/url]", Properties.mediaurl, fileName));
                    SendMessage reply = new SendMessage();
                    reply.setChatId(update.getMessage().getChatId());
                    reply.setText(Properties.res.getString("photoSuccess"));
                    try {
                        execute(reply);
                    } catch (TelegramApiException e) {
                        Logger.logException(e);
                    }
                } catch (IOException e) {
                    photoFail(update);
                    Logger.logException(e);
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
            Logger.logException(e);
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
            Runtime.getRuntime().exec(String.format(
                    "%s -i %s %s",
                    Properties.ffmpeg,
                    Properties.saveto + "stickers/" + fullFileName,
                    Properties.saveto + "stickers/" + originalFileName + ".png")
            );
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

            XenForo forum = new XenForo();
            forum.sendMessage(
                    user.getXfUserId(),
                    String.format("[sticker]%sstickers/%s[/sticker]", Properties.mediaurl, originalFileName + ".png")
            );
            forum.close();
        } catch (TelegramApiException | IOException e) {
            Logger.logException(e);
        }
    }

    private void whatsNew(Update update, User user) {
        XenForo forum = new XenForo();
        ArrayList<Post> posts = forum.whatsNew(user);
        forum.close();

        if (posts.size() > 0) {
            replyTo(update.getMessage().getChatId(), Properties.res.getString("newPosts"));

            for (Post post : posts) {
                String message =
                        "=====\n" +
                                Properties.res.getString("thread") + ": " + post.getThreadTitle() + "\n" +
                                Properties.res.getString("author") + ": " + post.getAuthor() + "\n" +
                                Properties.res.getString("link") + ": " + post.getUrl() + "\n\n" +
                                post.getMessage() +
                                "\n\n";

                replyTo(update.getMessage().getChatId(), cutTags(message));
            }
        } else {
            replyTo(update.getMessage().getChatId(), Properties.res.getString("noNewPosts"));
        }
    }

    private void follow(Update update, User user) throws NumberFormatException {
        String[] args = update.getMessage().getText().split(" ");
        if (args.length > 1) {
            Matcher matcher = Pattern.compile("\\.([0-9]*)/").matcher(args[1]);

            if (matcher.find()) {
                int threadId = Integer.valueOf(matcher.group(1));

                XenForo forum = new XenForo();
                Thread thread = new Thread(threadId);

                if (forum.isThreadExists(thread)) {
                    if (forum.followThread(user, thread)) {
                        replyTo(user.getTelegramChatId(), Properties.res.getString("followSuccess"));
                    } else {
                        replyTo(user.getTelegramChatId(), Properties.res.getString("followFailed"));
                    }
                } else {
                    replyTo(user.getTelegramChatId(), Properties.res.getString("followFailed"));
                }

                forum.close();
            } else {
                replyTo(user.getTelegramChatId(), Properties.res.getString("followFailed"));
            }
        } else {
            replyTo(update.getMessage().getChatId(),
                    Properties.res.getString("followHelp")
            );
        }
    }

    private void unfollow(Update update, User user) {
        String[] args = update.getMessage().getText().split(" ");
        if (args.length > 1) {
            Matcher matcher = Pattern.compile("\\.([0-9]*)/").matcher(args[1]);

            if (matcher.find()) {
                int threadId = Integer.valueOf(matcher.group(1));

                XenForo forum = new XenForo();
                Thread thread = new Thread(threadId);

                if (forum.isThreadExists(thread)) {
                    if (forum.unfollowThread(user, thread)) {
                        replyTo(user.getTelegramChatId(), Properties.res.getString("unfollowSuccess"));
                    } else {
                        replyTo(user.getTelegramChatId(), Properties.res.getString("unfollowFailed"));
                    }
                } else {
                    replyTo(user.getTelegramChatId(), Properties.res.getString("unfollowFailed"));
                }

                forum.close();
            } else {
                replyTo(user.getTelegramChatId(), Properties.res.getString("unfollowFailed"));
            }
        } else {
            replyTo(update.getMessage().getChatId(),
                    Properties.res.getString("unfollowHelp")
            );
        }
    }

    private String convertLinks(String message) {
        if (message.toLowerCase().contains("http")) {
            return message.replaceAll(
                    "(http|https)://([\\w_-]+(?:(?:\\.[\\w_-]+)+))([\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?",
                    "[url]$1://$2$3[/url]"
            );
        }

        return message;
    }
}
