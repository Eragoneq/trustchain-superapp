package nl.tudelft.trustchain.smartphonedemocracy.ui.compose

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.tudelft.trustchain.smartphonedemocracy.data.models.Election
import nl.tudelft.trustchain.smartphonedemocracy.data.models.ElectionStatus
import nl.tudelft.trustchain.smartphonedemocracy.data.models.TallyInfo
import nl.tudelft.trustchain.smartphonedemocracy.data.models.Vote
import nl.tudelft.trustchain.smartphonedemocracy.data.models.VoteType
import nl.tudelft.trustchain.smartphonedemocracy.network.AuthenticationStatus
import nl.tudelft.trustchain.smartphonedemocracy.ui.DebugViewModel
import nl.tudelft.trustchain.smartphonedemocracy.ui.ElectionDetailViewModel
import nl.tudelft.trustchain.smartphonedemocracy.ui.ElectionListViewModel
import java.time.Instant
import java.util.UUID

@Composable
fun ElectionListScreen(
    viewModel: ElectionListViewModel,
    onNavigateToDetail: (electionId: UUID) -> Unit,
) {
    val upcomingElections by viewModel.upcomingElections.collectAsStateWithLifecycle()
    val activeElections by viewModel.activeElections.collectAsStateWithLifecycle()
    val closedElections by viewModel.closedElections.collectAsStateWithLifecycle()

    ElectionListScreenContent(
        upcomingElections = upcomingElections,
        activeElections = activeElections,
        closedElections = closedElections,
        onNavigateToDetail = onNavigateToDetail
    )
}

