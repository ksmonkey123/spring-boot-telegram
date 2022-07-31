package ch.awae.telegram.spring.api

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.bots.AbsSender
import java.util.HashMap

class UpdateContext(
    val bot: AbsSender,
    val principal: Principal?,
    val update: Update,
) {
    private val parameterMap: MutableMap<String, Any> = HashMap()


    operator fun set(key: String, value: Any?) {
        when (value) {
            null -> parameterMap.remove(key)
            else -> parameterMap[key] = value
        }
    }

    operator fun get(key: String) : Any? = parameterMap[key]

    inline fun <reified T: Any> load(key : String) : T? = get(key) as? T
}