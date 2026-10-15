package com.example.medrational_android.ui.categories

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.ui.components.BrandRow
import com.example.medrational_android.ui.theme.Spacing

enum class DrawerDestination { HOME, SEARCH, NOVELTIES, WORKSHOPS, FAVORITES, DOWNLOADS, SUGGESTIONS, STATISTICS, PROFILE, SIGN_OUT }

data class DrawerEntry(val destination: DrawerDestination, @StringRes val label: Int)

/** A labelled block of the side menu. The sign-out block has no label. */
data class DrawerGroup(@StringRes val title: Int?, val entries: List<DrawerEntry>)

/** Blocks of the home screen's side menu, top to bottom. Sign out is always last. Empty blocks are left out. */
fun homeDrawerGroups(isAdmin: Boolean, isLoggedIn: Boolean): List<DrawerGroup> = listOf(
    DrawerGroup(R.string.drawer_group_explore, buildList {
        add(DrawerEntry(DrawerDestination.HOME, R.string.drawer_home))
        add(DrawerEntry(DrawerDestination.SEARCH, R.string.drawer_search))
        // What's new is relative to the account's last visit, so it needs a signed-in user
        if (isLoggedIn) {
            add(DrawerEntry(DrawerDestination.NOVELTIES, R.string.drawer_novelties))
            // The workshops API needs a signed-in user
            add(DrawerEntry(DrawerDestination.WORKSHOPS, R.string.drawer_projects))
        }
    }),
    DrawerGroup(R.string.drawer_group_mine, buildList {
        add(DrawerEntry(DrawerDestination.FAVORITES, R.string.drawer_favorites))
        add(DrawerEntry(DrawerDestination.DOWNLOADS, R.string.drawer_downloads))
        // Suggestions and the profile are tied to an account, so they need a signed-in user
        if (isLoggedIn) {
            add(DrawerEntry(DrawerDestination.SUGGESTIONS, if (isAdmin) R.string.drawer_suggestions_inbox else R.string.drawer_suggestions))
            add(DrawerEntry(DrawerDestination.PROFILE, R.string.drawer_profile))
        }
    }),
    // The statistics endpoint is admin only, and a 403 would log anyone else out
    DrawerGroup(R.string.drawer_group_admin, buildList {
        if (isLoggedIn && isAdmin) add(DrawerEntry(DrawerDestination.STATISTICS, R.string.drawer_statistics))
    }),
    DrawerGroup(null, buildList {
        if (isLoggedIn) add(DrawerEntry(DrawerDestination.SIGN_OUT, R.string.home_sign_out))
    })
).filter { it.entries.isNotEmpty() }

/** Every entry of [homeDrawerGroups], in menu order. */
fun homeDrawerEntries(isAdmin: Boolean, isLoggedIn: Boolean): List<DrawerEntry> =
    homeDrawerGroups(isAdmin, isLoggedIn).flatMap { it.entries }

private fun DrawerDestination.icon(): ImageVector = when (this) {
    DrawerDestination.HOME -> Icons.Outlined.Home
    DrawerDestination.SEARCH -> Icons.Outlined.Search
    DrawerDestination.NOVELTIES -> Icons.Outlined.NewReleases
    DrawerDestination.WORKSHOPS -> Icons.Outlined.Event
    DrawerDestination.FAVORITES -> Icons.Outlined.FavoriteBorder
    DrawerDestination.DOWNLOADS -> Icons.Outlined.History
    DrawerDestination.SUGGESTIONS -> Icons.Outlined.Lightbulb
    DrawerDestination.STATISTICS -> Icons.Outlined.BarChart
    DrawerDestination.PROFILE -> Icons.Outlined.Person
    DrawerDestination.SIGN_OUT -> Icons.AutoMirrored.Outlined.Logout
}

@Composable
fun HomeDrawerContent(
    userName: String?,
    email: String?,
    isAdmin: Boolean,
    groups: List<DrawerGroup>,
    onEntryClick: (DrawerDestination) -> Unit,
    modifier: Modifier = Modifier,
    // Files uploaded since the last visit to What's new; shown on that entry when > 0
    noveltyCount: Long = 0
) {
    ModalDrawerSheet(modifier = modifier, drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            DrawerHeader(userName = userName, email = email, isAdmin = isAdmin)
            groups.forEach { group ->
                if (group.title != null) {
                    Text(
                        text = stringResource(group.title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 28.dp, end = 28.dp, top = Spacing.lg, bottom = Spacing.xs)
                    )
                } else {
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm, horizontal = 28.dp))
                }
                group.entries.forEach { entry ->
                    DrawerItem(entry = entry, noveltyCount = noveltyCount, onClick = { onEntryClick(entry.destination) })
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

@Composable
private fun DrawerItem(entry: DrawerEntry, noveltyCount: Long, onClick: () -> Unit) {
    val signOut = entry.destination == DrawerDestination.SIGN_OUT
    NavigationDrawerItem(
        label = { Text(stringResource(entry.label), style = MaterialTheme.typography.labelLarge) },
        icon = { Icon(entry.destination.icon(), contentDescription = null) },
        badge = if (entry.destination == DrawerDestination.NOVELTIES && noveltyCount > 0) {
            { Badge { Text(formatBadgeCount(noveltyCount)) } }
        } else {
            null
        },
        // The drawer only lives on the home screen
        selected = entry.destination == DrawerDestination.HOME,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = if (signOut) {
            NavigationDrawerItemDefaults.colors(
                unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                unselectedTextColor = MaterialTheme.colorScheme.error,
                unselectedIconColor = MaterialTheme.colorScheme.error
            )
        } else {
            NavigationDrawerItemDefaults.colors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                unselectedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        }
    )
}

// "99+" past two digits, like most apps' notification badges
fun formatBadgeCount(count: Long): String = if (count > 99) "99+" else count.toString()

@Composable
private fun DrawerHeader(userName: String?, email: String?, isAdmin: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = Spacing.xl)) {
        BrandRow()
        val name = userName?.takeIf { it.isNotBlank() } ?: email
        if (name != null) {
            Spacer(modifier = Modifier.height(Spacing.lg))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (email != null && email != name) {
            Text(
                text = email,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isAdmin) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Text(
                    stringResource(R.string.drawer_admin),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
                )
            }
        }
    }
}
