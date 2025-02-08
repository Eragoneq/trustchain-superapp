package nl.tudelft.trustchain.smartphonedemocracy.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import nl.tudelft.trustchain.smartphonedemocracy.data.ElectionRepository
import nl.tudelft.trustchain.smartphonedemocracy.data.models.Election
import nl.tudelft.trustchain.smartphonedemocracy.data.models.ElectionStatus
import javax.inject.Inject

@HiltViewModel
class ElectionListViewModel @Inject constructor(
    private val electionRepository: ElectionRepository
) : ViewModel() {

    private val TAG = "ElectionListViewModel"

    private val _elections = MutableStateFlow<List<Election>>(emptyList())
    val elections: StateFlow<List<Election>> = _elections.asStateFlow()

    // Expose filtered elections by status
    val upcomingElections: StateFlow<List<Election>> = elections.map {
        it.filter { election -> election.status == ElectionStatus.UPCOMING }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeElections: StateFlow<List<Election>> = elections.map {
        it.filter { election -> election.status == ElectionStatus.OPEN }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val closedElections: StateFlow<List<Election>> = elections.map {
        it.filter { election -> election.status == ElectionStatus.CLOSED || election.status == ElectionStatus.TALLIED }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        Log.d(TAG, "Initializing ElectionListViewModel")
        refreshElections()
    }

    private fun refreshElections() {
        viewModelScope.launch {
            electionRepository.refreshElectionStatus()
            electionRepository.getAllElections().collect { elections ->
                _elections.value = elections
            }
        }
    }
}
