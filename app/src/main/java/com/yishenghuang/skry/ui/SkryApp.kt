package com.yishenghuang.skry.ui

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yishenghuang.skry.R
import com.yishenghuang.skry.ui.cleaner.CleanerDetailScreen
import com.yishenghuang.skry.ui.cleaner.CleanerScreen
import com.yishenghuang.skry.ui.cleaner.CleanerViewModel
import com.yishenghuang.skry.ui.dashboard.DashboardScreen
import com.yishenghuang.skry.ui.dashboard.DashboardViewModel
import com.yishenghuang.skry.ui.risk.RiskDetailScreen
import com.yishenghuang.skry.ui.risk.RiskExplorerScreen
import com.yishenghuang.skry.ui.risk.RiskViewModel
import com.yishenghuang.skry.ui.theme.SkryColors
import com.yishenghuang.skry.ui.vault.VaultScreen
import com.yishenghuang.skry.ui.vault.VaultViewModel
import com.yishenghuang.skry.util.MediaAccess

private enum class SkryDestination(
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Dashboard(R.string.nav_home, Icons.Filled.GridView, Icons.Outlined.GridView),
    Risk(R.string.nav_risk, Icons.Filled.Shield, Icons.Outlined.Shield),
    Clean(R.string.nav_clean, Icons.Filled.CleaningServices, Icons.Outlined.CleaningServices),
    Vault(R.string.nav_vault, Icons.Filled.Lock, Icons.Outlined.Lock)
}

