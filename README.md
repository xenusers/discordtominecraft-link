# Discord ↔ Minecraft Link Gate (Paper 1.21.x + MySQL)

This setup blocks player movement until they link Minecraft to Discord with `/link <code>`.

## What changed

- Plugin now uses **MySQL** (not SQLite).
- Discord bot now uses **MySQL** too.
- Both point to the same DB so linking is instant and persistent.

## Your MySQL info

Use this in both plugin config and bot env vars:

- Host: `db-mfl-02.apollopanel.com`
- Port: `3306`
- Database: `s213331_miencraft`
- User: `u213331_VU8wTPmYSL`
- Password: your provided password

## 1) Build plugin

```bash
mvn -DskipTests package
```

Then copy jar from `target/` into your Paper server `plugins/` folder.

## 2) First server start

Start Paper once so plugin creates:

```text
plugins/DiscordLinkGate/config.yml
```

Open that file and set DB values. Example:

```yaml
database:
  host: "db-mfl-02.apollopanel.com"
  port: 3306
  name: "s213331_miencraft"
  username: "u213331_VU8wTPmYSL"
  password: "YOUR_DB_PASSWORD"

code:
  expiry-seconds: 600
  length: 6
```

Restart server after editing.

## 3) Start Discord bot

```bash
cd bot
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

Set vars and run:

```bash
export DISCORD_BOT_TOKEN="YOUR_TOKEN"
export MYSQL_HOST="db-mfl-02.apollopanel.com"
export MYSQL_PORT="3306"
export MYSQL_DATABASE="s213331_miencraft"
export MYSQL_USERNAME="u213331_VU8wTPmYSL"
export MYSQL_PASSWORD="YOUR_DB_PASSWORD"
python link_bot.py
```

> If you really want to hardcode token, edit `bot/link_bot.py` and assign `BOT_TOKEN = "..."` directly.

## 4) Invite bot correctly

Invite URL must include scopes:

- `bot`
- `applications.commands`

And grant normal send/read permissions where you will use slash commands.

## 5) Test everything

1. Join Minecraft with unlinked account.
2. You should be unable to move.
3. Minecraft chat shows code (example `AB12CD`).
4. In Discord run `/link AB12CD`.
5. Should return success.
6. Move in Minecraft again: now unlocked.
7. Rejoin server: should still be linked.

## Commands

### Minecraft
- `/linkcode` -> regenerate code

### Discord
- `/link <code>` -> link your Discord
- `/unlink` -> remove all your links

## Troubleshooting

- Slash commands missing: ensure bot has `applications.commands`, wait ~1 minute.
- "Invalid code": wrong/expired code (default 10 min).
- Still blocked after linking: check Paper console for DB errors and confirm plugin+bot use the same MySQL DB.
