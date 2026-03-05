# Discord ↔ Minecraft Link Gate (Paper 1.21.x + MySQL)

Yes — same as your old MySQL setup, but now make sure SSL is enabled because your host requires it.

## Your database values

- Host: `sql3.freesqldatabase.com`
- Port: `3306`
- Database: `sql3818940`
- Username: `sql3818940`
- Password: `GJxGGMib41`

## Fix for this error

If you get:

```text
SSL Connection required, but not provided by server
```

set `database.ssl: true` in plugin config and `MYSQL_SSL=true` for the bot.

## 1) Plugin config (Paper)

Edit `plugins/DiscordLinkGate/config.yml`:

```yaml
database:
  host: "sql3.freesqldatabase.com"
  port: 3306
  name: "sql3818940"
  username: "sql3818940"
  password: "GJxGGMib41"
  ssl: true

code:
  expiry-seconds: 600
  length: 6
```

Restart server after changing it.

## 2) Bot env vars (PowerShell)

```powershell
$env:DISCORD_BOT_TOKEN="YOUR_TOKEN"
$env:MYSQL_HOST="sql3.freesqldatabase.com"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="sql3818940"
$env:MYSQL_USERNAME="sql3818940"
$env:MYSQL_PASSWORD="GJxGGMib41"
$env:MYSQL_SSL="true"
python .\link_bot.py
```

## 3) Build plugin

```powershell
mvn -DskipTests package
```

Copy jar from `target/` to your server `plugins/` folder.

## 4) Test flow

1. Join Minecraft unlinked: movement blocked.
2. Get code in chat.
3. Run `/link <code>` in Discord.
4. Movement unlocks.
5. Rejoin: remains linked.

## If access still fails

- Confirm DB host/user/pass are correct.
- Confirm your server host/IP is allowed by DB provider remote access.
- Keep SSL enabled (`database.ssl: true`, `MYSQL_SSL=true`).
