package nl.tudelft.trustchain.eudivoting.ui.compose

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.HowToVote
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable

data class NavigationItem(val label: String, val route: Route, val icon: ImageVector)

sealed interface Route {
    @Serializable object Home : Route
    @Serializable object Proposals : Route
    @Serializable object Results : Route
}

val items = listOf(
    NavigationItem("Home", Route.Home, Icons.Rounded.Home),
    NavigationItem("Proposals", Route.Proposals, Icons.Rounded.HowToVote),
    NavigationItem("Results", Route.Results, Icons.Rounded.FormatListNumbered),
)

@Composable
fun HomeScreen(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Route.Home,
    ) {
        composable<Route.Home> { HomeScreen() }
        composable<Route.Proposals> { ProposalListScreen(navController = navController) }
        composable<Route.Results> { ResultListScreen(navController = navController) }
    }
    EudiVotingTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route.toString() } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Text("Hello, world!", modifier = Modifier.padding(innerPadding))
        }
    }
}

@Preview(device = "id:pixel_9")
@Composable
private fun HomeScreenPreview() {
    HomeScreen()
}
