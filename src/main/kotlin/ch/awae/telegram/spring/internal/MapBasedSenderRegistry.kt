package ch.awae.telegram.spring.internal

import ch.awae.telegram.spring.api.SenderRegistry
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.bots.AbsSender

@Component
class MapBasedSenderRegistry : SenderRegistry {

    lateinit var botMap : Map<String, AbsSender>

    override fun get(botName: String) = botMap.getValue(botName)


}