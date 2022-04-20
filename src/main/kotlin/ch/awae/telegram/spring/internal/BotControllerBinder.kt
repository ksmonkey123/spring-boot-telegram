package ch.awae.telegram.spring.internal

import ch.awae.telegram.spring.annotation.*
import ch.awae.telegram.spring.annotation.mapping.FallbackMapping
import ch.awae.telegram.spring.annotation.mapping.OnCallback
import ch.awae.telegram.spring.annotation.mapping.OnMessage
import ch.awae.telegram.spring.annotation.param.Group
import ch.awae.telegram.spring.annotation.param.Raw
import ch.awae.telegram.spring.annotation.param.Text
import ch.awae.telegram.spring.api.IBotCredentials
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
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmName
import kotlin.reflect.typeOf

@Configuration
class BotControllerBinder(
    val botsApi: TelegramBotsApi,
    val telegramBotConfiguration: TelegramBotConfiguration,
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
            logger.info("registered bot '${botName}' with Telegram API")
        }

        senderRegistry.botMap = bindings

        logger.info("=========================================")
        logger.info("finished binding Telegram Bot Controllers")
        logger.info("=========================================")

    }

    private fun getBindings(): Map<String, BotControllerBinding> {
        val bots = appContext.getBeansWithAnnotation<BotController>()
            .map { (beanName, bean) ->
                val annotation = appContext.findAnnotationOnBean<BotController>(beanName)
                    ?: throw InitializationException("could not find @BotController annotation on bean '$beanName'")
                Pair(annotation.name, bean)
            }
            .groupBy({ it.first }, { it.second })

        logger.info("loaded ${bots.values.flatten().size} controller(s):")
        bots.forEach { (botName, controllers) ->
            logger.info(" - $botName:")
            controllers.forEach {
                logger.info("    - ${it::class.java.name}")
            }
        }

        return bots.mapValues { (botName, beans) ->
            getBinding(botName, telegramBotConfiguration.getBotCredentials(botName), beans)
        }
    }

    private fun getBinding(botName: String, config: IBotCredentials, beans: List<Any>): BotControllerBinding {
        val allHandlers = beans.flatMap { getHandlersForBean(it) }

        logger.info("loaded ${allHandlers.size} handlers(s) for bot '${botName}':")
        allHandlers.forEach {
            logger.info(" - $it")
        }

        val normalHandlers = allHandlers.filterNot { it is FallbackHandler }
        val fallbackHandlers = allHandlers.filterIsInstance<FallbackHandler>()

        if (fallbackHandlers.size > 1) {
            throw InitializationException("multiple fallback handlers found for bot '${botName}'! each bot may only have one fallback handler")
        }
        return BotControllerBinding(config, normalHandlers, fallbackHandlers.firstOrNull(), telegramBotConfiguration)
    }

    fun getHandlersForBean(bean: Any): List<Handler> {
        val beanAuthAnnotation = bean::class.findAnnotation<Authorized>()
        return (bean::class).functions.flatMap { getHandlersForFunction(bean, beanAuthAnnotation, it) }
    }

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
            ParameterMapper.getParameterMapping(it, annotation)
        }
    }

}