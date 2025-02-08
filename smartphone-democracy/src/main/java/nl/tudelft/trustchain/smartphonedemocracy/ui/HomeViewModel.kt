package nl.tudelft.trustchain.smartphonedemocracy.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.tudelft.ipv8.Peer
import nl.tudelft.trustchain.smartphonedemocracy.data.ElectionRepository
import nl.tudelft.trustchain.smartphonedemocracy.data.VoterRepository
import nl.tudelft.trustchain.smartphonedemocracy.ipv8.SmartphoneDemocracyCommunity
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val electionRepository: ElectionRepository,
    private val voterRepository: VoterRepository,
    private val community: SmartphoneDemocracyCommunity,
) : ViewModel() {

    private val TAG = "HomeViewModel"

    private val _communityUsers = MutableStateFlow<List<Peer>>(emptyList())
    val communityUsers: StateFlow<List<Peer>> = _communityUsers

    // StateFlow for block counts
    private val _blockCounts = MutableStateFlow(Pair(0L, 0))
    val blockCounts: StateFlow<Pair<Long, Int>> = _blockCounts.asStateFlow()

    val peerCount: StateFlow<Int> = communityUsers.map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    init {
        Log.d(TAG, "Initializing HomeViewModel")
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                updateBlockCounts()
                _communityUsers.value = community.getPeers()
            }
        }
    }

    // Function to update block counts
    private fun updateBlockCounts() {
        val blockCount = community.database.getBlockCount(null)
        val chainLength = community.getChainLength()
        _blockCounts.value = Pair(blockCount, chainLength)
//        Log.d(TAG, "Updated block counts: Total Blocks = $blockCount, Chain Length = $chainLength")
    }

    fun getStoredBlockCounts(): Pair<Long, Int> {
        val blockCount = community.database.getBlockCount(null)
        val chainLength = community.getChainLength()
        return Pair(blockCount, chainLength)
    }
}
