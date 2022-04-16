package ch.awae.telegram.spring.api

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton

data class Button(
    val text: String,
    val data: String,
)

data class Keyboard(
    val message: String,
    val keys: List<Button>,
    val columns: Int = 1
) {
    constructor(message: String, vararg buttons: Button) : this(message, buttons.toList())
    constructor(message: String, columns: Int, vararg buttons: Button) : this(message, buttons.toList(), columns)

    fun buildMarkup(): InlineKeyboardMarkup = InlineKeyboardMarkup(
        keys.map { InlineKeyboardButton(it.text).apply { callbackData = it.data } }
            .chunked(columns))
}