package com.example.medrational_android.ui.categories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.model.Category
import com.example.medrational_android.ui.components.ContentWidth
import com.example.medrational_android.ui.components.EmptyState
import com.example.medrational_android.ui.components.ErrorState
import com.example.medrational_android.ui.components.ListRowCard
import com.example.medrational_android.ui.components.LoadingState
import com.example.medrational_android.ui.components.MonogramTile
import com.example.medrational_android.ui.components.NoMatch
import com.example.medrational_android.ui.components.QuickSearchBar
import com.example.medrational_android.ui.components.SectionHeader
import com.example.medrational_android.ui.components.SwisraLogo
import com.example.medrational_android.ui.components.adaptiveHorizontalPadding
import com.example.medrational_android.ui.components.greetingName
import com.example.medrational_android.ui.components.listColumnsFor
import com.example.medrational_android.ui.components.windowWidthDp
import com.example.medrational_android.ui.search.filterCategories
import com.example.medrational_android.ui.theme.Spacing
import com.example.medrational_android.viewmodel.CategoryUiState
import com.example.medrational_android.viewmodel.CategoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    viewModel: CategoryViewModel,
    onCategoryClick: (Long, String) -> Unit,
    onSignOut: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToSuggestions: () -> Unit,
    onNavigateToNovelties: () -> Unit,
    onNavigateToWorkshops: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    // Opens the full search with what was typed in the home search bar
    onAdvancedSearch: (query: String) -> Unit = { onNavigateToSearch() },
    // Files uploaded since the last visit to What's new
    noveltyCount: Long = 0
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    val isAdmin = remember {
        val role = tokenManager.getRole() ?: ""
        role.contains("ADMIN", ignoreCase = true)
    }
    val isLoggedIn = remember { tokenManager.isLoggedIn() }
    val firstName = remember { greetingName(tokenManager.getUserName()) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // Back closes the side menu instead of leaving the app
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeDrawerContent(
                userName = tokenManager.getUserName(),
                email = tokenManager.getEmail(),
                isAdmin = isAdmin,
                groups = homeDrawerGroups(isAdmin = isAdmin, isLoggedIn = isLoggedIn),
                noveltyCount = noveltyCount,
                onEntryClick = { destination ->
                    // Close first so the menu isn't left open when coming back to home
                    scope.launch { drawerState.close() }
                    when (destination) {
                        DrawerDestination.HOME -> Unit
                        DrawerDestination.SEARCH -> onNavigateToSearch()
                        DrawerDestination.NOVELTIES -> onNavigateToNovelties()
                        DrawerDestination.WORKSHOPS -> onNavigateToWorkshops()
                        DrawerDestination.FAVORITES -> onNavigateToFavorites()
                        DrawerDestination.DOWNLOADS -> onNavigateToDownloads()
                        DrawerDestination.SUGGESTIONS -> onNavigateToSuggestions()
                        DrawerDestination.STATISTICS -> onNavigateToStatistics()
                        DrawerDestination.PROFILE -> onNavigateToProfile()
                        DrawerDestination.SIGN_OUT -> showSignOutDialog = true
                    }
                }
            )
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SwisraLogo(size = 30.dp)
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            // A dot tells the user there is something new behind the menu. It sits on the
                            // icon's corner, inside the button: BadgedBox put it past the button's edge,
                            // where the title is drawn over it
                            Box {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = if (noveltyCount > 0) stringResource(R.string.home_open_menu_new) else stringResource(R.string.home_open_menu)
                                )
                                if (noveltyCount > 0) {
                                    Badge(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 4.dp, y = (-4).dp)
                                            // Default 6dp dot is easy to miss
                                            .size(10.dp)
                                            // Ring in the bar's color so the dot stands out from the ☰ lines
                                            .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            floatingActionButton = {
                if (isAdmin) {
                    ExtendedFloatingActionButton(
                        onClick = { showCreateDialog = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.categories_new)) }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val state = uiState) {
                    is CategoryUiState.Loading -> LoadingState()
                    is CategoryUiState.Error -> ErrorState(message = state.message, onRetry = { viewModel.loadCategories() })
                    is CategoryUiState.Success -> HomeContent(
                        categories = state.categories,
                        firstName = firstName,
                        noveltyCount = if (isLoggedIn) noveltyCount else 0,
                        isAdmin = isAdmin,
                        onAdvancedSearch = onAdvancedSearch,
                        onNoveltiesClick = onNavigateToNovelties,
                        onCategoryClick = onCategoryClick,
                        onDeleteClick = { categoryToDelete = it }
                    )
                }
            }
        }
    }
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(stringResource(R.string.home_sign_out)) },
            text = { Text(stringResource(R.string.home_sign_out_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        tokenManager.clearToken()
                        showSignOutDialog = false
                        onSignOut()
                    }
                ) {
                    Text(stringResource(R.string.home_sign_out), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    // Deleting a category removes all its reasonings and files, so it's never one tap away
    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text(stringResource(R.string.categories_delete)) },
            text = { Text(stringResource(R.string.categories_delete_confirm, category.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(category.id)
                        categoryToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    if (showCreateDialog) {
        CreateCategoryDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, desc ->
                viewModel.createCategory(name, desc)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun HomeContent(
    categories: List<Category>,
    firstName: String?,
    noveltyCount: Long,
    isAdmin: Boolean,
    onAdvancedSearch: (query: String) -> Unit,
    onNoveltiesClick: () -> Unit,
    onCategoryClick: (Long, String) -> Unit,
    onDeleteClick: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    // Centered on very wide screens, so the tiles don't spread into many small columns
    val sidePadding = adaptiveHorizontalPadding(ContentWidth.Grid)
    var query by rememberSaveable { mutableStateOf("") }
    val searching = query.isNotBlank()
    val shown = remember(categories, query) { filterCategories(categories, query) }
    Column(modifier = modifier.fillMaxSize()) {
        // Above the list, so it stays at hand however far the categories go
        QuickSearchBar(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.home_search_categories),
            onAdvancedSearch = { onAdvancedSearch(query.trim()) },
            modifier = Modifier.padding(start = sidePadding, end = sidePadding, top = Spacing.xs, bottom = Spacing.sm)
        )
        LazyVerticalGrid(
            // One category per line on phones, two on tablets
            columns = GridCells.Fixed(listColumnsFor(windowWidthDp())),
            modifier = Modifier.weight(1f),
            // Room at the bottom so the admin button never covers the last cards
            contentPadding = PaddingValues(start = sidePadding, end = sidePadding, top = Spacing.xs, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // The greeting and novelties step aside while searching, so the results come first
            if (!searching) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Text(
                            text = if (firstName != null) stringResource(R.string.home_greeting, firstName) else stringResource(R.string.home_greeting_anonymous),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = stringResource(R.string.home_greeting_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (noveltyCount > 0 && !searching) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    NoveltyBanner(count = noveltyCount, onClick = onNoveltiesClick)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader(title = stringResource(R.string.categories_title))
            }
            if (categories.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyState(
                        title = stringResource(R.string.categories_empty),
                        hint = if (isAdmin) stringResource(R.string.categories_empty_admin_hint) else null,
                        icon = Icons.Outlined.MedicalServices,
                        modifier = Modifier.heightIn(min = 240.dp)
                    )
                }
            }
            if (searching && categories.isNotEmpty() && shown.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    NoMatch(
                        message = stringResource(R.string.categories_no_match, query.trim()),
                        onAdvancedSearch = { onAdvancedSearch(query.trim()) }
                    )
                }
            }
            items(shown, key = { it.id }) { category ->
                CategoryRow(
                    category = category,
                    isAdmin = isAdmin,
                    onClick = { onCategoryClick(category.id, category.name) },
                    onDeleteClick = { onDeleteClick(category) }
                )
            }
        }
    }
}

@Composable
private fun NoveltyBanner(count: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.NewReleases, contentDescription = null)
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.drawer_novelties), style = MaterialTheme.typography.titleSmall)
                Text(
                    pluralStringResource(R.plurals.novelties_new_count, count.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), count),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

/** A category on the home screen: its name with the description underneath, so it can be read. */
@Composable
internal fun CategoryRow(
    category: Category,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListRowCard(
        title = category.name,
        subtitle = category.description,
        subtitleMaxLines = DESCRIPTION_MAX_LINES,
        leading = { MonogramTile(name = category.name, key = category.id, fallback = Icons.Outlined.MedicalServices) },
        trailing = {
            if (isAdmin) {
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.categories_delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        onClick = onClick,
        modifier = modifier
    )
}

// Long descriptions end with "…" here; the category's page shows them in full
private const val DESCRIPTION_MAX_LINES = 6

@Composable
fun CreateCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.categories_new)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.categories_name)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(stringResource(R.string.categories_description_optional)) },
                    shape = MaterialTheme.shapes.small
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, desc.ifBlank { null }) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.common_create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        }
    )
}
