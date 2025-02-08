package nl.tudelft.trustchain.smartphonedemocracy.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import nl.tudelft.trustchain.smartphonedemocracy.data.models.Election
import nl.tudelft.trustchain.smartphonedemocracy.data.models.ElectionStatus
import nl.tudelft.trustchain.smartphonedemocracy.ui.ElectionDetailViewModel
import nl.tudelft.trustchain.smartphonedemocracy.ui.ElectionListViewModel

@Composable
fun ResultListScreen(
    votingViewModel: ElectionListViewModel,
    navController: NavController
) {
    val closedElections by votingViewModel.closedElections.collectAsStateWithLifecycle()

    LazyColumn {
        items(closedElections) { election ->
            ResultItem(election = election) {
                navController.navigate("resultDetail/${election.id}")
            }
        }
    }
}

@Composable
fun ResultItem(
    election: Election,
    onItemClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(8.dp),
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
                    text = "Status: ${election.status}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Voters: ${election.voterCount}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (election.status == ElectionStatus.TALLIED) {
                    Text(
                        text = "Votes: ${election.voteCount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun ResultDetailScreen(
    electionId: String,
    votingViewModel: ElectionDetailViewModel = hiltViewModel()
) {
    val election by votingViewModel.getElectionFlow(electionId).collectAsStateWithLifecycle()

    if (election == null) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Loading election details...")
        }
        return
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = election!!.name,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = election!!.description,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Start Date: ${election!!.startDate}",
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "End Date: ${election!!.endDate}",
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Status: ${election!!.status}",
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Registered Voters: ${election!!.voterCount}",
            modifier = Modifier.padding(top = 8.dp)
        )

        if (election!!.status == ElectionStatus.TALLIED) {
            Text(
                text = "Results:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = election!!.tally?.tallyResult ?: "No results available",
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Total Votes: ${election!!.voteCount}",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

