package com.kotdex.annotations

import net.dv8tion.jda.api.interactions.commands.Command

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class ContextMenu(val name: String, val type: Command.Type = Command.Type.USER)
