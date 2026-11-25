package com.kotdex.internal.registration

import com.kotdex.coroutines.await
import com.kotdex.metadata.MetadataStorage
import net.dv8tion.jda.api.JDA
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.kotdex.Registrar")

internal suspend fun registerCommands(jda: JDA, metadata: MetadataStorage, devGuilds: Set<Long>) {
    val commands = buildCommands(metadata, devGuilds)

    if (devGuilds.isEmpty()) {
        val global = commands[null].orEmpty()
        jda.updateCommands().addCommands(global).await()
        logger.info("Registered {} global commands", global.size)
    }

    for ((id, data) in commands) {
        if (id == null) continue
        val guild = jda.getGuildById(id)
        if (guild == null) {
            logger.warn("Skipping commands for guild {}: the bot is not in it", id)
            continue
        }
        guild.updateCommands().addCommands(data).await()
        logger.info("Registered {} commands in guild {}", data.size, guild.name)
    }
}
