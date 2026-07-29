package com.yishenghuang.skry.ui.vault

import android.graphics.Bitmap
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.yishenghuang.skry.R
import com.yishenghuang.skry.ui.components.MonochromeTag
import com.yishenghuang.skry.ui.components.SkryCard
import com.yishenghuang.skry.ui.components.SkryEmptyState
import com.yishenghuang.skry.ui.components.SkryScreenHeader
import com.yishenghuang.skry.ui.theme.AppDimensions
import com.yishenghuang.skry.ui.theme.SkryColors
import com.yishenghuang.skry.ui.theme.SkryTheme
import com.yishenghuang.skry.ui.theme.Typography

@Composable
fun VaultScreen(
    state: VaultUiState,
    onUnlockSuccess: () -> Unit,
    onUnlockFailed: (String) -> Unit = {},
    onLock: () -> Unit = {},
    onOpen: (String) -> Unit = {},
    onCloseDetail: () -> Unit = {},
    onDelete: () -> Unit = {},
    onClearMessage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    if (state.selectedId != null && state.unlocked) {
        VaultDetailPane(
            state = state,
            onBack = onCloseDetail,
            onDelete = onDelete,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimensions.spaceSm)
    ) {
        SkryScreenHeader(
            title = stringResource(R.string.vault_title),
            subtitle = if (state.unlocked) {
                stringResource(R.string.vault_subtitle_unlocked, state.count)
            } else {
                stringResource(R.string.vault_subtitle_locked)
            },
            trailing = if (state.unlocked) {
                {
                    TextButton(onClick = onLock) {
                        Icon(Icons.Outlined.Lock, contentDescription = null, tint = SkryColors.Accent)
                        Spacer(Modifier.width(AppDimensions.spaceXxxs))
                        Text(stringResource(R.string.vault_lock), color = SkryColors.Accent)
                    }
                }
            } else {
                null
            }
        )

        state.message?.let { msg ->
            SkryCard(
                minHeight = AppDimensions.spaceXxl,
                contentPadding = PaddingValues(AppDimensions.spaceSm),
                onClick = onClearMessage
            ) {
                Text(msg, style = Typography.bodyMedium, color = SkryColors.Primary)
            }
            Spacer(Modifier.height(AppDimensions.spaceSm))
        }

        if (!state.unlocked) {
            SkryEmptyState(
                title = if (state.count > 0) {
                    stringResource(R.string.vault_locked_title_n, state.count)
                } else {
                    stringResource(R.string.vault_locked_title)
                },
                subtitle = stringResource(R.string.vault_locked_subtitle),
                icon = Icons.Outlined.Lock,
                modifier = Modifier.padding(top = AppDimensions.spaceSm)
            )
            Spacer(Modifier.height(AppDimensions.spaceMd))
            Button(
                onClick = {
                    if (activity == null) {
                        onUnlockFailed(context.getString(R.string.vault_err_host))
                        return@Button
                    }
                    launchBiometric(
                        activity = activity,
                        onSuccess = onUnlockSuccess,
                        onError = onUnlockFailed
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.radiusButton),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SkryColors.Primary,
                    contentColor = SkryColors.OnBackground
                )
            ) {
                Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                Spacer(Modifier.width(AppDimensions.spaceXs))
                Text(stringResource(R.string.vault_unlock_biometrics))
            }
            Spacer(Modifier.height(AppDimensions.spaceSm))
            OutlinedButton(
                onClick = {
                    if (activity == null) {
                        onUnlockFailed(context.getString(R.string.vault_err_host))
                        return@OutlinedButton
                    }
                    launchBiometric(
                        activity = activity,
                        allowDeviceCredential = true,
                        onSuccess = onUnlockSuccess,
                        onError = onUnlockFailed
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.radiusButton),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SkryColors.Accent)
            ) {
                Icon(Icons.Outlined.LockOpen, contentDescription = null)
                Spacer(Modifier.width(AppDimensions.spaceXs))
                Text(stringResource(R.string.vault_unlock_device))
            }
        } else if (state.items.isEmpty()) {
            SkryEmptyState(
                title = stringResource(R.string.vault_empty_title),
                subtitle = stringResource(R.string.vault_empty_subtitle),
                icon = Icons.Outlined.LockOpen,
                modifier = Modifier.padding(top = AppDimensions.spaceSm)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = AppDimensions.spaceSm),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.spaceSm)
            ) {
                items(state.items, key = { it.id }) { item ->
                    SkryCard(
                        onClick = { onOpen(item.id) },
                        minHeight = AppDimensions.thumbSize,
                        contentPadding = PaddingValues(AppDimensions.spaceSm)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(AppDimensions.thumbSize)
                                    .clip(RoundedCornerShape(AppDimensions.radiusThumb))
                                    .background(SkryColors.SurfaceLifted),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = SkryColors.Primary
                                )
                            }
                            Spacer(Modifier.width(AppDimensions.spaceSm))
                            Column(modifier = Modifier.weight(1f)) {
                                MonochromeTag(text = item.title)
                                Spacer(Modifier.height(AppDimensions.spaceXs))
                                Text(item.subtitle, style = Typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultDetailPane(
    state: VaultUiState,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SkryColors.Background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimensions.spaceXs, vertical = AppDimensions.spaceXs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = SkryColors.OnBackground
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.vault_item_title), style = Typography.titleLarge)
                Text(
                    stringResource(R.string.vault_item_subtitle),
                    style = Typography.bodyMedium
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppDimensions.spaceSm)
                .padding(bottom = AppDimensions.spaceLg),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.spaceSm)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppDimensions.spaceXxl * 7)
                    .clip(RoundedCornerShape(AppDimensions.radiusCard))
                    .background(SkryColors.Surface),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.loadingPreview -> CircularProgressIndicator(color = SkryColors.Primary)
                    state.preview != null -> VaultBitmap(state.preview)
                    else -> Text(
                        stringResource(R.string.vault_preview_unavailable),
                        style = Typography.bodyMedium
                    )
                }
            }

            Text(
                stringResource(R.string.vault_detail_body),
                style = Typography.bodyMedium
            )

            Button(
                onClick = onDelete,
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AppDimensions.radiusButton),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SkryColors.Risk,
                    contentColor = SkryColors.OnBackground
                )
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                Spacer(Modifier.width(AppDimensions.spaceXs))
                Text(stringResource(R.string.vault_delete))
            }
        }
    }
}

