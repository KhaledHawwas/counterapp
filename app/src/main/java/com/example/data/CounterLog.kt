package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counter_logs")
data class CounterLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val counterId: Int,
    val previousValue: Int,
    val newValue: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val label: String? = null
)
