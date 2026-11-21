package com.example.mobilelab01.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

object DateValidation {

    const val DATE_PATTERN = "dd.MM.yyyy"
    const val DATE_HINT = "дд.мм.гггг"

    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT)

    fun isValid(date: String): Boolean {
        if (date.isBlank()) return false
        return try {
            LocalDate.parse(date.trim(), formatter)
            true
        } catch (_: DateTimeParseException) {
            false
        }
    }
}
