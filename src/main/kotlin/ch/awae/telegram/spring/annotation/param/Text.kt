package ch.awae.telegram.spring.annotation.param

/**
 * marks a parameter of a mapping function for value extraction of the message text or callback data
 *
 * This is simply an alias for '@Group()'
 *
 * @see Group
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Text
