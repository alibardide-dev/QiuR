package com.phoenix.qiur.data

import androidx.room3.ColumnTypeConverter
import java.util.Date
import java.util.UUID

object DateConverter {
    @ColumnTypeConverter
    fun fromTimestamp(value: Long?): Date? =
        value?.let { Date(it) }

    @ColumnTypeConverter
    fun dateToTimestamp(date: Date?): Long? =
        date?.time
}

object UUIDConverter {
    @ColumnTypeConverter
    fun fromUUID(uuid: UUID): String =
        uuid.toString()

    @ColumnTypeConverter
    fun uuidFromString(string: String): UUID =
        UUID.fromString(string)
}