@Composable
fun SkryApp() {
    var destination by rememberSaveable { mutableStateOf(SkryDestination.Dashboard) }
    var selectedRiskId by rememberSaveable { mutableStateOf<String?>(null) }

    val deleteViewModel: GalleryDeleteViewModel = viewModel()
    val deleteConfirmation by deleteViewModel.confirmation.collectAsStateWithLifecycle()
    val deleteBusy by deleteViewModel.busy.collectAsStateWithLifecycle()
    val deleteSender by deleteViewModel.sender.collectAsStateWithLifecycle()
    val deleteMessage by deleteViewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    val context = LocalContext.current
    val vaultSavedMessage = stringResource(R.string.vault_msg_saved)
    val vaultFailedMessage = stringResource(R.string.vault_msg_failed)
    val application = context.applicationContext as Application
    val dashboardViewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.factory(application)
    )
    val riskViewModel: RiskViewModel = viewModel(
        factory = RiskViewModel.factory(application)
    )
    val cleanerViewModel: CleanerViewModel = viewModel(
        factory = CleanerViewModel.factory(application)
    )
    val vaultViewModel: VaultViewModel = viewModel(
        factory = VaultViewModel.factory(application)
    )
    val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
    val riskState by riskViewModel.uiState.collectAsStateWithLifecycle()
    val cleanerState by cleanerViewModel.uiState.collectAsStateWithLifecycle()
    val cleanerDetail by cleanerViewModel.detailState.collectAsStateWithLifecycle()
    val vaultState by vaultViewModel.uiState.collectAsStateWithLifecycle()
    val vaultBusy by riskViewModel.vaultBusy.collectAsStateWithLifecycle()
    val vaultResult by riskViewModel.vaultResult.collectAsStateWithLifecycle()
    LaunchedEffect(vaultResult) {
        vaultResult?.let { result ->
            riskViewModel.clearVaultResult()
            if (result.success) {
                vaultViewModel.showMessage(result.message ?: vaultSavedMessage)
                selectedRiskId = null
                destination = SkryDestination.Vault
            } else {
                snackbar.showSnackbar(result.message ?: vaultFailedMessage)
            }
        }
    }
    val selectedRisk = riskState.items.firstOrNull { it.id == selectedRiskId }
    val showingCleanerDetail = destination == SkryDestination.Clean && cleanerDetail.item != null
    val showingVaultDetail =
        destination == SkryDestination.Vault && vaultState.selectedId != null && vaultState.unlocked

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        dashboardViewModel.onPermissionResult(MediaAccess.hasGalleryAccess(context))
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result -> deleteViewModel.onSystemResult(result.resultCode == Activity.RESULT_OK) }

    LaunchedEffect(deleteSender?.sequence) {
        deleteSender?.let { request ->
            try {
                deleteLauncher.launch(IntentSenderRequest.Builder(request.intentSender).build())
                deleteViewModel.senderLaunched()
            } catch (_: Exception) { deleteViewModel.launchFailed() }
        }
    }
    LaunchedEffect(deleteMessage) {
        deleteMessage?.let {
            snackbar.showSnackbar(it)
            deleteViewModel.clearMessage()
        }
    }
    if (deleteConfirmation.isNotEmpty()) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = deleteViewModel::cancelConfirmation,
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body, deleteConfirmation.size)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = deleteViewModel::confirm) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = deleteViewModel::cancelConfirmation) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        dashboardViewModel.onPermissionResult(MediaAccess.hasGalleryAccess(context))
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        vaultViewModel.lock()
    }

    LaunchedEffect(selectedRiskId, riskState.items) {
        if (selectedRiskId != null && riskState.items.none { it.id == selectedRiskId }) {
            selectedRiskId = null
        }
    }

    val showingRiskDetail = destination == SkryDestination.Risk && selectedRiskId != null
    val hideBottomBar = showingRiskDetail || showingCleanerDetail || showingVaultDetail

    if (showingRiskDetail) {
        BackHandler { selectedRiskId = null }
    }
    if (showingCleanerDetail) {
        BackHandler { cleanerViewModel.closeDetail() }
    }
    if (showingVaultDetail) {
        BackHandler { vaultViewModel.closeDetail() }
    }

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbar) },
        topBar = {
            if (deleteBusy) {
                androidx.compose.material3.TextButton(onClick = deleteViewModel::cancelPending) {
                    Text(stringResource(R.string.delete_stop_pending))
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
        containerColor = SkryColors.Background,
        bottomBar = {
            if (!hideBottomBar) {
                NavigationBar(
                    containerColor = SkryColors.Background,
                    contentColor = SkryColors.OnBackground
                ) {
                    SkryDestination.entries.forEach { item ->
                        val selected = destination == item
                        val label = stringResource(item.labelRes)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                destination = item
                                if (item != SkryDestination.Risk) selectedRiskId = null
                                if (item != SkryDestination.Clean) cleanerViewModel.closeDetail()
                                if (item != SkryDestination.Vault) vaultViewModel.closeDetail()
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = label
                                )
                            },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SkryColors.Primary,
                                selectedTextColor = SkryColors.Primary,
                                unselectedIconColor = SkryColors.Accent,
                                unselectedTextColor = SkryColors.Accent,
                                indicatorColor = SkryColors.Surface
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (destination) {
            SkryDestination.Dashboard -> DashboardScreen(
                state = dashboardState,
                onPauseScan = dashboardViewModel::pauseScan,
                onOpenSettings = {
                    context.startActivity(android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    ))
                },
                onPrivacyClick = { destination = SkryDestination.Risk },
                onDuplicatesClick = {
                    cleanerViewModel.selectSection(com.yishenghuang.skry.ui.cleaner.CleanerSection.Duplicates)
                    destination = SkryDestination.Clean
                },
                onBlurryClick = {
                    cleanerViewModel.selectSection(com.yishenghuang.skry.ui.cleaner.CleanerSection.Blurry)
                    destination = SkryDestination.Clean
                },
                onRequestPermission = {
                    permissionLauncher.launch(MediaAccess.requiredReadPermissions())
                },
                onScanNow = dashboardViewModel::scanGallery,
                modifier = contentModifier
            )
            SkryDestination.Risk -> {
                if (selectedRiskId != null) {
                    RiskDetailScreen(
                        item = selectedRisk,
                        onBack = { selectedRiskId = null },
                        onConfirmLeak = {
                            selectedRiskId?.let(riskViewModel::confirmLeak)
                            selectedRiskId = null
                        },
                        onDismiss = {
                            selectedRiskId?.let(riskViewModel::dismissFalsePositive)
                            selectedRiskId = null
                        },
                        onRestore = {
                            selectedRiskId?.let(riskViewModel::restoreReview)
                            selectedRiskId = null
                        },
                        vaultBusy = vaultBusy,
                        onMoveToVault = { selectedRiskId?.let(riskViewModel::moveToVault) },
                        modifier = contentModifier
                    )
                } else {
                    RiskExplorerScreen(
                        state = riskState,
                        onOpen = { selectedRiskId = it.id },
                        onToggle = riskViewModel::toggleSelection,
                        onSelectAll = riskViewModel::selectAllVisible,
                        onClearSelection = riskViewModel::clearSelection,
                        onFilterChange = riskViewModel::setFilter,
                        onCategoryChange = riskViewModel::setCategoryFilter,
                        onBatchConfirm = riskViewModel::batchConfirmSelected,
                        onBatchClear = riskViewModel::batchClearSelected,
                        onBatchRestore = riskViewModel::batchRestoreSelected,
                        modifier = contentModifier
                    )
                }
            }
            SkryDestination.Clean -> {
                if (showingCleanerDetail) {
                    CleanerDetailScreen(
                        state = cleanerDetail,
                        onBack = cleanerViewModel::closeDetail,
                        onOpenRelated = cleanerViewModel::openDetail,
                        modifier = contentModifier
                    )
                } else {
                    CleanerScreen(
                        state = cleanerState,
                        deleteBusy = deleteBusy,
                        onSectionSelected = cleanerViewModel::selectSection,
                        onOpen = { cleanerViewModel.openDetail(it.id) },
                        onToggle = cleanerViewModel::toggleSelection,
                        onSelectAll = cleanerViewModel::selectAllVisible,
                        onClearSelection = cleanerViewModel::clearSelection,
                        onDeleteSelected = {
                            val uris = cleanerViewModel.selectedUris()
                            if (uris.isEmpty()) return@CleanerScreen
                            deleteViewModel.request(uris)
                        },
                        modifier = contentModifier
                    )
                }
            }
            SkryDestination.Vault -> VaultScreen(
                state = vaultState,
                onUnlockSuccess = vaultViewModel::onUnlockSuccess,
                onUnlockFailed = vaultViewModel::showMessage,
                onLock = vaultViewModel::lock,
                onOpen = vaultViewModel::openItem,
                onCloseDetail = vaultViewModel::closeDetail,
                onDelete = vaultViewModel::deleteSelected,
                onDeleteOriginal = {
                    vaultState.items.firstOrNull { it.id == vaultState.selectedId }?.originalUri?.let {
                        deleteViewModel.request(listOf(Uri.parse(it)))
                    }
                },
                onClearMessage = vaultViewModel::clearMessage,
                modifier = contentModifier
            )
        }
    }
}
