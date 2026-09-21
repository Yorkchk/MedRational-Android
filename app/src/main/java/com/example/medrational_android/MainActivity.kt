package com.example.medrational_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.medrational_android.data.api.ApiClient
import com.example.medrational_android.data.auth.TokenManager
import com.example.medrational_android.ui.auth.ForgotPasswordScreen
import com.example.medrational_android.ui.auth.LoginScreen
import com.example.medrational_android.ui.auth.SignUpScreen
import com.example.medrational_android.ui.categories.CategoryListScreen
import com.example.medrational_android.ui.reasonings.ReasoningDetailScreen
import com.example.medrational_android.ui.search.SearchScreen
import com.example.medrational_android.ui.theme.MedRationalAndroidTheme
import com.example.medrational_android.ui.welcome.WelcomeScreen
import com.example.medrational_android.viewmodel.AuthViewModel
import com.example.medrational_android.viewmodel.CategoryViewModel
import com.example.medrational_android.viewmodel.ReasoningViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.initialize(this)
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

        // 4. Categories Main Screen
        composable("categories") {
            CategoryListScreen(
                viewModel = categoryViewModel,
                onCategoryClick = { id, name ->
                    navController.navigate("reasonings/$id/$name")
                },
                onSignOut = {
                    tokenManager.clearToken()
                    navController.navigate("welcome") {
                        popUpTo("categories") { inclusive = true }
                    }
                },
                onNavigateToSearch = {
                    navController.navigate("search")
                }
            )
        }

        // Search & Discovery Screen
        composable("search") {
            SearchScreen(
                onNavigateToReasoning = { categoryId, reasoningId ->
                    navController.navigate("reasonings/$categoryId/Details")
                },
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
                onBackClick = { navController.popBackStack() }
            )
        }


    }
}