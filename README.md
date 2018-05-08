# siropuTelegram
Transport between Telegram and Siropu Shoutbox for XenForo 2.

## Get ready
### Prepare your XenForo installation
To connect XenForo and Telegram accounts, create custom user field in XenForo Admin Control Panel.

* Field ID: telegram
* Field type: Single-line text box
* Value match requirements: A-Z, 0-9, and _ only

Other fields may be customized as you wish.

### Prepare public directories
Create two directories to be visible from web.

* media/
* media/stickers/

### Other
* ffmpeg is required to convert webp stickers to png files.

## Features
### Thread following
Users can subscribe threads to receive notifications when someone makes a new post.

`/follow https://forum.com/threads/thread-title.2477/`

Result:

> 🆕 New peply in thread «Thread to follow», link: https://forum.com/threads/2478/post-310520

### New threads announcment
Bot notifies its users when a new thread appears.

> aleksei created a new thread "Bla bla thread": https://forum.com/threads/2478

### Bot admin
You can set bot admins in bot.properties file.

`admins = aleksei_ee,other_telegram_user`

Admin can change properties in real-time using `/set key value` command.

```
/set lang ru
```

Or see current config using `/set` without arguments. Bot's reply:

```
settings_table = stchat_settings
bot_token = XXX
mediaurl = https://forum.com/media/
db_password = dbpassword
dev = 0
db_host = forum.com:3306/forumdb
logging = 0
bot_username = forumbot
saveto = /home/forum/www/media/
follow_table = stchat_follow
db_user = forumdbuser
exclude_nodes = 10
xf_prefix = xf_
ffmpeg = ffmpeg
admins = admin_username
forumurl = https://forum.com/
users_table = stchat_user
lang = ru
```