@Composable
private fun VaultBitmap(bitmap: Bitmap) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = stringResource(R.string.vault_item_title),
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
    )
}

private fun launchBiometric(
    activity: FragmentActivity,
    allowDeviceCredential: Boolean = false,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val manager = BiometricManager.from(activity)
    val authenticators = if (allowDeviceCredential) {
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else {
        BiometricManager.Authenticators.BIOMETRIC_STRONG
    }
    when (manager.canAuthenticate(authenticators)) {
        BiometricManager.BIOMETRIC_SUCCESS -> Unit
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
            onError(activity.getString(R.string.vault_err_none_enrolled))
            return
        }
        else -> {
            if (!allowDeviceCredential) {
                onError(activity.getString(R.string.vault_err_unavailable))
                return
            }
        }
    }

    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    onError(errString.toString())
                }
            }
        }
    )
    val builder = BiometricPrompt.PromptInfo.Builder()
        .setTitle(activity.getString(R.string.vault_prompt_title))
        .setSubtitle(activity.getString(R.string.vault_prompt_subtitle))
        .setAllowedAuthenticators(authenticators)
    if (!allowDeviceCredential) {
        builder.setNegativeButtonText(activity.getString(R.string.action_cancel))
    }
    prompt.authenticate(builder.build())
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun VaultPreview() {
    SkryTheme {
        VaultScreen(
            state = VaultUiState(unlocked = false, count = 2),
            onUnlockSuccess = {}
        )
    }
}
