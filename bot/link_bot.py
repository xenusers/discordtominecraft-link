import json
import os
import time
from pathlib import Path

import discord
from discord import app_commands

LOGINS_FILE = Path(os.getenv("LOGINS_FILE", "logins.json"))
BOT_TOKEN = os.getenv("DISCORD_BOT_TOKEN", "")


def load_store() -> dict:
    if not LOGINS_FILE.exists():
        return {"links": {}, "pending_codes": {}}

    try:
        data = json.loads(LOGINS_FILE.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return {"links": {}, "pending_codes": {}}

    if not isinstance(data, dict):
        return {"links": {}, "pending_codes": {}}

    data.setdefault("links", {})
    data.setdefault("pending_codes", {})
    return data


def save_store(data: dict) -> None:
    LOGINS_FILE.parent.mkdir(parents=True, exist_ok=True)
    LOGINS_FILE.write_text(json.dumps(data, indent=2), encoding="utf-8")


class LinkClient(discord.Client):
    def __init__(self) -> None:
        intents = discord.Intents.default()
        super().__init__(intents=intents)
        self.tree = app_commands.CommandTree(self)

    async def setup_hook(self) -> None:
        await self.tree.sync()


client = LinkClient()


@client.tree.command(name="link", description="Link your Discord to your Minecraft code")
@app_commands.describe(code="The in-game link code")
async def link(interaction: discord.Interaction, code: str) -> None:
    code = code.strip().upper()
    store = load_store()

    pending = store["pending_codes"].get(code)
    if pending is None:
        await interaction.response.send_message(
            "Invalid code. Join Minecraft and get a fresh code.",
            ephemeral=True,
        )
        return

    now = int(time.time())
    expires_at = int(pending.get("expires_at", 0))
    if expires_at < now:
        store["pending_codes"].pop(code, None)
        save_store(store)
        await interaction.response.send_message(
            "That code expired. Rejoin Minecraft or use /linkcode.",
            ephemeral=True,
        )
        return

    minecraft_uuid = pending.get("minecraft_uuid", "")
    store["links"][minecraft_uuid] = {
        "discord_id": str(interaction.user.id),
        "linked_at": now,
    }
    store["pending_codes"].pop(code, None)
    save_store(store)

    await interaction.response.send_message(
        f"Linked! Minecraft UUID `{minecraft_uuid}` can now move in-game.",
        ephemeral=True,
    )


@client.tree.command(name="unlink", description="Unlink your Discord from all Minecraft accounts")
async def unlink(interaction: discord.Interaction) -> None:
    store = load_store()
    discord_id = str(interaction.user.id)

    to_delete = [
        uuid
        for uuid, info in store["links"].items()
        if isinstance(info, dict) and info.get("discord_id") == discord_id
    ]

    for uuid in to_delete:
        store["links"].pop(uuid, None)

    save_store(store)

    await interaction.response.send_message(
        f"Unlinked {len(to_delete)} Minecraft account(s).",
        ephemeral=True,
    )


if __name__ == "__main__":
    if not BOT_TOKEN:
        raise RuntimeError("Set DISCORD_BOT_TOKEN before running the bot.")
    client.run(BOT_TOKEN)
