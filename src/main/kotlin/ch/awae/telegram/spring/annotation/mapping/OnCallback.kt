package ch.awae.telegram.spring.annotation.mapping

@Target(AnnotationTarget.FUNCTION)
@Repeatable
annotation class OnCallback(
        /**
         * Regular Expression that must match the data field of the callback
         *
         * Indexed and named groups can be used to extract parts of the data. These
         * can be passed to the function through String parameters with the @Group annotation.
         * Additionally, String parameters without any annotation are bound to a named group
         * with the same name as the parameter.
         *
         * @see ch.awae.telegram.spring.annotation.param.Group
         */
        val pattern: String,

        /**
         * by default, inline keyboards are removed once an answer has been given.
         * set this to 'true' in order to suppress this behaviour.
         */
        val keepKeyboard: Boolean = false,

        /**
         * handler selection priority (lower number means higher priority). default: 0
         *
         * If multiple handlers are matched, one is selected at ran. (Actually, within a @BotController bean they seem
         * to be selected alphabetically. It is unclear in what order the beans are selected.)
         * Use this field to control the priority of the handlers to control which one should be chosen.
         */
        val priority: Int = 0,
)