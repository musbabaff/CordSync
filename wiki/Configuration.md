# ⚙️ Configuration & Features

CordSync offers extensive configuration to tailor the plugin precisely to your server's needs.

## Premium Modules Overview

### 🔐 2FA Login Protection
Found in `config.yml` under `2fa-login`. When players join the server, their connection will be halted, and a Direct Message (DM) will be sent to their linked Discord account with a verification embed. If they press "Approve", they log in immediately. If "Deny" is pressed, the server kicks them.

### 🎮 In-Game GUI Menu
Type `/link` or `/hesapesle` to open a beautiful, chest-based graphical interface. Players can see their Discord ID, linked status, available rewards, and link/unlink directly from the UI.

### 🖥️ Console Bridge
Found under `console-bridge`. Input a Discord channel ID where the server console will be mirrored in real-time. Admins can type commands like `say Hello` or `kick Player` directly into the Discord channel, and they execute securely on the Minecraft server. You can protect this by providing an `admin-role-id` and a `blocked-commands` list.

### 🌐 Webhook Chat Bridge
In the `chat-bridge` section, you can input a **Discord Webhook URL**. Once configured, whenever a player chats in-game, the Webhook will send the message to Discord, automatically replacing the avatar with the player's **Minecraft Skin Head** and their name! 

### ⬇️ Join/Quit Embeds
In the `discord.join-quit-messages` section, linked players will trigger rich Discord embeds in a selected channel whenever they join or leave the server. These embeds show their current Discord Roles, Minecraft Skins as thumbnails, and current online player count.

### 🔄 Reverse Sync (Discord -> LuckPerms)
Usually, CordSync gives Discord roles when a player links their Minecraft account. **Reverse Sync** does the opposite! In the `reverse-sync` section, you can map Discord Role IDs to LuckPerms group names.
- Example: If a linked user receives the "Nitro Booster" role on Discord, CordSync will instantly grant them the `booster` rank on the Minecraft server via LuckPerms!

---

## 🌎 Multi-Language Locales
Every single piece of text, including GUI titles, lores, and Discord Bot embeds, can be translated and modified via the `plugins/CordSync/locales/` folder.

To switch languages:
1. Open `config.yml`.
2. Change the `language` key (e.g., `en`, `tr`, `de`, `es`, `fr`).
3. Reload the plugin or restart the server.
*If a key is accidentally deleted, the plugin will safely fall back to `en.yml` to prevent errors!*
