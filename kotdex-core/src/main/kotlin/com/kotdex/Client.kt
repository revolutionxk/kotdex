package com.kotdex

import com.kotdex.internal.dispatch.Dispatcher
import com.kotdex.internal.handlers.instantiate
import com.kotdex.internal.registration.registerCommands
import com.kotdex.internal.scan
import com.kotdex.metadata.MetadataStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GenericGuildEvent
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.interactions.Interaction
import net.dv8tion.jda.api.requests.GatewayIntent
import org.slf4j.LoggerFactory
import java.util.ServiceLoader
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean

public class Client(configure: ClientOptions.() -> Unit = {}) {
    private val options = ClientOptions().apply(configure)
    private val logger = LoggerFactory.getLogger(Client::class.java)
    private val pending = CopyOnWriteArrayList<EventListener>()

    @Volatile
    private var instance: JDA? = null

    public val metadata: MetadataStorage = MetadataStorage(
        setOf(Client::class, JDA::class, Guild::class, User::class, Member::class, MessageChannel::class, Message::class),
    )

    public val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName("kotdex"))

    public val jda: JDA get() = instance ?: error("The client is not logged in yet")

    private val dispatcher = Dispatcher(metadata, ::injectables, options.errorHandler, options.prefix, options.caseSensitive)

    private val router = EventListener { event -> scope.launch { dispatcher.dispatch(event) } }

    public fun scan(vararg packages: String) {
        metadata.register(*scan(packages).map(::instantiate).toTypedArray())
    }

    public fun register(vararg instances: Any) {
        metadata.register(*instances)
    }

    public inline fun <reified E : GenericEvent> on(noinline block: suspend (E) -> Unit) {
        listen(E::class.java, once = false, block)
    }

    public inline fun <reified E : GenericEvent> once(noinline block: suspend (E) -> Unit) {
        listen(E::class.java, once = true, block)
    }

    @PublishedApi
    internal fun <E : GenericEvent> listen(type: Class<E>, once: Boolean, block: suspend (E) -> Unit) {
        val listener = object : EventListener {
            private val done = AtomicBoolean()

            override fun onEvent(event: GenericEvent) {
                if (!type.isInstance(event)) return
                if (once) {
                    if (!done.compareAndSet(false, true)) return
                    instance?.removeEventListener(this)
                }
                scope.launch {
                    try {
                        block(type.cast(event))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Throwable) {
                        options.errorHandler(event, e)
                    }
                }
            }
        }
        instance?.addEventListener(listener) ?: pending.add(listener)
    }

    public suspend fun login(token: String): JDA {
        if (metadata.simpleCommands.isNotEmpty() && GatewayIntent.MESSAGE_CONTENT !in options.intents) {
            logger.warn(
                "Prefix commands are registered but GatewayIntent.MESSAGE_CONTENT is not enabled; guild messages will arrive without content",
            )
        }
        val jda = withContext(Dispatchers.IO) {
            configure(JDABuilder.createDefault(token, options.intents)).build().also { instance = it }.awaitReady()
        }
        if (options.registerCommands) initApplicationCommands()
        return jda
    }

    public suspend fun initApplicationCommands() {
        registerCommands(jda, metadata, options.guilds)
    }

    public fun shutdown() {
        scope.cancel()
        instance?.shutdown()
    }

    internal fun configure(builder: JDABuilder): JDABuilder {
        builder.addEventListeners(router, *pending.toTypedArray())
        ServiceLoader.load(ClientExtension::class.java).forEach { it.install(builder) }
        options.builders.forEach { it(builder) }
        return builder
    }

    private fun injectables(event: GenericEvent): List<Any> = buildList {
        add(this@Client)
        add(event.jda)
        when (event) {
            is Interaction -> {
                event.guild?.let(::add)
                add(event.user)
                event.member?.let(::add)
                (event.channel as? MessageChannel)?.let(::add)
                when (event) {
                    is MessageContextInteractionEvent -> add(event.target)
                    is GenericComponentInteractionCreateEvent -> add(event.message)
                    is ModalInteractionEvent -> event.message?.let(::add)
                }
            }
            is MessageReceivedEvent -> {
                add(event.message)
                if (event.isFromGuild) add(event.guild)
                add(event.author)
                event.member?.let(::add)
                add(event.channel)
            }
            is GenericGuildEvent -> add(event.guild)
        }
    }
}
