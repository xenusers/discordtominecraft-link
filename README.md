# Discord ↔ Minecraft Link Gate (Paper 1.21.x + MySQL)

Yes — same as your old MySQL setup, but SSL must be enabled for this host.

## Your database values

- Host: `sql3.freesqldatabase.com`
- Port: `3306`
- Database: `sql3818940`
- Username: `sql3818940`
- Password: **the real password from your email** (not the text `Check your emails`)

## Why it still failed

Your log means the plugin tried a non-SSL connection. This update now:

- Uses MySQL `sslMode=REQUIRED` when SSL is enabled.
- Automatically retries with SSL if config had `database.ssl: false` and DB says SSL is required.

## 1) Plugin config (Paper)

Edit `plugins/DiscordLinkGate/config.yml`:

```yaml
database:
  host: "sql3.freesqldatabase.com"
  port: 3306
  name: "sql3818940"
  username: "sql3818940"
  password: "REAL_PASSWORD_FROM_EMAIL"
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
$env:MYSQL_PASSWORD="REAL_PASSWORD_FROM_EMAIL"
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

## If it still fails

- Confirm password is the actual email password value.
- Confirm your host/IP is allowed in your DB panel.
- Keep SSL enabled (`database.ssl: true`, `MYSQL_SSL=true`).


## Important if log says `[DiscordLinkTest]` instead of `[DiscordLinkGate]`

You are likely running an older/different jar. Delete old plugin jar(s) and keep only the latest one from this repo, then restart. Also edit the config for the plugin name that is actually loading.
