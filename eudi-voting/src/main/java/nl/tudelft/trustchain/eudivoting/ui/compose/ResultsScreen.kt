package nl.tudelft.trustchain.eudivoting.ui.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import nl.tudelft.trustchain.eudivoting.data.Proposal
import nl.tudelft.trustchain.eudivoting.ui.VotingViewModel

@Composable
fun ResultListScreen(
    votingViewModel: VotingViewModel = hiltViewModel(),
    navController: NavController
) {
    LazyColumn {
        items(votingViewModel.fetchProposals()) { proposal ->
            ResultItem(proposal = proposal) {
                navController.navigate("resultDetail/${proposal.id}")
            }
        }
    }
}

@Composable
fun ResultItem(
    proposal: Proposal,
    onItemClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(8.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(8.dp)
    ) {
        Text(
            text = "Proposal ${proposal.id}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(text = proposal.description, fontStyle = FontStyle.Italic)
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Text(text = "Deadline: ${proposal.deadline}")
            Text(text = "Votes: ${proposal.minVotes}")
        }
    }
}

@Composable
fun ResultDetailScreen(proposal: Proposal) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Proposal ${proposal.id}",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold
        )
        Text(text = proposal.description, modifier = Modifier.padding(top = 8.dp))
        Text(text = "Deadline: ${proposal.deadline}", modifier = Modifier.padding(top = 8.dp))
        Text(text = "Votes: ${proposal.minVotes}", modifier = Modifier.padding(top = 8.dp))
    }
}
