# Discord ↔ Minecraft Link Gate (Paper 1.21.x)

This project has two parts that work together:

1. A **Paper plugin** that blocks movement until a player links.
2. A **Discord bot** with slash commands (`/link` and `/unlink`).

The plugin and bot share the same SQLite database file so links persist.

## 1) What this does

1. Player joins Minecraft.
2. If not linked yet, player gets a 6-character code and cannot move.
3. Player runs `/link <code>` in Discord.
4. Bot validates the code and saves the Discord↔Minecraft link.
5. Plugin detects link and allows movement.
6. Next login, player stays linked (no relogin needed).

---

## 2) Prerequisites

- Java 21 (for Paper 1.21.x).
- A Paper 1.21.x server.
- Python 3.10+ (for the Discord bot).
- A Discord bot token from Discord Developer Portal.
- Your bot invited to your server with `applications.commands` scope.

---

## 3) Build the plugin jar

From this repository root:

```bash
mvn -DskipTests package
```

Output jar will be in `target/`.

> If Maven download is blocked in your environment, build on your local machine/VPS with normal internet access.

---

## 4) Install on your Minecraft server

1. Copy the plugin jar to your Paper server `plugins/` folder.
2. Start the server once.
3. Confirm plugin loads in console (`DiscordLinkGate enabled.`).
4. Confirm DB file exists at:

```text
plugins/DiscordLinkGate/linking.db
```

---

## 5) Set up and run the Discord bot

From this repository:

```bash
cd bot
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Set env vars (IMPORTANT: `LINK_DB_PATH` must point to the same DB used by Paper):

```bash
export DISCORD_BOT_TOKEN="YOUR_BOT_TOKEN"
export LINK_DB_PATH="/absolute/path/to/your/server/plugins/DiscordLinkGate/linking.db"
python link_bot.py
```

When bot starts, it syncs slash commands using `bot.tree.sync()`.

---

## 6) Test flow (end-to-end)

1. Join Minecraft with an unlinked account.
2. Try moving → movement should be blocked.
3. You should see an in-game code message.
4. In Discord, run:

```text
/link ABC123
```

(Use your real code.)

5. You should get a Discord success message.
6. Back in Minecraft, movement should unlock within a few seconds.
7. Disconnect/rejoin Minecraft → should still be linked and able to move.

If code expires, use `/linkcode` in-game to generate a new one.

---

## 7) Commands

### Minecraft

- `/linkcode` → regenerate a fresh code.

### Discord

- `/link <code>` → link current Discord user.
- `/unlink` → remove all links for current Discord user.

---

## 8) Troubleshooting checklist

- Bot commands not showing?
  - Re-invite bot with `applications.commands` scope.
  - Confirm bot is online.
  - Wait a minute for global command propagation.

- Link says invalid code?
  - Ensure you used the current code shown in-game.
  - Code expires after 10 minutes.
  - Ensure bot points to same `linking.db` file as the Minecraft server.

- Still blocked after linking?
  - Wait up to ~5 seconds (plugin refresh task).
  - Check server logs for plugin/DB errors.
  - Verify DB path is identical for plugin and bot.

---

## 9) Version note

- Built for Paper API `1.21.1` and intended for 1.21.x servers.
