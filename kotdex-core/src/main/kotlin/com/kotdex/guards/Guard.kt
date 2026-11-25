package com.kotdex.guards

import net.dv8tion.jda.api.events.GenericEvent

public fun interface Guard {
    public suspend fun check(event: GenericEvent): Boolean
}
