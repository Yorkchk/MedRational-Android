package com.example.medrational_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import coil.Coil
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.data.locale.AppLanguage
import com.example.medrational_android.data.settings.AppTheme
import com.example.medrational_android.data.rating.FileRatingStore
import com.example.medrational_android.data.push.WorkshopPush
import com.example.medrational_android.data.push.projectIdFromPush
import com.example.medrational_android.data.push.routeForPushTap
import com.example.medrational_android.data.push.workshopRoute
import com.example.medrational_android.ui.auth.ForgotPasswordScreen
import com.example.medrational_android.ui.auth.LoginScreen
import com.example.medrational_android.ui.auth.SignUpScreen
import com.example.medrational_android.ui.categories.CategoryListScreen
import com.example.medrational_android.ui.downloads.RecentDownloadsScreen
import com.example.medrational_android.ui.favorites.FavoritesScreen
import com.example.medrational_android.ui.novelties.NoveltiesScreen
import com.example.medrational_android.ui.profile.ChangeEmailScreen
import com.example.medrational_android.ui.profile.ChangePasswordScreen
import com.example.medrational_android.ui.profile.DeleteAccountScreen
import com.example.medrational_android.ui.profile.ProfileScreen
import com.example.medrational_android.ui.files.FileDetailScreen
import com.example.medrational_android.ui.reasonings.ReasoningDetailScreen
import com.example.medrational_android.ui.search.SearchScreen
import com.example.medrational_android.ui.statistics.StatisticsScreen
import com.example.medrational_android.ui.suggestions.NewSuggestionScreen
import com.example.medrational_android.ui.suggestions.SuggestionDetailScreen
import com.example.medrational_android.ui.suggestions.SuggestionListScreen
import com.example.medrational_android.ui.theme.SwisraTheme
import com.example.medrational_android.ui.welcome.WelcomeScreen
import com.example.medrational_android.ui.workshops.EditWorkshopScreen
import com.example.medrational_android.ui.workshops.NewWorkshopScreen
import com.example.medrational_android.ui.workshops.WorkshopDetailScreen
import com.example.medrational_android.ui.workshops.WorkshopListScreen
import com.example.medrational_android.viewmodel.AuthViewModel
import com.example.medrational_android.viewmodel.CategoryViewModel
import com.example.medrational_android.viewmodel.ChangeEmailViewModel
import com.example.medrational_android.viewmodel.ChangePasswordViewModel
import com.example.medrational_android.viewmodel.DeleteAccountViewModel
import com.example.medrational_android.viewmodel.ProfileViewModel
import com.example.medrational_android.viewmodel.FavoriteViewModel
import com.example.medrational_android.viewmodel.RecentDownloadsViewModel
import com.example.medrational_android.viewmodel.NewSuggestionViewModel
import com.example.medrational_android.viewmodel.NoveltiesViewModel
import com.example.medrational_android.viewmodel.StatisticsViewModel
import com.example.medrational_android.viewmodel.NoveltyBadgeViewModel
import com.example.medrational_android.viewmodel.ReasoningViewModel
import com.example.medrational_android.viewmodel.SuggestionDetailViewModel
import com.example.medrational_android.viewmodel.SuggestionListViewModel
import com.example.medrational_android.viewmodel.EditWorkshopViewModel
import com.example.medrational_android.viewmodel.NewWorkshopViewModel
import com.example.medrational_android.viewmodel.WorkshopDetailViewModel
import com.example.medrational_android.viewmodel.WorkshopListViewModel
import java.io.File

// AppCompatActivity applies the language picked in the profile (AppLanguage)
class MainActivity : AppCompatActivity() {

    // Project of a tapped workshop push, opened once the app is showing; consumed by MedRationalApp
    private var pushProjectId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Before super.onCreate, so the first frame already has the picked light or dark look
        AppTheme.apply(this)
        // Before super.onCreate: swaps the starting theme (logo on the brand background) for Theme.Swisra
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Screens draw behind the status and navigation bars; Scaffolds pad their content
        enableEdgeToEdge()
        AppLanguage.ensureDefault()
        ApiClient.initialize(this)
        WorkshopPush.ensureChannel(this)
        // Every installed app gets workshop pushes, signed in or not
        WorkshopPush.subscribe(this)
        // Not on a recreation: the push was already handled
        if (savedInstanceState == null) pushProjectId = intent.pushProjectId()
        // Animated GIFs; ImageDecoder also handles HEIF and animated WebP on Android 9+. The client
        // sends the token to our API, which workshop attachment images need
        Coil.setImageLoader(
            ImageLoader.Builder(applicationContext)
                .okHttpClient { ApiClient.imageHttpClient }
                .components {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
                }
                .build()
        )
        setContent {
            SwisraTheme {
                MedRationalApp(
                    pushProjectId = pushProjectId,
                    onPushProjectOpened = { pushProjectId = null }
                )
            }
        }
    }

    // A push tapped while the app is already open
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.pushProjectId()?.let { pushProjectId = it }
    }

    // Pushes put their data in the launch intent's extras, as strings
    private fun Intent.pushProjectId(): Long? =
        projectIdFromPush(mapOf(WorkshopPush.EXTRA_PROJECT_ID to getStringExtra(WorkshopPush.EXTRA_PROJECT_ID)))
}

