package ch.awae.telegram.spring.internal

import ch.awae.telegram.spring.annotation.Authorized
import ch.awae.telegram.spring.annotation.BotController
import ch.awae.telegram.spring.annotation.mapping.FallbackMapping
import ch.awae.telegram.spring.annotation.mapping.OnCallback
import ch.awae.telegram.spring.annotation.mapping.OnMessage
import ch.awae.telegram.spring.api.BotCredentials
import ch.awae.telegram.spring.api.TelegramBotConfiguration
import ch.awae.telegram.spring.internal.handler.CallbackHandler
import ch.awae.telegram.spring.internal.handler.FallbackHandler
import ch.awae.telegram.spring.internal.handler.Handler
import ch.awae.telegram.spring.internal.handler.MessageHandler
import ch.awae.telegram.spring.internal.param.ParameterMapping
import org.springframework.beans.factory.findAnnotationOnBean
import org.springframework.beans.factory.getBeansWithAnnotation
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.telegram.telegrambots.meta.TelegramBotsApi
import java.util.logging.Logger
import javax.annotation.PostConstruct
import kotlin.reflect.KFunction
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmName

@Configuration
class BotControllerBinder(
    val botsApi: TelegramBotsApi,
    val telegramBotConfiguration: TelegramBotConfiguration<*,*>,
    val senderRegistry: MapBasedSenderRegistry,
    val appContext: ApplicationContext,
) {

    private val logger = Logger.getLogger(BotControllerBinder::class.jvmName)

    @PostConstruct
    fun initBots() {
        logger.info("=========================================")
        logger.info("starting binding Telegram Bot Controllers")
        logger.info("=========================================")

        val bindings = getBindings()
        bindings.forEach { (botName, bot) ->
            botsApi.registerBot(bot)
            logger.info("registered bot '${printableName(botName)}' with Telegram API")
        }

        senderRegistry.botMap = bindings

        logger.info("=========================================")
        logger.info("finished binding Telegram Bot Controllers")
        logger.info("=========================================")

    }

    private fun getBindings(): Map<String, BotControllerBinding<*,*>> {
        val bots = appContext.getBeansWithAnnotation<BotController>()
            .map { (beanName, bean) ->
                val annotation = appContext.findAnnotationOnBean<BotController>(beanName)
                    ?: throw InitializationException("could not find @BotController annotation on bean '$beanName'")
                Pair(annotation.name, bean)
            }
            .groupBy({ it.first }, { it.second })

        logger.info("loaded ${bots.values.flatten().size} controller(s):")
        bots.forEach { (botName, controllers) ->
            logger.info(" - ${printableName(botName)}:")
            controllers.forEach {
                logger.info("    - ${it::class.java.name}")
            }
        }

        return bots.mapValues { (botName, beans) ->
            getBinding(botName, telegramBotConfiguration.getBotCredentials(botName), beans)
        }
    }

    private fun getBinding(botName: String, config: BotCredentials, beans: List<Any>): BotControllerBinding<*,*> {
        val allHandlers = beans.flatMap { getHandlersForBean(it) }

        logger.info("loaded ${allHandlers.size} handlers(s) for bot '${printableName(botName)}':")
        allHandlers.forEach {
            logger.info(" - $it")
            it.parameterMapping.forEachIndexed { index, mapping ->
                logger.config("    - parameter $index: $mapping")
            }
        }

        val normalHandlers = allHandlers.filterNot { it is FallbackHandler }
        val fallbackHandlers = allHandlers.filterIsInstance<FallbackHandler>()

        if (fallbackHandlers.size > 1) {
            throw InitializationException("multiple fallback handlers found for bot '${printableName(botName)}'! each bot may only have one fallback handler")
        }
        return BotControllerBinding(config, normalHandlers, fallbackHandlers.firstOrNull(), telegramBotConfiguration)
    }

    fun getHandlersForBean(bean: Any): List<Handler> {
        val classes = listOf(bean::class) + bean::class.allSuperclasses
        return classes.flatMap {
            val classAuth = it.findAnnotation<Authorized>()
            it.functions.map { it to classAuth }
        }.flatMap { (function, classAuth) -> getHandlersForFunction(bean, classAuth, function) }
    }

    private fun printableName(botName: String): String = botName.ifEmpty { "[default]" }

    private fun getHandlersForFunction(
        bean: Any,
        beanAuthAnnotation: Authorized?,
        function: KFunction<*>
    ): List<Handler> {
        return function.annotations.mapNotNull {
            buildHandler(bean, beanAuthAnnotation, function, it)
        }
    }

    private fun buildHandler(
        bean: Any,
        beanAuthAnnotation: Authorized?,
        function: KFunction<*>,
        annotation: Annotation
    ): Handler? {
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
                FallbackHandler(
                    bean,
                    function,
                    getFunctionParamMappings(function, annotation),
                    annotation,
                    beanAuthAnnotation,
                    authAnnotation
                )
            }

            else -> null
        }
    }

    private fun getFunctionParamMappings(function: KFunction<*>, annotation: Annotation): List<ParameterMapping> {
        return function.valueParameters.map {
            ParameterMapper.getParameterMapping(it, annotation)
        }
    }

}
