package com.phoenix.qiur.data

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.phoenix.qiur.data.dao.HistoryDao
import com.phoenix.qiur.data.model.History
import com.phoenix.qiur.utils.DatabaseConstants

@Database(
    entities = [History::class],
    version = DatabaseConstants.DB_VERSION,
    exportSchema = true,
)
@ColumnTypeConverters(
    DateConverter::class,
    UUIDConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}