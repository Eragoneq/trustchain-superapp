package nl.tudelft.trustchain.smartphonedemocracy.ui.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

data class NavigationItem(val route: Route, val icon: ImageVector)

@Serializable
sealed class Route(val name: String) {
    @Serializable
    data object Home : Route("Home")

    @Serializable
    data object Elections : Route("Elections")

    @Serializable
    data object Results : Route("Results")

    @Serializable
    data class ElectionDetail(val electionId: String) : Route("ElectionDetail")
}


@Composable
fun AppNavigation(
    navController: NavHostController,
    innerPadding: PaddingValues,
    showDebugActions: Boolean
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable<Route.Home> {
            HomeScreen(votingViewModel = hiltViewModel(), showDebugActions = showDebugActions)
        }
        composable<Route.Elections> {
            ElectionListScreen(viewModel = hiltViewModel(), onNavigateToDetail = { id ->
                navController.navigate(Route.ElectionDetail(id.toString()))
            })
        }
        composable<Route.Results> {
            ResultListScreen(
                navController = navController,
                votingViewModel = hiltViewModel()
            )
        }
        //TODO: Possible use a dialog instead of a composable
        composable<Route.ElectionDetail> { backStackEntry ->
            val details: Route.ElectionDetail = backStackEntry.toRoute()
            ElectionDetailScreen(
                electionId = details.electionId,
                votingViewModel = hiltViewModel()
            )
        }
    }
}

@Preview
@Composable
private fun NavigationPreview() {
    AppNavigation(
        navController = rememberNavController(),
        innerPadding = PaddingValues(),
        showDebugActions = false
    )
}
