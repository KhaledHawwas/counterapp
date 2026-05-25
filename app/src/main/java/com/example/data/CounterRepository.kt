package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class CounterRepository(private val counterDao: CounterDao) {

    val allCounters: Flow<List<Counter>> = counterDao.getAllCounters()
    val allLogs: Flow<List<CounterLog>> = counterDao.getAllLogs()

    fun getLogsForCounter(counterId: Int): Flow<List<CounterLog>> = 
        counterDao.getLogsForCounter(counterId)

    suspend fun insertCounter(counter: Counter): Long {
        return counterDao.insertCounter(counter)
    }

    suspend fun updateCounter(counter: Counter) {
        counterDao.updateCounter(counter)
    }

    suspend fun deleteCounter(counter: Counter) {
        counterDao.deleteLogsForCounter(counter.id)
        counterDao.deleteCounter(counter)
    }

    suspend fun deleteCounterById(id: Int) {
        counterDao.deleteLogsForCounter(id)
        counterDao.deleteCounterById(id)
    }

    suspend fun incrementCount(counterId: Int, label: String? = null) {
        val counter = counterDao.getCounterById(counterId) ?: return
        val previousValue = counter.count
        val newValue = previousValue + counter.incrementStep
        val updatedCounter = counter.copy(
            count = newValue,
            lastUpdated = System.currentTimeMillis()
        )
        counterDao.updateCounter(updatedCounter)
        counterDao.insertLog(
            CounterLog(
                counterId = counterId,
                previousValue = previousValue,
                newValue = newValue,
                label = label ?: "Incremented (+${counter.incrementStep})"
            )
        )
    }

    suspend fun decrementCount(counterId: Int, label: String? = null) {
        val counter = counterDao.getCounterById(counterId) ?: return
        val previousValue = counter.count
        val newValue = previousValue - counter.decrementStep
        val updatedCounter = counter.copy(
            count = newValue,
            lastUpdated = System.currentTimeMillis()
        )
        counterDao.updateCounter(updatedCounter)
        counterDao.insertLog(
            CounterLog(
                counterId = counterId,
                previousValue = previousValue,
                newValue = newValue,
                label = label ?: "Decremented (-${counter.decrementStep})"
            )
        )
    }

    suspend fun resetCount(counterId: Int) {
        val counter = counterDao.getCounterById(counterId) ?: return
        val previousValue = counter.count
        val newValue = counter.initialValue
        val updatedCounter = counter.copy(
            count = newValue,
            lastUpdated = System.currentTimeMillis()
        )
        counterDao.updateCounter(updatedCounter)
        counterDao.insertLog(
            CounterLog(
                counterId = counterId,
                previousValue = previousValue,
                newValue = newValue,
                label = "Reset to initial value (${counter.initialValue})"
            )
        )
    }

    suspend fun updateCustomCount(counterId: Int, newValue: Int, label: String? = null) {
        val counter = counterDao.getCounterById(counterId) ?: return
        val previousValue = counter.count
        val updatedCounter = counter.copy(
            count = newValue,
            lastUpdated = System.currentTimeMillis()
        )
        counterDao.updateCounter(updatedCounter)
        counterDao.insertLog(
            CounterLog(
                counterId = counterId,
                previousValue = previousValue,
                newValue = newValue,
                label = label ?: "Set to $newValue"
            )
        )
    }
}
