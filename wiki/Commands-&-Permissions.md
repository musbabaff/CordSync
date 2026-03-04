# 💬 Commands & Permissions

CordSync keeps commands simple to use while offering powerful administrative features through the Discord Bot.

## 🕹️ Minecraft Commands

| Command | Aliases | Description | Permission |
|---------|---------|-------------|------------|
| `/link` | `/hesapesle`, `/mlink` | Generates a 6-digit connection code, or opens the GUI. | `cordsync.use` |
| `/unlink` | | Removes the player's Discord link safely. | `cordsync.use` |

*By default, all players have the `cordsync.use` permission.*

### Admin / VIP Permissions
| Permission Node | Description |
|-----------------|-------------|
| `cordsync.chatbridge` | Allows a player's messages to be sent via the Webhook Chat Bridge to Discord. Give this to VIPs and staff to make them stand out! |

---

## 🤖 Discord Slash Commands

| Command | Usage | Description |
|---------|-------|-------------|
| `/link [code]` | `/link 123456` | Verifies a Minecraft linking code and links accounts. |
| `/unlink` | `/unlink` | Removes the Discord connection from the Minecraft account. |

*These commands can be used within any channel where the bot has permission, but they are completely hidden from other users using **Ephemeral Messages**!*

---

## 📦 PlaceholderAPI (PAPI) Placeholders

Use these in menus, scoreboards, and tablists! You must have PlaceholderAPI installed to use these.

| Placeholder | Value / Output |
|-------------|----------------|
| `%cordsync_is_linked%` | `true` or `false` |
| `%cordsync_discord_name%` | Player's Discord Username (e.g. `musbabaff`) |
| `%cordsync_discord_tag%` | Player's Server Display Name |
| `%cordsync_discord_id%` | Player's 18-digit Discord ID |
| `%cordsync_discord_avatar%` | URL to the player's Discord Avatar |
| `%cordsync_discord_role%` | Their Highest Discord Role Name |
| `%cordsync_linked_count%` | Total number of linked accounts stored in DB |
| `%cordsync_online_linked%` | Current amount of linked players online in server |
