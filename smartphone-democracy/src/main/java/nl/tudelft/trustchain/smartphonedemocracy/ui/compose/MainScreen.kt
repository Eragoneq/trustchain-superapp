package nl.tudelft.trustchain.smartphonedemocracy.ui.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import nl.tudelft.trustchain.smartphonedemocracy.ui.DebugViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingApp(
    navController: NavHostController = rememberNavController(),
) {
    val viewModel: DebugViewModel = hiltViewModel()
    val items = listOf(
        NavigationItem(Route.Home, Icons.Rounded.Home),
        NavigationItem(Route.Elections, Icons.Rounded.HowToVote),
        NavigationItem(Route.Results, Icons.Rounded.FormatListNumbered),
    )

    // State for debug actions visibility, hoisted to the main screen
    var showDebugActions by rememberSaveable { mutableStateOf(false) }
    // State for the overflow menu visibility
    var showMenu by rememberSaveable { mutableStateOf(false) }

    EudiVotingTheme {
        Scaffold(
            bottomBar = {
                BottomTabNavigation(items = items, navController = navController)
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { viewModel.debugCreateVoteBlock() }) {
                    Icon(imageVector = Icons.Rounded.HowToVote, contentDescription = null)
                }
            },
            topBar = {
                TopAppBar(
                    title = { Text("Smartphone Democracy") },
                    colors = TopAppBarDefaults.mediumTopAppBarColors(),
                    actions = {
                        IconButton(onClick = { showMenu = !showMenu }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Toggle debug"
                                    )
                                },
                                onClick = {
                                    showDebugActions = !showDebugActions
                                    showMenu = false // Close the menu
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Refresh cache database"
                                    )
                                },
                                onClick = {
                                    viewModel.debugRefreshCacheDatabase()
                                }
                            )
                        }

                    }
                )
            }
        )
        { innerPadding ->
            AppNavigation(
                navController = navController,
                innerPadding = innerPadding,
                showDebugActions = showDebugActions
            )
        }
    }
}

@Composable
fun BottomTabNavigation(items: List<NavigationItem>, navController: NavHostController) {
    var selectedTabIndex by rememberSaveable {
        mutableIntStateOf(0)
    }
    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedTabIndex == index,
                onClick = {
                    selectedTabIndex = index
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(imageVector = item.icon, contentDescription = null) },
                label = { Text(item.route.name) })
        }
    }
}

@Preview
@Composable
private fun AppView() {
    VotingApp()
}
