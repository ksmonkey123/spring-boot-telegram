package ch.awae.telegram.spring.api

interface Principal {
    val userId: Long
    val roles: Array<String>
}
