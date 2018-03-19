# siropuTelegram
Transport between Telegram and Siropu Shoutbox for siropuTelegram.XenForo.siropuTelegram.XenForo 2.

## How to use
### Prepare your siropuTelegram.XenForo.siropuTelegram.XenForo installation
To connect siropuTelegram.XenForo.siropuTelegram.XenForo and Telegram accounts, create custom user field in siropuTelegram.XenForo.siropuTelegram.XenForo Admin Control Panel.

* Field ID: telegram
* Field type: Single-line text box
* Value match requirements: A-Z, 0-9, and _ only

Other fields may be customized as you wish.

### First bot launch
You'll be asked for several properties:
* XF installation database host, username and password.
* XF database prefix ("xf_" by default).
* Set table names required to store bot's data. Example: transport_settings and transport_users.
* Telegram bot token & username.
* Full path to a media folder, that'll be visible from web (required for photos and stickers). Example: /home/forum/www/media/
* URL to the media folder, that'll be posted to a forum. Example: https://hostname/media/
* Ffmpeg binary path. It's used to convert webp stickers to png images, visible in all browsers. Example: ffmpeg.

After successful launch, you can type "/start" to your bot in Telegram and chat with friends on forum via Telegram.

### Additional properties
dev - if dev = 1, all messages from Telegram will be printed out and not inserted into the database.

lang - bot language.