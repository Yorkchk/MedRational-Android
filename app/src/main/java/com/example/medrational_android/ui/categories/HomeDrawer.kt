package com.example.medrational_android.ui.categories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class DrawerDestination { HOME, SEARCH, NOVELTIES, FAVORITES, DOWNLOADS, SUGGESTIONS, SIGN_OUT }

data class DrawerEntry(val destination: DrawerDestination, val label: String)

/** Entries of the home screen's side menu, top to bottom. Sign out is always last. */
fun homeDrawerEntries(isAdmin: Boolean, isLoggedIn: Boolean): List<DrawerEntry> = buildList {
    add(DrawerEntry(DrawerDestination.HOME, "Home"))
    add(DrawerEntry(DrawerDestination.SEARCH, "Search"))
    // What's new is relative to the account's last visit, so it needs a signed-in user
    if (isLoggedIn) {
        add(DrawerEntry(DrawerDestination.NOVELTIES, "What's new"))
    }
    add(DrawerEntry(DrawerDestination.FAVORITES, "Favorites"))
    add(DrawerEntry(DrawerDestination.DOWNLOADS, "Recent downloads"))
    // Suggestions are tied to an account, so they need a signed-in user
    if (isLoggedIn) {
        add(DrawerEntry(DrawerDestination.SUGGESTIONS, if (isAdmin) "Suggestions inbox" else "Suggestions"))
        add(DrawerEntry(DrawerDestination.SIGN_OUT, "Sign out"))
    }
}

private fun DrawerDestination.icon(): ImageVector = when (this) {
    DrawerDestination.HOME -> Icons.Default.Home
    DrawerDestination.SEARCH -> Icons.Default.Search
    DrawerDestination.NOVELTIES -> Icons.Default.NewReleases
    DrawerDestination.FAVORITES -> Icons.Default.Favorite
    DrawerDestination.DOWNLOADS -> Icons.Default.History
    DrawerDestination.SUGGESTIONS -> Icons.Default.Lightbulb
    DrawerDestination.SIGN_OUT -> Icons.AutoMirrored.Filled.Logout
}

@Composable
fun HomeDrawerContent(
    userName: String?,
    email: String?,
    isAdmin: Boolean,
    entries: List<DrawerEntry>,
    onEntryClick: (DrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
    // Files uploaded since the last visit to What's new; shown on that entry when > 0
    noveltyCount: Long = 0
) {
    ModalDrawerSheet(modifier = modifier) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            DrawerHeader(userName = userName, email = email, isAdmin = isAdmin)
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            entries.forEach { entry ->
                val signOut = entry.destination == DrawerDestination.SIGN_OUT
                if (signOut) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 28.dp))
                }
                NavigationDrawerItem(
                    label = { Text(entry.label) },
                    icon = { Icon(entry.destination.icon(), contentDescription = null) },
                    badge = if (entry.destination == DrawerDestination.NOVELTIES && noveltyCount > 0) {
                        { Badge { Text(formatBadgeCount(noveltyCount)) } }
                    } else {
                        null
                    },
                    // The drawer only lives on the home screen
                    selected = entry.destination == DrawerDestination.HOME,
                    onClick = { onEntryClick(entry.destination) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    colors = if (signOut) {
                        NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.error,
                            unselectedIconColor = MaterialTheme.colorScheme.error
                        )
                    } else {
                        NavigationDrawerItemDefaults.colors()
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

// "99+" past two digits, like most apps' notification badges
fun formatBadgeCount(count: Long): String = if (count > 99) "99+" else count.toString()

@Composable
private fun DrawerHeader(userName: String?, email: String?, isAdmin: Boolean) {
    Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)) {
        Text(
            text = "MedRational",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        val name = userName?.takeIf { it.isNotBlank() } ?: email
        if (name != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (email != null && email != name) {
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isAdmin) {
            Spacer(modifier = Modifier.height(8.dp))
            SuggestionChip(onClick = {}, label = { Text("Admin") })
        }
    }
}