@Composable
fun ElectionListScreenContent(
    upcomingElections: List<Election>,
    activeElections: List<Election>,
    closedElections: List<Election>,
    onNavigateToDetail: (electionId: UUID) -> Unit
) {
    // State for each section's expanded/collapsed status
    var upcomingExpanded by rememberSaveable { mutableStateOf(true) }
    var activeExpanded by rememberSaveable { mutableStateOf(true) }
    var closedExpanded by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
    ) {
        LazyColumn {
            // Upcoming Elections Section
            item {
                SectionHeader(
                    title = "Upcoming Elections",
                    isExpanded = upcomingExpanded,
                    onHeaderClick = { upcomingExpanded = !upcomingExpanded }
                )
            }
            if (upcomingExpanded && upcomingElections.isNotEmpty()) {
                items(upcomingElections, key = { it.id }) { election ->
                    ElectionItem(
                        election = election,
                        onItemClick = { onNavigateToDetail(election.id) }
                    )
                }
            } else if (upcomingExpanded && upcomingElections.isEmpty()) {
                item {
                    Text(
                        text = "No upcoming elections.",
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                }
            }

            // Active Elections Section
            item {
                SectionHeader(
                    title = "Active Elections",
                    isExpanded = activeExpanded,
                    onHeaderClick = { activeExpanded = !activeExpanded }
                )
            }
            if (activeExpanded && activeElections.isNotEmpty()) {
                items(activeElections, key = { it.id }) { election ->
                    ElectionItem(
                        election = election,
                        onItemClick = { onNavigateToDetail(election.id) }
                    )
                }
            } else if (activeExpanded && activeElections.isEmpty()) {
                item {
                    Text(
                        text = "No active elections.",
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                }
            }

            // Closed Elections Section
            item {
                SectionHeader(
                    title = "Closed Elections",
                    isExpanded = closedExpanded,
                    onHeaderClick = { closedExpanded = !closedExpanded }
                )
            }
            if (closedExpanded && closedElections.isNotEmpty()) {
                items(closedElections, key = { it.id }) { election ->
                    ElectionItem(
                        election = election,
                        onItemClick = { onNavigateToDetail(election.id) }
                    )
                }
            } else if (closedExpanded && closedElections.isEmpty()) {
                item {
                    Text(
                        text = "No closed elections.",
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    isExpanded: Boolean,
    onHeaderClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onHeaderClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = if (isExpanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
            contentDescription = if (isExpanded) "Collapse section" else "Expand section"
        )
    }
}

@Composable
fun ElectionItem( // This Composable is already suitable for direct preview
    election: Election,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp), // Adjusted padding slightly
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = election.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = election.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    // Assuming you add a status field to your Election data class
                    text = "Status: ${election.status.name}", // Example: election.status.name
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Voters: ${election.voterCount}", // Assuming voterCount is in Election
                    style = MaterialTheme.typography.bodySmall
                )
                if (election.status == ElectionStatus.TALLIED) {
                    Text(
                        text = "Votes: ${election.voteCount}", // Assuming voteCount is in Election
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// Renamed for clarity, this is your stateful screen
@Composable
fun ElectionDetailScreen(
    electionId: String,
    votingViewModel: ElectionDetailViewModel,
) {
    val debugViewModel: DebugViewModel = hiltViewModel()
    val election by remember { votingViewModel.getElectionFlow(electionId) }.collectAsStateWithLifecycle()
    val authStatus by votingViewModel.authStatus.collectAsState()
    val context = LocalContext.current

    Log.d("ElectionDetailScreen", "Recomposing ElectionDetailScreen")

    // Handle loading state or if election is not found
    if (election == null) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Loading election details...")
        }
        return
    }

    ElectionDetailScreenContent(
        election = election!!,
        authStatus = authStatus[electionId] ?: AuthenticationStatus.PENDING,
        onAuthenticate = {
            votingViewModel.authenticateForElection(electionId) { uri ->
                val intent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(intent)
            }
        },
        onCastVote = { option ->
            votingViewModel.castVote(
                electionId = election!!.id.toString(),
                vote = option
            )
        },
        onDebugRegister = {
            debugViewModel.debugRegisterVoterBlock(election!!.id.toString())
        },
        onDebugVote = {
            debugViewModel.debugCreateVoteBlock(election!!.id.toString())
        }
    )
}

@Composable
fun ElectionDetailScreenContent(
    election: Election,
    authStatus: AuthenticationStatus,
    onAuthenticate: () -> Unit,
    onCastVote: (option: Vote) -> Unit,
    onDebugRegister: () -> Unit,
    onDebugVote: (option: String) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = election.name,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = election.description,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.padding(top = 16.dp))
        Text(
            text = "Start Date: ${election.startDate}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "End Date: ${election.endDate}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Status: ${election.status.name}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Registered Voters: ${election.voterCount}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Amount of votes: ${election.voteCount}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.padding(top = 16.dp))

        // Authentication Section
        if (authStatus !is AuthenticationStatus.Verified) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Authentication Required",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text(
                        text = when (authStatus) {
                            AuthenticationStatus.PENDING -> "Please authenticate to participate in this election"
                            AuthenticationStatus.FAILED -> "Authentication failed. Please try again"
                            else -> "Authentication required"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Button(
                        onClick = onAuthenticate,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Authenticate with EUDI")
                    }
                }
            }
        } else {
            // Registration Confirmation Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Registration Complete",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text(
                        text = "You have been successfully registered for this election. You can now cast your vote when voting is open.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.padding(top = 16.dp))

        // Debug Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Debug Actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.padding(8.dp))
                Button(
                    onClick = onDebugRegister,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Debug: Register as Voter")
                }
                Spacer(modifier = Modifier.padding(4.dp))
                Button(
                    onClick = { onDebugVote("debug_vote") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Debug: Cast Test Vote")
                }
            }
        }

        Spacer(modifier = Modifier.padding(top = 16.dp))

        if (election.status == ElectionStatus.OPEN && authStatus is AuthenticationStatus.Verified) {
            Text(
                text = "Voting Options:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            when (election.type) {
                is VoteType.YesNo -> {
                    val yesNo = election.type
                    Button(
                        onClick = { onCastVote(yesNo.vote(true)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(yesNo.yesOptionName)
                    }

                    Button(
                        onClick = { onCastVote(yesNo.vote(false)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(yesNo.noOptionName)
                    }
                }
            }
        } else if (election.status == ElectionStatus.TALLIED) {
            Text(
                text = "Results:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Text(
                text = election.tally?.tallyResult ?: "No results available.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else if (election.status == ElectionStatus.OPEN && authStatus !is AuthenticationStatus.Verified) {
            Text(
                text = "Please authenticate to vote in this election.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else {
            Text(
                text = "Voting is not currently open for this election.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

// --- Previews ---

@Preview(showBackground = false)
@Composable
private fun ElectionListScreenContentPreview() {
    val sampleElections = listOf(
        Election(
            UUID.randomUUID(),
            "Presidential Election 2024",
            "This is a very long description that should ideally be truncated with ellipsis if it exceeds the available space.",
            Instant.now(),
            Instant.now(),
            VoteType.YesNo(
                yesOptionName = "Vote Yes",
                noOptionName = "Vote No",
            ),
            null,
            status = ElectionStatus.OPEN
        ),
        Election(
            UUID.randomUUID(),
            "Presidential Election 2024",
            "This is a very long description that should ideally be truncated with ellipsis if it exceeds the available space.",
            Instant.now(),
            Instant.now(),
            VoteType.YesNo(
                yesOptionName = "Vote Yes",
                noOptionName = "Vote No",
            ),
            null,
            status = ElectionStatus.ARCHIVED
        ),
        Election(
            UUID.randomUUID(),
            "Presidential Election 2024",
            "This is a very long description that should ideally be truncated with ellipsis if it exceeds the available space.",
            Instant.now(),
            Instant.now(),
            VoteType.YesNo(
                yesOptionName = "Vote Yes",
                noOptionName = "Vote No",
            ),
            null,
            status = ElectionStatus.UPCOMING
        ),
    )
    EudiVotingTheme {
        ElectionListScreenContent(
            upcomingElections = sampleElections.filter { it.status == ElectionStatus.UPCOMING },
            activeElections = sampleElections.filter { it.status == ElectionStatus.OPEN },
            closedElections = sampleElections.filter { it.status == ElectionStatus.CLOSED || it.status == ElectionStatus.TALLIED },
            onNavigateToDetail = {}
        )
    }
}

@Preview(showBackground = false, name = "Election Item Preview")
@Composable
private fun ElectionItemPreview() {
    EudiVotingTheme {
        ElectionItem(
            election = Election(
                id = UUID.randomUUID(),
                name = "Presidential Election 2024",
                description = "Vote for the next president. This is a very long description that should ideally be truncated with ellipsis if it exceeds the available space.",
                startDate = Instant.now(),
                endDate = Instant.now(),
                type = VoteType.YesNo(yesOptionName = "Vote Yes", noOptionName = "Vote No"),
                voterCount = 1250,
                voteCount = 980, // Only relevant if status indicates voting is over
                status = ElectionStatus.OPEN // Example status
            ),
            onItemClick = {}
        )
    }
}

@Preview(showBackground = false, device = "id:pixel_6")
@Composable
private fun ElectionDetailScreenContentPreview_Open() {
    val sampleElection = Election(
        id = UUID.randomUUID(),
        name = "Referendum on New Park",
        description = "This referendum decides whether the city council should allocate funds for the development of a new central park. The project includes green spaces, playgrounds, and a community garden.",
        startDate = Instant.now(),
        endDate = Instant.now(),
        type = VoteType.YesNo("Yes, build the park", "No, allocate funds elsewhere"),
        voterCount = 2345,
        status = ElectionStatus.OPEN
    )
    EudiVotingTheme {
        ElectionDetailScreenContent(
            election = sampleElection,
            authStatus = AuthenticationStatus.PENDING,
            onAuthenticate = {},
            onCastVote = {},
            onDebugRegister = {},
            onDebugVote = {}
        )
    }
}

@Preview(showBackground = false, device = "id:pixel_6")
@Composable
private fun ElectionDetailScreenContentPreview_Tallied() {
    val sampleElection = Election(
        id = UUID.randomUUID(),
        name = "Board Member Election Results",
        description = "Results for the annual election of new board members for the community association.",
        startDate = Instant.now(),
        endDate = Instant.now(),
        type = VoteType.YesNo(yesOptionName = "ALICE", noOptionName = "BOB"),
        voterCount = 780,
        voteCount = 650,
        status = ElectionStatus.TALLIED,
        tally = TallyInfo(
            electionId = "sample-election-2",
            tallyResult = "Alice Wonderland: 450 votes\nBob The Builder: 380 votes\n\nAlice Wonderland is elected.",
            zkpTallyProof = "dummy_zkp_tally_proof_data_string_for_preview"
        )
    )
    EudiVotingTheme {
        ElectionDetailScreenContent(
            election = sampleElection,
            authStatus = AuthenticationStatus.Verified("sample-nullifier"),
            onAuthenticate = {},
            onCastVote = {},
            onDebugRegister = {},
            onDebugVote = {}
        )
    }
}
