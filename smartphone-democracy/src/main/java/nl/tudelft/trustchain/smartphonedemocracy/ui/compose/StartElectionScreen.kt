package nl.tudelft.trustchain.smartphonedemocracy.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartElectionScreen(navController: NavController) {
    var electionTitle by rememberSaveable { mutableStateOf("") }
    var electionDescription by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Start New Election") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Create a New Election", style = typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = electionTitle,
                onValueChange = { electionTitle = it },
                label = { Text("Election Title") }
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = electionDescription,
                onValueChange = { electionDescription = it },
                label = { Text("Election Description") },
                minLines = 3
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                // TODO: Implement logic to create and send election to the network
                // For now, just navigate back or to a confirmation screen
                navController.popBackStack()
            }) {
                Text("Submit Election")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartElectionScreenPreview() {
    EudiVotingTheme {
        StartElectionScreen(rememberNavController())
    }
}
