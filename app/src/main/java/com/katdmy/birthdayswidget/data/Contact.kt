package com.katdmy.birthdayswidget.data

import java.time.LocalDate

data class Contact(
    val id: Long,
    val lookupKey: String,
    val name: String,
    val ages: Int?,
    val eventName: String,
    val nextDate: LocalDate
)
