package ch.awae.telegram.spring.internal

import ch.awae.telegram.spring.annotation.*
import ch.awae.telegram.spring.api.BotCredentials
import ch.awae.telegram.spring.api.Principal
import ch.awae.telegram.spring.api.TelegramBotConfiguration
import ch.awae.telegram.spring.internal.handler.CallbackHandler
import ch.awae.telegram.spring.internal.handler.FallbackHandler
import ch.awae.telegram.spring.internal.handler.Handler
import ch.awae.telegram.spring.internal.handler.MessageHandler
import ch.awae.telegram.spring.internal.param.*
import org.springframework.beans.factory.findAnnotationOnBean
import org.springframework.beans.factory.getBeansWithAnnotation
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import java.util.logging.Logger
import javax.annotation.PostConstruct
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.typeOf

@Configuration
class BotControllerBinder(
        val botsApi: TelegramBotsApi,
        val appContext: ApplicationContext,
        val botCredentials: List<BotCredentials>,
        val telegramBotConfiguration: TelegramBotConfiguration,
) {

    private val logger = Logger.getLogger(BotControllerBinder::class.jvmName)

    @PostConstruct
    fun initBots() {
        logger.info("=========================================")
        logger.info("starting binding Telegram Bot Controllers")
        logger.info("=========================================")

        val bindings = getBindings()
        bindings.forEach {
            botsApi.registerBot(it)
            logger.info("registered bot '${it.configuration.botName}' with Telegram API")
        }

        logger.info("=========================================")
        logger.info("finished binding Telegram Bot Controllers")
        logger.info("=========================================")

    }

    private fun getBindings(): List<BotControllerBinding> {
        val credentials = botCredentials.associateBy { it.botName }

        logger.info("loaded ${credentials.size} bot credentials(s):")
        credentials.keys.forEach { logger.info(" - $it") }

        val bots = appContext.getBeansWithAnnotation<BotController>()
                .map { (beanName, bean) ->
                    val annotation = appContext.findAnnotationOnBean<BotController>(beanName)
                            ?: throw InitializationException("could not find @BotController annotation on bean '$beanName'")
                    val configuration = credentials[annotation.name]
                            ?: throw InitializationException("missing BotCredentials for bot '${annotation.name}'")
                    Pair(configuration, bean)
                }
                .groupBy({ it.first }, { it.second })

        logger.info("loaded ${bots.values.flatten().size} controller(s):")
        bots.forEach { (config, controllers) ->
            logger.info(" - ${config.botName}:")
            controllers.forEach {
                logger.info("    - ${it::class.java.name}")
            }
        }

        return bots.map { (config, beans) -> getBinding(config, beans) }
    }

    private fun getBinding(config: BotCredentials, beans: List<Any>): BotControllerBinding {
        val allHandlers = beans.flatMap { getHandlersForBean(it) }

        logger.info("loaded ${allHandlers.size} handlers(s) for bot '${config.botName}':")
        allHandlers.forEach {
            logger.info(" - $it")
        }

        val normalHandlers = allHandlers.filterNot { it is FallbackHandler }
        val fallbackHandlers = allHandlers.filterIsInstance<FallbackHandler>()

        if (fallbackHandlers.size > 1) {
            throw InitializationException("multiple fallback handlers found for bot '${config.botName}'! each bot may only have one fallback handler")
        }
        return BotControllerBinding(config, normalHandlers, fallbackHandlers.firstOrNull(), telegramBotConfiguration)
    }

    fun getHandlersForBean(bean: Any): List<Handler> {
        val beanAuthAnnotation = bean::class.findAnnotation<Authorized>()
        return (bean::class).functions.flatMap { getHandlersForFunction(bean, beanAuthAnnotation, it) }
    }

    private fun getHandlersForFunction(bean: Any, beanAuthAnnotation : Authorized?, function: KFunction<*>): List<Handler> {
        return function.annotations.mapNotNull {
            buildHandler(bean, beanAuthAnnotation, function, it)
        }
    }

    private fun buildHandler(bean: Any, beanAuthAnnotation: Authorized?, function: KFunction<*>, annotation: Annotation): Handler? {
        val authAnnotation = function.findAnnotation<Authorized>()
        return when (annotation) {
            is OnMessage ->
                MessageHandler(
                        bean,
                        Regex(annotation.pattern),
                        function,
                        getFunctionParamMappings(function, annotation),
                        annotation,
                        beanAuthAnnotation,
                        authAnnotation
                )
            is OnCallback ->
                CallbackHandler(
                        bean,
                        Regex(annotation.pattern),
                        function,
                        getFunctionParamMappings(function, annotation),
                        annotation,
                        beanAuthAnnotation,
                        authAnnotation
                )
            is FallbackMapping -> {
                if (function.returnType.isSubtypeOf(typeOf<Unit>()))
                    FallbackHandler(
                            bean,
                            function,
                            getFunctionParamMappings(function, annotation),
                            beanAuthAnnotation,
                            authAnnotation
                    )
                else
                    throw InitializationException("function annotated with @FallbackMapping must have return Type kotlin.Unit (void)")
            }
            else -> null
        }
    }

    private fun getFunctionParamMappings(function: KFunction<*>, annotation: Annotation): List<ParameterMapping> {
        return function.valueParameters.map {
            getParameterMapping(it, annotation)
        }
    }

    private fun getParameterMapping(param: KParameter, mappingAnnotation: Annotation): ParameterMapping {
        val name = param.name
        val type = param.type

        return when (val annotation = param.annotations.firstOrNull()) {
            is Group -> if (annotation.name.isNotEmpty()) NamedGroup(annotation.name) else IndexedGroup(annotation.id)
            is Raw -> when (type) {
                // nonnull raw parameters are not always allowed
                typeOf<Update>() -> RawUpdate
                typeOf<Message>() -> if (mappingAnnotation is OnMessage) RawMessage else throw InitializationException("no raw message available")
                typeOf<CallbackQuery>() -> if (mappingAnnotation is OnCallback) RawCallback else throw InitializationException("no raw callbackQuery available")
                // optional raw parameters are always allowed
                typeOf<Update?>() -> RawUpdate
                typeOf<Message?>() -> RawMessage
                typeOf<CallbackQuery?>() -> RawCallback
                else -> throw InitializationException("could not determine parameter mapping for parameter $param")
            }
            is User -> when {
                type.isSubtypeOf(typeOf<Principal?>()) -> TypedPrincipal(type)
                else -> throw InitializationException("unsupported type for parameter $param annotated with @User")
            }
            null -> when {
                type.isSubtypeOf(typeOf<String?>()) && name != null -> NamedGroup(name)
                else -> throw InitializationException("could not determine parameter mapping for parameter $param")
            }
            else -> throw InitializationException("could not determine parameter mapping for parameter $param")
        }
    }

}
