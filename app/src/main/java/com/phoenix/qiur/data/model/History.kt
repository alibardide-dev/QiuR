package com.phoenix.qiur.data.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import java.util.Date
import java.util.UUID

@Entity
data class History(
    @PrimaryKey
    val uuid: UUID = UUID.randomUUID(),
    val raw: String,
    val format: String,
    val type: String,
    @ColumnInfo("created_at")
    val createdAt: Date,
    @ColumnInfo("updated_at")
    val updatedAt: Date,
)