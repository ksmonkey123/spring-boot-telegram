package ch.awae.telegram.spring.api

data class BasicBotCredentials(override val username : String, override val token: String) : BotCredentials