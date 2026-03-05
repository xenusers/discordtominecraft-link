# Discord ↔ Minecraft Link Gate (Paper 1.21.x + `logins.json`)

Your MySQL error means the hosting DB is rejecting your server host. This build removes DB dependency and uses a shared JSON file instead.

## What changed

- Plugin now stores links + pending codes in `logins.json`.
- Discord bot also reads/writes that same `logins.json`.
- No MySQL required.

## Fix for the error you posted

Error was:

```text
Access denied for user ...
```

That is a database permission/network issue. With this JSON setup, that issue is gone.

---

## 1) Build plugin (Windows PowerShell)

Install Java + Maven if needed:

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
winget install Apache.Maven
```

Then in repo root:

```powershell
mvn -DskipTests package
```

Copy plugin jar from `target/` into your Paper server `plugins/` folder.

---

## 2) Configure plugin storage path

Start server once, then edit:

```text
plugins/DiscordLinkGate/config.yml
```

Example:

```yaml
storage:
  file: "logins.json"

code:
  expiry-seconds: 600
  length: 6
```

If bot runs on another machine, use an **absolute shared path** (network mount/sync path).

---

## 3) Run bot with same `logins.json`

In `bot/`:

```powershell
py -3 -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

Run bot with path to the exact same JSON file:

```powershell
$env:DISCORD_BOT_TOKEN="YOUR_TOKEN"
$env:LOGINS_FILE="C:\path\to\server\plugins\DiscordLinkGate\logins.json"
python .\link_bot.py
```

> The plugin and bot must point to the same file, or linking won't sync.

---

## 4) Test flow

1. Join Minecraft (unlinked account).
2. Movement should be blocked.
3. You get a code in chat.
4. In Discord: `/link <code>`.
5. Bot replies linked.
6. Movement unlocks in Minecraft.
7. Rejoin: still linked (persisted in `logins.json`).

---

## Commands

### Minecraft
- `/linkcode` -> regenerate code

### Discord
- `/link <code>` -> link your Discord
- `/unlink` -> remove your linked accounts

---

## `logins.json` format

```json
{
  "links": {
    "minecraft-uuid": {
      "discord_id": "1234567890",
      "linked_at": 1737000000
    }
  },
  "pending_codes": {
    "ABC123": {
      "minecraft_uuid": "minecraft-uuid",
      "expires_at": 1737000600
    }
  }
}
```

---

## Troubleshooting

- `mvn` not recognized: reopen PowerShell after install, run `mvn -version`.
- Slash commands missing: invite bot with `applications.commands` scope, wait ~1 minute.
- Invalid code: code expired or bot is not using same `LOGINS_FILE` path as plugin.
