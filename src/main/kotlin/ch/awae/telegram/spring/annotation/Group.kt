package ch.awae.telegram.spring.annotation

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Group(val name: String = "", val id : Int = 0)
