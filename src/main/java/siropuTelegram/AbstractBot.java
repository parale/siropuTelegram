package siropuTelegram;

import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.util.regex.Pattern;

abstract class AbstractBot extends TelegramLongPollingBot {
    public void onUpdateReceived(Update update) {

    }

    String cutTags(String s) {
        if (s.toLowerCase().contains("[/url]")) {
            if (s.toLowerCase().contains("[/url]")) {
                s = s.replaceAll(
                        "(?i)\\[url(=(.*?))?\\](.*?)\\[/url\\]",
                        "$2 $3"
                );
            }
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

        if (s.toLowerCase().contains("[/attach]")) {
            if (s.toLowerCase().contains("[/attach]")) {
                s = s.replaceAll(
                        "(?i)\\[attach(.*?)?\\](.*?)\\[/attach\\]",
                        Properties.forumurl + "attachments/$2/"
                );
            }
        }

        if (s.toLowerCase().contains("[media=youtube]") && s.toLowerCase().contains("[/media]")) {
            s = s.replaceAll(
                    "(?i)\\[media=youtube\\](id=)?([A-Za-z0-9]+);?(.*?)\\[/media\\]",
                    "https://www.youtube.com/watch?v=$2"
            );
        }

        if (s.toLowerCase().contains("[/quote]")) {
            s = s.replaceAll("(?i)\\[quote(=\"(.*?),(.*))\\]((.|\\s)*?)\\[/quote\\]", "<i>" + Properties.res.getString("quoteOf") + " $2: «$4»</i>");
            s = s.replaceAll("(?i)\\[quote\\]((.|\\s)*?)\\[/quote\\]", "<i>" + Properties.res.getString("quote") + ": «$1»</i>");
        }

        s = s.replaceAll("(?i)(\\[(\\/?)(.*?\\]))", "");

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
}
