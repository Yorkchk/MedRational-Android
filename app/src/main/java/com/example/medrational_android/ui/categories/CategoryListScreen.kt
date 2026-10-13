package com.example.medrational_android.ui.categories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.model.Category
import com.example.medrational_android.ui.text.asString
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

    var showCreateDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
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
                entries = homeDrawerEntries(isAdmin = isAdmin, isLoggedIn = tokenManager.isLoggedIn()),
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
                        DrawerDestination.PROFILE -> onNavigateToProfile()
                        DrawerDestination.SIGN_OUT -> showSignOutDialog = true
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.categories_title), fontWeight = FontWeight.Bold) },
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
                                            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSearch) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.categories_search_files))
                        }
                    }
                )
            },
            floatingActionButton = {
                if (isAdmin) {
                    FloatingActionButton(onClick = { showCreateDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.categories_add))
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (val state = uiState) {
                    is CategoryUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    is CategoryUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = state.message.asString(), color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { viewModel.loadCategories() }) { Text(stringResource(R.string.common_retry)) }
                        }
                    }
                    is CategoryUiState.Success -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.categories, key = { it.id }) { category ->
                                CategoryCard(
                                    category = category,
                                    isAdmin = isAdmin,
                                    onClick = { onCategoryClick(category.id, category.name) },
                                    onDelete = { viewModel.deleteCategory(category.id) }
                                )
                            }
                        }
                    }
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
fun CategoryCard(
    category: Category,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!category.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isAdmin) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.categories_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.categories_name)) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text(stringResource(R.string.categories_description_optional)) }
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