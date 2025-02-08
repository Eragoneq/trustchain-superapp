package nl.tudelft.trustchain.smartphonedemocracy.ui.compose

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import nl.tudelft.trustchain.smartphonedemocracy.ui.DebugViewModel
import nl.tudelft.trustchain.smartphonedemocracy.ui.HomeViewModel

@Composable
fun HomeScreen(
    votingViewModel: HomeViewModel,
    showDebugActions: Boolean,
) {
    val debugViewModel: DebugViewModel = hiltViewModel()
    val peerCount by votingViewModel.peerCount.collectAsState()
    val blockCounts by votingViewModel.blockCounts.collectAsState()

    HomeScreenContent(
        peersSize = peerCount,
        totalBlocks = blockCounts.first,
        chainLength = blockCounts.second,
        showDebugActions = showDebugActions,
        onCreateElectionBlockClick = { debugViewModel.debugCreateElectionBlock() },
        onRegisterVoterBlockClick = { debugViewModel.debugRegisterVoterBlock() },
        onCreateVoteBlockClick = { debugViewModel.debugCreateVoteBlock() },
        onRefreshChainFromPeersClick = { debugViewModel.debugRefreshChainFromPeers() }
    )
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    peersSize: Int,
    totalBlocks: Long,
    chainLength: Int,
    showDebugActions: Boolean,
    onCreateElectionBlockClick: () -> Unit,
    onRegisterVoterBlockClick: () -> Unit,
    onCreateVoteBlockClick: () -> Unit,
    onRefreshChainFromPeersClick: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        item {
            Text("Welcome to Smartphone Democracy!", style = typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Please select a tab to continue.", style = typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("System Status", style = typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    ListItem(
                        headlineContent = { Text("Connected Peers") },
                        supportingContent = { Text("$peersSize peers connected") }
                    )
                    ListItem(
                        headlineContent = { Text("Blockchain Status") },
                        supportingContent = {
                            Text("Total Blocks: $totalBlocks\nChain Length: $chainLength")
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showDebugActions) {
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Debug Actions", style = typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onCreateElectionBlockClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create Election Block")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRegisterVoterBlockClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Register Voter Block")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onCreateVoteBlockClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Create Vote Block")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = onRefreshChainFromPeersClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Refresh Chain from Peers")
                        }
                    }
                }
            }
        }
    }
}

@Preview(device = "id:pixel_9", showBackground = true)
@Composable
private fun HomeScreenPreview() {
    EudiVotingTheme {
        Surface {
            HomeScreenContent(
                peersSize = 3,
                totalBlocks = 1000L,
                chainLength = 50,
                showDebugActions = false,
                onCreateElectionBlockClick = { Log.d("Preview", "Create Election Block Clicked") },
                onRegisterVoterBlockClick = { Log.d("Preview", "Register Voter Block Clicked") },
                onCreateVoteBlockClick = { Log.d("Preview", "Create Vote Block Clicked") },
                onRefreshChainFromPeersClick = {
                    Log.d(
                        "Preview",
                        "Refresh Chain from Peers Clicked"
                    )
                }
            )
        }
    }
}

@Preview(device = "id:pixel_9", showBackground = true, name = "HomeScreen Preview (Debug Visible)")
@Composable
private fun HomeScreenPreviewDebugVisible() {
    EudiVotingTheme {
        Surface {
            HomeScreenContent(
                peersSize = 3,
                totalBlocks = 1000L,
                chainLength = 50,
                showDebugActions = true,
                onCreateElectionBlockClick = { Log.d("Preview", "Create Election Block Clicked") },
                onRegisterVoterBlockClick = { Log.d("Preview", "Register Voter Block Clicked") },
                onCreateVoteBlockClick = { Log.d("Preview", "Create Vote Block Clicked") },
                onRefreshChainFromPeersClick = {
                    Log.d(
                        "Preview",
                        "Refresh Chain from Peers Clicked"
                    )
                }
            )
        }
    }
}
