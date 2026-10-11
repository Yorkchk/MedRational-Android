package com.example.medrational_android

import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
import com.example.medrational_android.ui.auth.ForgotPasswordScreen
import com.example.medrational_android.ui.auth.LoginScreen
import com.example.medrational_android.ui.auth.SignUpScreen
import com.example.medrational_android.ui.categories.CategoryListScreen
import com.example.medrational_android.ui.downloads.RecentDownloadsScreen
import com.example.medrational_android.ui.favorites.FavoritesScreen
import com.example.medrational_android.ui.novelties.NoveltiesScreen
import com.example.medrational_android.ui.files.FileDetailScreen
import com.example.medrational_android.ui.reasonings.ReasoningDetailScreen
import com.example.medrational_android.ui.search.SearchScreen
import com.example.medrational_android.ui.suggestions.NewSuggestionScreen
import com.example.medrational_android.ui.suggestions.SuggestionDetailScreen
import com.example.medrational_android.ui.suggestions.SuggestionListScreen
import com.example.medrational_android.ui.theme.MedRationalAndroidTheme
import com.example.medrational_android.ui.welcome.WelcomeScreen
import com.example.medrational_android.viewmodel.AuthViewModel
import com.example.medrational_android.viewmodel.CategoryViewModel
import com.example.medrational_android.viewmodel.FavoriteViewModel
import com.example.medrational_android.viewmodel.RecentDownloadsViewModel
import com.example.medrational_android.viewmodel.NewSuggestionViewModel
import com.example.medrational_android.viewmodel.NoveltiesViewModel
import com.example.medrational_android.viewmodel.NoveltyBadgeViewModel
import com.example.medrational_android.viewmodel.ReasoningViewModel
import com.example.medrational_android.viewmodel.SuggestionDetailViewModel
import com.example.medrational_android.viewmodel.SuggestionListViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.initialize(this)
        // Animated GIFs; ImageDecoder also handles HEIF and animated WebP on Android 9+
        Coil.setImageLoader(
            ImageLoader.Builder(applicationContext)
                .components {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
                }
                .build()
        )
        setContent {
            MedRationalAndroidTheme {
                MedRationalApp()
            }
        }
    }
}

@Composable
fun MedRationalApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    val categoryViewModel: CategoryViewModel = viewModel()
    val reasoningViewModel: ReasoningViewModel = viewModel()

    // Determine initial destination: if already authenticated, jump to categories
    val startScreen = if (tokenManager.isLoggedIn()) "categories" else "welcome"

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
                onNavigateToAdminDashboard = {
                    navController.navigate("categories") { popUpTo("welcome") { inclusive = true } }
                },
                onNavigateToUserDashboard = {
                    navController.navigate("categories") { popUpTo("welcome") { inclusive = true } }
                },
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
                onSignOut = {
                    tokenManager.clearToken()
                    navController.navigate("welcome") { popUpTo("categories") { inclusive = true } }
                },
                onNavigateToSearch = { navController.navigate("search") },
                onNavigateToFavorites = { navController.navigate("favorites") }, // <-- Add this
                onNavigateToDownloads = { navController.navigate("downloads") },
                onNavigateToSuggestions = { navController.navigate("suggestions") },
                onNavigateToNovelties = { navController.navigate("novelties") },
                noveltyCount = noveltyCount
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

        // Admins get the inbox of every user's suggestions; users get their own
        composable("suggestions") {
            val isAdmin = remember { tokenManager.isAdmin() }
            val suggestionListViewModel: SuggestionListViewModel = viewModel {
                SuggestionListViewModel(ApiClient.api, isAdmin)
            }

            SuggestionListScreen(
                viewModel = suggestionListViewModel,
                isAdmin = isAdmin,
                onSuggestionClick = { id -> navController.navigate("suggestions/$id") },
                onNewSuggestionClick = { navController.navigate("suggestions/new") },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("suggestions/new") {
            val newSuggestionViewModel: NewSuggestionViewModel = viewModel { NewSuggestionViewModel(ApiClient.api) }

            NewSuggestionScreen(
                viewModel = newSuggestionViewModel,
                // Replace the form with the new conversation so Back returns to the list
                onSent = { id ->
                    navController.navigate("suggestions/$id") { popUpTo("suggestions/new") { inclusive = true } }
                },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "suggestions/{suggestionId}",
            arguments = listOf(navArgument("suggestionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val suggestionDetailViewModel: SuggestionDetailViewModel = viewModel {
                SuggestionDetailViewModel(ApiClient.api)
            }

            SuggestionDetailScreen(
                suggestionId = backStackEntry.arguments?.getLong("suggestionId") ?: 0L,
                viewModel = suggestionDetailViewModel,
                isAdmin = remember { tokenManager.isAdmin() },
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