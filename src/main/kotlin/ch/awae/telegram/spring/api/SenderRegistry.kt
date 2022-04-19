package ch.awae.telegram.spring.api

import org.telegram.telegrambots.meta.bots.AbsSender

interface SenderRegistry {

    /**
     * get sender with default bot name (empty string)
     */
    fun getDefault() = get("")

    /**
     * get sender with given botName.
     *
     * Due to the bean life-cycle this method may only be available
     * after @PostInit functions have run. It is therefore recommended to
     * only access this in functions used after the spring application
     * context has fully been constructed. Retrieving the sender instance
     * with this function every time it is required. Operator access makes this
     * relatively non-verbose.
     */
    operator fun get(botName: String): AbsSender

}