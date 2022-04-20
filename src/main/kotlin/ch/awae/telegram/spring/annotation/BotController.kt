package ch.awae.telegram.spring.annotation

import org.springframework.stereotype.Component

/**
 * Marks a Component as a Telegram Bot Controller.
 *
 * Telegram bot controllers can have functions annotated with
 * mapping annotations. (ch.awae.telegram.spring.annotation.mapping.*)
 *
 * These functions are picked up and used to handle incoming updates
 * from the telegram bot API.
 *
 * Each controller class is associated with exactly one telegram bot, but
 * each bot can have multiple controllers. If only one bot is used in a project,
 * the default empty name can be used. If multiple bots are used it is recommended
 * not to use the empty name - although allowed - as it may lead to confusion.
 */
@Component
@Target(AnnotationTarget.CLASS)
annotation class BotController(val name: String = "")
