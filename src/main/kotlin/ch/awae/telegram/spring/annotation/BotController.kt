package ch.awae.telegram.spring.annotation

import org.springframework.stereotype.Component

@Component
@Target(AnnotationTarget.CLASS)
annotation class BotController(val name: String)
