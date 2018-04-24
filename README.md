# siropuTelegram
Transport between Telegram and Siropu Shoutbox for XenForo 2.

## How to use
### Prepare your XenForo installation
To connect XenForo and Telegram accounts, create custom user field in XenForo Admin Control Panel.

* Field ID: telegram
* Field type: Single-line text box
* Value match requirements: A-Z, 0-9, and _ only

Other fields may be customized as you wish.

## Prepare public directories
Create two directories to be visible from web.

* media/
* media/stickers/

## Other
* ffmpeg is required to convert webp stickers to png files.