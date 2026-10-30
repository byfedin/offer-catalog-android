package dev.byfedin.offercatalog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import dev.byfedin.offercatalog.catalog.CatalogScreen
import dev.byfedin.offercatalog.catalog.CatalogViewModel
import dev.byfedin.offercatalog.data.FixtureOfferRepository
import dev.byfedin.offercatalog.data.OfferRepository
import dev.byfedin.offercatalog.detail.DetailScreen
import dev.byfedin.offercatalog.detail.DetailViewModel
import dev.byfedin.offercatalog.ui.CatalogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val repository = remember { FixtureOfferRepository() }
            CatalogTheme { OfferCatalogApp(repository) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfferCatalogApp(repository: OfferRepository) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val inDetails = entry?.destination?.route == "offer/{id}"
    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    if (inDetails)
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(
                                painterResource(R.drawable.ic_back),
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            Box(
                Modifier.widthIn(max = 680.dp)
                    .fillMaxSize()
                    .align(androidx.compose.ui.Alignment.TopCenter)
            ) {
                NavHost(navController = nav, startDestination = "catalog") {
                    composable("catalog") {
                        val model: CatalogViewModel =
                            viewModel(
                                factory =
                                    viewModelFactory {
                                        initializer {
                                            CatalogViewModel(repository, createSavedStateHandle())
                                        }
                                    }
                            )
                        val state by model.state.collectAsStateWithLifecycle()
                        CatalogScreen(
                            state,
                            model::setQuery,
                            model::setCategory,
                            model::resetFilters,
                            model::loadMore,
                            model::retry,
                        ) { id ->
                            nav.navigate("offer/${android.net.Uri.encode(id)}") {
                                launchSingleTop = true
                            }
                        }
                    }
                    composable(
                        "offer/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.StringType }),
                    ) { backStack ->
                        val id = backStack.arguments?.getString("id").orEmpty()
                        val model: DetailViewModel =
                            viewModel(
                                factory =
                                    viewModelFactory {
                                        initializer { DetailViewModel(repository, id) }
                                    }
                            )
                        val state by model.state.collectAsStateWithLifecycle()
                        DetailScreen(state, model::retry) { nav.popBackStack() }
                    }
                }
            }
        }
    }
}
