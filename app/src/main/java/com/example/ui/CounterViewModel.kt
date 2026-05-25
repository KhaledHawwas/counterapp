package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Counter
import com.example.data.CounterLog
import com.example.data.CounterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CounterViewModel(private val repository: CounterRepository) : ViewModel() {

    // List of counters
    val counters: StateFlow<List<Counter>> = repository.allCounters
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ID of currently selected counter
    private val _selectedCounterId = MutableStateFlow<Int?>(null)
    val selectedCounterId: StateFlow<Int?> = _selectedCounterId.asStateFlow()

    // Currently selected counter object
    val selectedCounter: StateFlow<Counter?> = combine(counters, _selectedCounterId) { list, id ->
        if (id != null) {
            list.find { it.id == id } ?: list.firstOrNull()
        } else {
            list.firstOrNull()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Log tracking for the selected counter
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedCounterLogs: StateFlow<List<CounterLog>> = _selectedCounterId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getLogsForCounter(id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allLogs: StateFlow<List<CounterLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Initialize default counter if database is empty
        viewModelScope.launch {
            val currentCounters = repository.allCounters.first()
            if (currentCounters.isEmpty()) {
                val defaultId = repository.insertCounter(
                    Counter(
                        name = "Tally Counter",
                        count = 0,
                        incrementStep = 1,
                        decrementStep = 1,
                        initialValue = 0,
                        colorHex = "#4F46E5" // Modern indigo
                    )
                )
                _selectedCounterId.value = defaultId.toInt()
            } else {
                _selectedCounterId.value = currentCounters.first().id
            }
        }
    }

    fun selectCounter(id: Int) {
        _selectedCounterId.value = id
    }

    fun createCounter(
        name: String,
        initialValue: Int,
        incrementStep: Int,
        decrementStep: Int,
        colorHex: String
    ) {
        viewModelScope.launch {
            val newId = repository.insertCounter(
                Counter(
                    name = name.trim().ifEmpty { "New Counter" },
                    count = initialValue,
                    initialValue = initialValue,
                    incrementStep = if (incrementStep > 0) incrementStep else 1,
                    decrementStep = if (decrementStep > 0) decrementStep else 1,
                    colorHex = colorHex,
                    lastUpdated = System.currentTimeMillis()
                )
            )
            _selectedCounterId.value = newId.toInt()
        }
    }

    fun deleteSelectedCounter() {
        val currentId = _selectedCounterId.value ?: return
        viewModelScope.launch {
            repository.deleteCounterById(currentId)
            val currentList = repository.allCounters.first()
            if (currentList.isNotEmpty()) {
                _selectedCounterId.value = currentList.first().id
            } else {
                _selectedCounterId.value = null
            }
        }
    }

    fun increment() {
        val currentId = _selectedCounterId.value ?: return
        viewModelScope.launch {
            repository.incrementCount(currentId)
        }
    }

    fun decrement() {
        val currentId = _selectedCounterId.value ?: return
        viewModelScope.launch {
            repository.decrementCount(currentId)
        }
    }

    fun reset() {
        val currentId = _selectedCounterId.value ?: return
        viewModelScope.launch {
            repository.resetCount(currentId)
        }
    }

    fun updateCountDirectly(newValue: Int) {
        val currentId = _selectedCounterId.value ?: return
        viewModelScope.launch {
            repository.updateCustomCount(currentId, newValue, "Manually updated value")
        }
    }

    fun updateSelectedCounterDetails(
        name: String,
        incrementStep: Int,
        decrementStep: Int,
        colorHex: String
    ) {
        val currentCounter = selectedCounter.value ?: return
        viewModelScope.launch {
            val updated = currentCounter.copy(
                name = name.trim().ifEmpty { currentCounter.name },
                incrementStep = if (incrementStep > 0) incrementStep else 1,
                decrementStep = if (decrementStep > 0) decrementStep else 1,
                colorHex = colorHex,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateCounter(updated)
        }
    }
}

class CounterViewModelFactory(private val repository: CounterRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CounterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CounterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
