package com.phoenix.qiur.data.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.phoenix.qiur.data.model.History

@Dao
interface HistoryDao {

    @Insert
    suspend fun insertEntry(history: History)

    @Update
    suspend fun updateEntry(history: History)

    @Delete
    suspend fun deleteEntry(history: History)

    @Query("DELETE FROM history")
    suspend fun deleteAll()

    @Query("SELECT *  FROM history")
    suspend fun getAllEntries(): List<History>

}