@Composable
fun MedRationalApp(
    pushProjectId: Long? = null,
    onPushProjectOpened: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    // Android 13+ asks before an app may show notifications, workshop pushes included
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    val categoryViewModel: CategoryViewModel = viewModel()
    val reasoningViewModel: ReasoningViewModel = viewModel()

    // Determine initial destination: if already authenticated, jump to categories
    val startScreen = if (tokenManager.isLoggedIn()) "categories" else "welcome"

    // Asked at startup rather than after sign-in: signed-out phones get workshop pushes too
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // A push tapped while signed out, opened once the user signs in
    var pendingPushProjectId by rememberSaveable { mutableStateOf<Long?>(null) }

    // A tapped workshop push opens its project, through the login screen when signed out
    LaunchedEffect(pushProjectId) {
        val projectId = pushProjectId ?: return@LaunchedEffect
        val isLoggedIn = tokenManager.isLoggedIn()
        if (!isLoggedIn) pendingPushProjectId = projectId
        navController.navigate(routeForPushTap(projectId, isLoggedIn)) { launchSingleTop = true }
        onPushProjectOpened()
    }

    // Login can now be reached from anywhere (a push, or an expired session), so home replaces the
    // whole back stack
    val onSignedIn: () -> Unit = {
        navController.navigate("categories") { popUpTo(navController.graph.id) { inclusive = true } }
        pendingPushProjectId?.let { projectId ->
            pendingPushProjectId = null
            navController.navigate(workshopRoute(projectId))
        }
    }

    // Signing out from the menu, or after deleting the account
    val signOut: () -> Unit = {
        // So the session can't be renewed any more; reads the refresh token before it's cleared
        ApiClient.endSessionOnServer()
        tokenManager.clearToken()
        // The store holds this user's own scores
        FileRatingStore.shared.clear()
        // The phone stays subscribed to workshop pushes; tapping one leads to login
        navController.navigate("welcome") { popUpTo(navController.graph.id) { inclusive = true } }
    }

    NavHost(navController = navController, startDestination = startScreen) {

        // 1. Welcome Landing Screen (2 buttons: Log In & Sign Up)
        composable("welcome") {
            WelcomeScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onNavigateToSignUp = { navController.navigate("signup") }
            )
        }

        // 2. Sign Up Screen (New Users -> OTP -> return to Welcome)
        composable("signup") {
            val authViewModel: AuthViewModel = viewModel {
                AuthViewModel(tokenManager)
            }

            SignUpScreen(
                viewModel = authViewModel,
                onSignUpCompleted = {
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = "login") {
            val authViewModel: AuthViewModel = viewModel {
                AuthViewModel(tokenManager)
            }

            LoginScreen(
                viewModel = authViewModel,
                onNavigateToAdminDashboard = onSignedIn,
                onNavigateToUserDashboard = onSignedIn,
                onNavigateToForgotPassword = {
                    navController.navigate("forgot_password")
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = "forgot_password") {
            val authViewModel: AuthViewModel = viewModel {
                AuthViewModel(tokenManager)
            }

            ForgotPasswordScreen(
                viewModel = authViewModel,
                onPasswordResetCompleted = {
                    navController.popBackStack() // Returns to login screen once reset is complete
                },
                onBackClick = { navController.popBackStack() }
            )

        }

        composable("categories") {
            val noveltyBadgeViewModel: NoveltyBadgeViewModel = viewModel { NoveltyBadgeViewModel(ApiClient.api) }
            val noveltyCount by noveltyBadgeViewModel.unreadCount.collectAsState()
            // Runs each time home is shown, so the badge clears after visiting What's new.
            // Signed-out users have no baseline, and a 401/403 would log them out
            LaunchedEffect(Unit) {
                if (tokenManager.isLoggedIn()) noveltyBadgeViewModel.refresh()
            }

            CategoryListScreen(
                viewModel = categoryViewModel,
                onCategoryClick = { id, name -> navController.navigate("reasonings/$id/${Uri.encode(name)}") },
                onSignOut = signOut,
                onNavigateToSearch = { navController.navigate("search") },
                onNavigateToFavorites = { navController.navigate("favorites") }, // <-- Add this
                onNavigateToDownloads = { navController.navigate("downloads") },
                onNavigateToSuggestions = { navController.navigate("suggestions") },
                onNavigateToNovelties = { navController.navigate("novelties") },
                onNavigateToWorkshops = { navController.navigate("workshops") },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToStatistics = { navController.navigate("statistics") },
                noveltyCount = noveltyCount
            )
        }

        composable("profile") {
            val profileViewModel: ProfileViewModel = viewModel { ProfileViewModel(ApiClient.api, tokenManager) }

            ProfileScreen(
                viewModel = profileViewModel,
                onChangeEmailClick = { navController.navigate("profile/email") },
                onChangePasswordClick = { navController.navigate("profile/password") },
                onDeleteAccountClick = { navController.navigate("profile/delete") },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("profile/email") {
            val changeEmailViewModel: ChangeEmailViewModel = viewModel { ChangeEmailViewModel(ApiClient.api, tokenManager) }

            ChangeEmailScreen(
                viewModel = changeEmailViewModel,
                currentEmail = remember { tokenManager.getEmail() },
                onEmailChanged = {
                    Toast.makeText(context, R.string.profile_email_updated, Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("profile/password") {
            val changePasswordViewModel: ChangePasswordViewModel = viewModel { ChangePasswordViewModel(ApiClient.api, tokenManager) }

            ChangePasswordScreen(
                viewModel = changePasswordViewModel,
                email = remember { tokenManager.getEmail() },
                onPasswordChanged = {
                    Toast.makeText(context, R.string.profile_password_changed, Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("profile/delete") {
            val deleteAccountViewModel: DeleteAccountViewModel = viewModel { DeleteAccountViewModel(ApiClient.api) }

            DeleteAccountScreen(
                viewModel = deleteAccountViewModel,
                email = remember { tokenManager.getEmail() },
                onAccountDeleted = {
                    Toast.makeText(context, R.string.profile_account_deleted, Toast.LENGTH_LONG).show()
                    signOut()
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("novelties") {
            val noveltiesViewModel: NoveltiesViewModel = viewModel { NoveltiesViewModel(ApiClient.api) }

            NoveltiesScreen(
                viewModel = noveltiesViewModel,
                onFileClick = { reasoningId, fileId -> navController.navigate("file/$reasoningId/$fileId") },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Admin only: reached from the side menu, which only lists it for admins
        composable("statistics") {
            val statisticsViewModel: StatisticsViewModel = viewModel { StatisticsViewModel(ApiClient.api) }

            StatisticsScreen(
                viewModel = statisticsViewModel,
                onFileClick = { reasoningId, fileId -> navController.navigate("file/$reasoningId/$fileId") },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Suggestions are one-way: admins get the inbox, users get the form to send one
        composable("suggestions") {
            val isAdmin = remember { tokenManager.isAdmin() }
            if (isAdmin) {
                val suggestionListViewModel: SuggestionListViewModel = viewModel {
                    SuggestionListViewModel(ApiClient.api)
                }

                SuggestionListScreen(
                    viewModel = suggestionListViewModel,
                    onSuggestionClick = { id -> navController.navigate("suggestions/$id") },
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                val newSuggestionViewModel: NewSuggestionViewModel = viewModel { NewSuggestionViewModel(ApiClient.api) }

                NewSuggestionScreen(
                    viewModel = newSuggestionViewModel,
                    onDoneClick = { navController.popBackStack() },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        // Admin only
        composable(
            route = "suggestions/{suggestionId}",
            arguments = listOf(navArgument("suggestionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val context = LocalContext.current
            val suggestionDetailViewModel: SuggestionDetailViewModel = viewModel {
                SuggestionDetailViewModel(ApiClient.api, File(context.cacheDir, "previews"))
            }

            SuggestionDetailScreen(
                suggestionId = backStackEntry.arguments?.getLong("suggestionId") ?: 0L,
                viewModel = suggestionDetailViewModel,
                onDeleted = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("workshops") {
            val isAdmin = remember { tokenManager.isAdmin() }
            val workshopListViewModel: WorkshopListViewModel = viewModel {
                WorkshopListViewModel(ApiClient.api)
            }

            WorkshopListScreen(
                viewModel = workshopListViewModel,
                isAdmin = isAdmin,
                onProjectClick = { id -> navController.navigate("workshops/$id") },
                onNewProjectClick = { navController.navigate("workshops/new") },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Admin only: reached from the list's FAB
        composable("workshops/new") {
            val newWorkshopViewModel: NewWorkshopViewModel = viewModel {
                NewWorkshopViewModel(ApiClient.api, tokenManager.getUserId())
            }

            NewWorkshopScreen(
                viewModel = newWorkshopViewModel,
                // Open the new project so documents can be attached; back returns to the list
                onCreated = { id ->
                    navController.navigate("workshops/$id") { popUpTo("workshops/new") { inclusive = true } }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "workshops/{workshopId}",
            arguments = listOf(navArgument("workshopId") { type = NavType.LongType })
        ) { backStackEntry ->
            val isAdmin = remember { tokenManager.isAdmin() }
            val workshopId = backStackEntry.arguments?.getLong("workshopId") ?: 0L
            val workshopDetailViewModel: WorkshopDetailViewModel = viewModel {
                WorkshopDetailViewModel(ApiClient.api)
            }

            WorkshopDetailScreen(
                projectId = workshopId,
                viewModel = workshopDetailViewModel,
                isAdmin = isAdmin,
                onEditClick = { navController.navigate("workshops/$workshopId/edit") },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Admin only: reached from the project page's menu. Back on the project page, it reloads
        composable(
            route = "workshops/{workshopId}/edit",
            arguments = listOf(navArgument("workshopId") { type = NavType.LongType })
        ) { backStackEntry ->
            val editWorkshopViewModel: EditWorkshopViewModel = viewModel {
                EditWorkshopViewModel(ApiClient.api)
            }

            EditWorkshopScreen(
                projectId = backStackEntry.arguments?.getLong("workshopId") ?: 0L,
                viewModel = editWorkshopViewModel,
                onSaved = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("downloads") {
            val recentDownloadsViewModel: RecentDownloadsViewModel = viewModel {
                RecentDownloadsViewModel(ApiClient.api, tokenManager.getUserId())
            }

            RecentDownloadsScreen(
                viewModel = recentDownloadsViewModel,
                onFileClick = { reasoningId, fileId -> navController.navigate("file/$reasoningId/$fileId") },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("favorites") {
            val favoriteViewModel: FavoriteViewModel = viewModel {
                FavoriteViewModel(tokenManager)
            }

            FavoritesScreen(
                viewModel = favoriteViewModel,
                onNavigateToReasoning = { catId, catName ->
                    navController.navigate("reasonings/$catId/${Uri.encode(catName)}")
                },
                onFileClick = { reasoningId, fileId -> navController.navigate("file/$reasoningId/$fileId") },
                onBackClick = { navController.popBackStack() }
            )
        }

        // Search & Discovery Screen
        composable("search") {
            SearchScreen(
                onNavigateToReasoning = { categoryId, categoryName ->
                    navController.navigate("reasonings/$categoryId/${Uri.encode(categoryName)}")
                },
                onFileClick = { reasoningId, fileId -> navController.navigate("file/$reasoningId/$fileId") },
                onBackClick = { navController.popBackStack() }
            )
        }

        // 5. Reasoning & Files Detail Screen
        composable(
            route = "reasonings/{categoryId}/{categoryName}",
            arguments = listOf(
                navArgument("categoryId") { type = NavType.LongType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: 0L
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""

            ReasoningDetailScreen(
                categoryId = categoryId,
                categoryName = categoryName,
                viewModel = reasoningViewModel,
                onFileClick = { reasoningId, fileId -> navController.navigate("file/$reasoningId/$fileId") },
                onBackClick = { navController.popBackStack() }
            )
        }

        // 6. File page: title, reasoning context and document preview
        composable(
            route = "file/{reasoningId}/{fileId}",
            arguments = listOf(
                navArgument("reasoningId") { type = NavType.LongType },
                navArgument("fileId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            FileDetailScreen(
                reasoningId = backStackEntry.arguments?.getLong("reasoningId") ?: 0L,
                fileId = backStackEntry.arguments?.getLong("fileId") ?: 0L,
                onBackClick = { navController.popBackStack() }
            )
        }


    }
}