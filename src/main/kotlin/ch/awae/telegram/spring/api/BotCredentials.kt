package ch.awae.telegram.spring.api

data class BotCredentials(override val username : String, override val token: String) : IBotCredentials