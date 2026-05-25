package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CounterDao {
    @Query("SELECT * FROM counters ORDER BY lastUpdated DESC")
    fun getAllCounters(): Flow<List<Counter>>

    @Query("SELECT * FROM counters WHERE id = :id")
    suspend fun getCounterById(id: Int): Counter?

    @Query("SELECT * FROM counters WHERE id = :id")
    fun getCounterByIdFlow(id: Int): Flow<Counter?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounter(counter: Counter): Long

    @Update
    suspend fun updateCounter(counter: Counter)

    @Delete
    suspend fun deleteCounter(counter: Counter)

    @Query("DELETE FROM counters WHERE id = :id")
    suspend fun deleteCounterById(id: Int)

    // Log records
    @Query("SELECT * FROM counter_logs WHERE counterId = :counterId ORDER BY timestamp DESC")
    fun getLogsForCounter(counterId: Int): Flow<List<CounterLog>>

    @Query("SELECT * FROM counter_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<CounterLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CounterLog)

    @Query("DELETE FROM counter_logs WHERE counterId = :counterId")
    suspend fun deleteLogsForCounter(counterId: Int)
}
