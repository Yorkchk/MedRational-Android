package com.example.medrational_android.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.medrational_android.R
import com.example.medrational_android.ui.components.ErrorState
import com.example.medrational_android.ui.components.LoadingState
import com.example.medrational_android.data.locale.AppLanguage
import com.example.medrational_android.data.settings.AppTheme
import com.example.medrational_android.data.model.UserProfile
import com.example.medrational_android.ui.text.asString
import com.example.medrational_android.viewmodel.ProfileSaveState
import com.example.medrational_android.viewmodel.ProfileUiState
import com.example.medrational_android.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onChangeEmailClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val saveState by viewModel.saveState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isEditing by rememberSaveable { mutableStateOf(false) }

    // Reloads each time the screen is shown, so a changed email appears after coming back
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val profileUpdated = stringResource(R.string.profile_updated)
    LaunchedEffect(saveState) {
        if (saveState is ProfileSaveState.Saved) {
            isEditing = false
            viewModel.clearSaveState()
            snackbarHostState.showSnackbar(profileUpdated)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    if (uiState is ProfileUiState.Success && !isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.profile_edit))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> LoadingState()
                is ProfileUiState.Error -> {
                    ErrorState(message = state.message, onRetry = viewModel::load)
                }
                is ProfileUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isEditing) {
                            EditProfileCard(
                                profile = state.profile,
                                saveState = saveState,
                                onSave = viewModel::save,
                                onCancel = {
                                    viewModel.clearSaveState()
                                    isEditing = false
                                }
                            )
                        } else {
                            ProfileInfoCard(profile = state.profile)
                        }
                        SecurityCard(
                            onChangeEmailClick = onChangeEmailClick,
                            onChangePasswordClick = onChangePasswordClick,
                            onDeleteAccountClick = onDeleteAccountClick
                        )
                        LanguageCard()
                        ThemeCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileInfoCard(profile: UserProfile, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileField(label = stringResource(R.string.profile_first_name), value = profile.firstName)
            ProfileField(label = stringResource(R.string.profile_last_name), value = profile.lastName)
            ProfileField(label = stringResource(R.string.profile_email), value = profile.email)
            ProfileField(label = stringResource(R.string.profile_phone), value = profile.phoneNumber)
            if (profile.role.contains("ADMIN", ignoreCase = true)) {
                SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.profile_admin)) })
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_not_set),
            style = MaterialTheme.typography.bodyLarge,
            color = if (value.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EditProfileCard(
    profile: UserProfile,
    saveState: ProfileSaveState,
    onSave: (firstName: String, lastName: String, phone: String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var firstName by rememberSaveable { mutableStateOf(profile.firstName.orEmpty()) }
    var lastName by rememberSaveable { mutableStateOf(profile.lastName.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(profile.phoneNumber.orEmpty()) }
    val isSaving = saveState is ProfileSaveState.Saving

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = firstName,
                onValueChange = { if (it.length <= 50) firstName = it },
                label = { Text(stringResource(R.string.profile_first_name)) },
                singleLine = true,
                enabled = !isSaving,
                supportingText = { Text(stringResource(R.string.common_char_count, firstName.length, 50)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = lastName,
                onValueChange = { if (it.length <= 50) lastName = it },
                label = { Text(stringResource(R.string.profile_last_name)) },
                singleLine = true,
                enabled = !isSaving,
                supportingText = { Text(stringResource(R.string.common_char_count, lastName.length, 50)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { if (it.length <= 10 && it.all { c -> c.isDigit() }) phone = it },
                label = { Text(stringResource(R.string.profile_phone_10_digits)) },
                singleLine = true,
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )
            if (saveState is ProfileSaveState.Error) {
                Text(saveState.message.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCancel, enabled = !isSaving, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.common_cancel))
                }
                Button(
                    onClick = { onSave(firstName, lastName, phone) },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.common_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityCard(
    onChangeEmailClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            SecurityRow(
                icon = Icons.Default.Email,
                title = stringResource(R.string.profile_change_email),
                subtitle = stringResource(R.string.profile_change_email_hint),
                onClick = onChangeEmailClick
            )
            HorizontalDivider()
            SecurityRow(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.profile_change_password),
                subtitle = stringResource(R.string.profile_change_password_hint),
                onClick = onChangePasswordClick
            )
            HorizontalDivider()
            SecurityRow(
                icon = Icons.Default.DeleteForever,
                title = stringResource(R.string.profile_delete_account),
                subtitle = stringResource(R.string.profile_delete_account_hint),
                onClick = onDeleteAccountClick,
                iconTint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SecurityRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconTint)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** French or English for the app's own text. Picking one recreates the screen in that language. */
@Composable
private fun LanguageCard(modifier: Modifier = Modifier) {
    val current = remember { AppLanguage.current() }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.profile_language), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.profile_language_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AppLanguage.entries.forEach { language ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = language == current,
                            onClick = { if (language != current) AppLanguage.set(language) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = language == current, onClick = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(language.nativeName, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** Light (the default), dark, or like the phone. Picking one recreates the screen in that look. */
@Composable
private fun ThemeCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val current = remember { AppTheme.current(context) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Contrast, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.profile_theme), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.profile_theme_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            AppTheme.entries.forEach { theme ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = theme == current,
                            onClick = { if (theme != current) AppTheme.set(context, theme) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = theme == current, onClick = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(stringResource(theme.label()), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

private fun AppTheme.label(): Int = when (this) {
    AppTheme.LIGHT -> R.string.theme_light
    AppTheme.DARK -> R.string.theme_dark
    AppTheme.SYSTEM -> R.string.theme_system
}
