package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "counters")
data class Counter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val count: Int = 0,
    val incrementStep: Int = 1,
    val decrementStep: Int = 1,
    val initialValue: Int = 0,
    val colorHex: String = "#4F46E5",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
)
