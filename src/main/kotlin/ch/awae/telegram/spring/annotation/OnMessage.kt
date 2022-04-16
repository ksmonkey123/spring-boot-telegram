package ch.awae.telegram.spring.annotation

/**
 * Marks a method of a BotController to be invoked for incoming messages.
 */
@Target(AnnotationTarget.FUNCTION)
@Repeatable
annotation class OnMessage(
        /**
         * Regular Expression that must match the text of the message.
         *
         * Indexed and named groups can be used to extract parts of the message. These
         * can be passed to the function through String parameters with the @Group annotation.
         * Additionally, String parameters without any annotation are bound to a named group
         * with the same name as the parameter.
         *
         * @see Group
         */
        val pattern: String,

        /**
         * set to 'true' if the result of the method should be sent as a marked response to the incoming message.
         * only applicable for result types that have custom serialization logic (e.g. String or Keyboard).
         * If a raw BotApiMessage is returned, this flag is ignored.
         */
        val linkResponse: Boolean = false,

        /**
         * handler selection priority (lower number means higher priority). default: 0
         *
         * If multiple handlers are matched, one is selected at ran. (Actually, within a @BotController bean they seem
         * to be selected alphabetically. It is unclear in what order the beans are selected.)
         * Use this field to control the priority of the handlers to control which one should be chosen.
         */
        val priority: Int = 0,
)
