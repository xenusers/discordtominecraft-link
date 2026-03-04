import os
import time

import discord
import pymysql
from discord import app_commands

DB_HOST = os.getenv("MYSQL_HOST", "db-mfl-02.apollopanel.com")
DB_PORT = int(os.getenv("MYSQL_PORT", "3306"))
DB_NAME = os.getenv("MYSQL_DATABASE", "s213331_miencraft")
DB_USER = os.getenv("MYSQL_USERNAME", "u213331_VU8wTPmYSL")
DB_PASSWORD = os.getenv("MYSQL_PASSWORD", "!=@jOyx38E+QqhTC4CRuE@iD")

# You can hardcode your token here if you want, but env var is safer.
BOT_TOKEN = os.getenv("DISCORD_BOT_TOKEN", "")


def get_connection() -> pymysql.Connection:
    return pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        user=DB_USER,
        password=DB_PASSWORD,
        database=DB_NAME,
        autocommit=True,
        cursorclass=pymysql.cursors.DictCursor,
    )


def init_db() -> None:
    with get_connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS links (
                    minecraft_uuid VARCHAR(36) PRIMARY KEY,
                    discord_id VARCHAR(32) NOT NULL,
                    linked_at BIGINT NOT NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
            )
            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS pending_codes (
                    code VARCHAR(16) PRIMARY KEY,
                    minecraft_uuid VARCHAR(36) NOT NULL,
                    expires_at BIGINT NOT NULL,
                    INDEX idx_pending_uuid (minecraft_uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """
            )


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
        with conn.cursor() as cursor:
            cursor.execute(
                "SELECT minecraft_uuid, expires_at FROM pending_codes WHERE code = %s",
                (code,),
            )
            row = cursor.fetchone()

            if row is None:
                await interaction.response.send_message(
                    "Invalid code. Join Minecraft and get a fresh code.",
                    ephemeral=True,
                )
                return

            minecraft_uuid = row["minecraft_uuid"]
            expires_at = int(row["expires_at"])
            now = int(time.time())
            if expires_at < now:
                cursor.execute("DELETE FROM pending_codes WHERE code = %s", (code,))
                await interaction.response.send_message(
                    "That code expired. Rejoin Minecraft or use /linkcode.",
                    ephemeral=True,
                )
                return

            cursor.execute(
                """
                INSERT INTO links(minecraft_uuid, discord_id, linked_at)
                VALUES (%s, %s, %s)
                ON DUPLICATE KEY UPDATE discord_id = VALUES(discord_id), linked_at = VALUES(linked_at)
                """,
                (minecraft_uuid, str(interaction.user.id), now),
            )
            cursor.execute("DELETE FROM pending_codes WHERE code = %s", (code,))

    await interaction.response.send_message(
        f"Linked! Minecraft UUID `{minecraft_uuid}` can now move in-game.",
        ephemeral=True,
    )


@client.tree.command(name="unlink", description="Unlink your Discord from all Minecraft accounts")
async def unlink(interaction: discord.Interaction) -> None:
    with get_connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute("DELETE FROM links WHERE discord_id = %s", (str(interaction.user.id),))

    await interaction.response.send_message(
        "Unlinked all your linked Minecraft accounts.",
        ephemeral=True,
    )


if __name__ == "__main__":
    init_db()
    if not BOT_TOKEN:
        raise RuntimeError("Set DISCORD_BOT_TOKEN before running the bot.")
    client.run(BOT_TOKEN)
