import os
import sqlite3
import time

import discord
from discord import app_commands

DB_PATH = os.getenv("LINK_DB_PATH", "plugins/DiscordLinkGate/linking.db")
BOT_TOKEN = os.getenv("DISCORD_BOT_TOKEN", "")


def get_connection() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH)
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS links (
            minecraft_uuid TEXT PRIMARY KEY,
            discord_id TEXT NOT NULL,
            linked_at INTEGER NOT NULL
        )
        """
    )
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS pending_codes (
            code TEXT PRIMARY KEY,
            minecraft_uuid TEXT NOT NULL,
            expires_at INTEGER NOT NULL
        )
        """
    )
    return conn


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

    with get_connection() as conn:
        row = conn.execute(
            "SELECT minecraft_uuid, expires_at FROM pending_codes WHERE code = ?",
            (code,),
        ).fetchone()

        if row is None:
            await interaction.response.send_message("Invalid code. Join Minecraft and get a fresh code.", ephemeral=True)
            return

        minecraft_uuid, expires_at = row
        now = int(time.time())
        if expires_at < now:
            conn.execute("DELETE FROM pending_codes WHERE code = ?", (code,))
            await interaction.response.send_message("That code expired. Rejoin Minecraft or use /linkcode.", ephemeral=True)
            return

        conn.execute(
            "INSERT OR REPLACE INTO links(minecraft_uuid, discord_id, linked_at) VALUES (?, ?, ?)",
            (minecraft_uuid, str(interaction.user.id), now),
        )
        conn.execute("DELETE FROM pending_codes WHERE code = ?", (code,))

    await interaction.response.send_message(
        f"Linked! Minecraft UUID `{minecraft_uuid}` can now move in-game.",
        ephemeral=True,
    )


@client.tree.command(name="unlink", description="Unlink your Discord from all Minecraft accounts")
async def unlink(interaction: discord.Interaction) -> None:
    with get_connection() as conn:
        conn.execute("DELETE FROM links WHERE discord_id = ?", (str(interaction.user.id),))

    await interaction.response.send_message("Unlinked all your linked Minecraft accounts.", ephemeral=True)


if __name__ == "__main__":
    if not BOT_TOKEN:
        raise RuntimeError("Set DISCORD_BOT_TOKEN before running the bot.")
    client.run(BOT_TOKEN)
