package com.kotdex.coroutines

import kotlinx.coroutines.future.await
import net.dv8tion.jda.api.requests.RestAction

public suspend fun <T> RestAction<T>.await(): T = submit().await()
