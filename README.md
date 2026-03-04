# Discord ↔ Minecraft Link Gate (Paper 1.21.x + MySQL)

This setup blocks player movement until they link Minecraft to Discord with `/link <code>`.

## What changed

- Plugin uses **MySQL**.
- Discord bot uses **MySQL**.
- Both point to the same DB so linking is instant and persistent.

## Your MySQL info

Use this in both plugin config and bot env vars:

- Host: `db-mfl-02.apollopanel.com`
- Port: `3306`
- Database: `s213331_miencraft`
- User: `u213331_VU8wTPmYSL`
- Password: your provided password

---

## Windows quick fix for your exact errors

You got:

- `bash : The term 'bash' is not recognized`
- `mvn : The term 'mvn' is not recognized`

That means on Windows:

1. **You do not need `bash` at all** for this project.
2. Maven is not installed (or not in PATH).

### Install Java 21 and Maven (PowerShell as Administrator)

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Apache.Maven
```

Close and reopen PowerShell, then verify:

```powershell
java -version
mvn -version
```

If `mvn` still fails, reboot once or add Maven `bin` folder to PATH manually.

---

## Build plugin (PowerShell, Windows)

Run from repo folder:

```powershell
mvn -DskipTests package
```

Jar will be in `target/`.

Copy jar into your Paper server `plugins/` folder.

---

## First server start

Start Paper once so plugin creates:

```text
plugins/DiscordLinkGate/config.yml
```

Edit that file:

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

Restart server.

---

## Run Discord bot on Windows (PowerShell)

```powershell
cd bot
py -3 -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

Set env vars for this session:

```powershell
$env:DISCORD_BOT_TOKEN="YOUR_TOKEN"
$env:MYSQL_HOST="db-mfl-02.apollopanel.com"
$env:MYSQL_PORT="3306"
$env:MYSQL_DATABASE="s213331_miencraft"
$env:MYSQL_USERNAME="u213331_VU8wTPmYSL"
$env:MYSQL_PASSWORD="YOUR_DB_PASSWORD"
python .\link_bot.py
```

> You said token can be hardcoded. It works, but env var is safer.

---

## Test everything

1. Join Minecraft with unlinked account.
2. Movement should be blocked.
3. You get a code in chat.
4. In Discord run `/link <code>`.
5. Bot says linked.
6. Movement unlocks in Minecraft.
7. Rejoin server: still linked.

---

## Commands

### Minecraft
- `/linkcode` -> regenerate code

### Discord
- `/link <code>` -> link your Discord
- `/unlink` -> remove all your links

---

## Troubleshooting

- **`bash` not recognized**: ignore bash, use PowerShell commands above.
- **`mvn` not recognized**: install Maven with winget and reopen terminal.
- Slash commands missing: ensure bot invited with `applications.commands`, wait ~1 minute.
- "Invalid code": wrong/expired code (default 10 minutes).
- Still blocked after linking: check Paper console for DB errors and confirm plugin+bot use same MySQL DB.

---

## Security note (important)

If you shared your DB password or Discord bot token publicly, rotate both immediately.
