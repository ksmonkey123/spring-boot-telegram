package ch.awae.telegram.spring.annotation.param

/**
 * marks a parameter of a mapping function for value extraction from the matching regex.
 *
 * OnMessage and OnCallback mappings use a regex matching the text or callback data respectively.
 * This annotation can be used to pass a captured group of the matched value to the function.
 *
 * This annotation can only be used on parameters of type 'String' or 'String?'
 *
 * - The default annotation without explicit parameter values passes the entire regex value to the function.
 * - If the 'name' is non-empty the corresponding named group is passed to the function. (or null if the group does not exist)
 * - If the 'name' is empty the 'id' is used to select a group by index (0 is the full value)
 *
 * When a group does not exist, null is passed. Because of this one must take care when defining a mapping function:
 * Any group that is not guaranteed to always be captured must be declared as nullable.
 *
 * Note: group annotations are optional on parameters for named groups: by default any parameter with type 'String'
 * or 'String?' without an annotation will be treated as a named group parameter with the group name corresponding
 * to the parameter name.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Group(val name: String = "", val id : Int = 0)
