package com.baseline.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baseline.app.data.EvidenceItem
import com.baseline.app.data.Metric
import com.baseline.app.data.evidenceLibrary
import com.baseline.app.logic.Format
import com.baseline.app.logic.Insights
import com.baseline.app.ui.BaselineTab
import com.baseline.app.ui.BaselineTheme
import com.baseline.app.ui.CheckinScreen
import com.baseline.app.ui.CoachScreen
import com.baseline.app.ui.EvidenceDetailSheet
import com.baseline.app.ui.EvidenceScreen
import com.baseline.app.ui.ExperimentDetailScreen
import com.baseline.app.ui.ExperimentsScreen
import com.baseline.app.ui.Export
import com.baseline.app.ui.LockScreen
import com.baseline.app.ui.NewExperimentScreen
import com.baseline.app.ui.OnboardingScreen
import com.baseline.app.ui.Palette
import com.baseline.app.ui.ProgramsScreen
import com.baseline.app.ui.SettingsScreen
import com.baseline.app.ui.TodayScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Own edge-to-edge explicitly. Android 15+ forces it for targetSdk 35+,
        // and leaving it to the platform put the custom header beneath the status
        // bar on devices like the Fairphone 5. Handling insets here makes the
        // result identical across devices and versions.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContent {
            BaselineTheme {
                val vm: BaselineViewModel = viewModel()
                val state by vm.state.collectAsState()
                AppRoot(vm, state)
            }
        }
    }
}

private sealed interface Screen {
    data object Tabs : Screen
    data object Checkin : Screen
    data object NewExperiment : Screen
    data object Coach : Screen
    data object Settings : Screen
    data object EditProfile : Screen
    data class ExperimentDetail(val id: String) : Screen
    data class Evidence(val item: EvidenceItem) : Screen
}

@Composable
private fun AppRoot(vm: BaselineViewModel, state: UiState) {
    var tab by remember { mutableStateOf(0) }
    var screen by remember { mutableStateOf<Screen>(Screen.Tabs) }
    var showFocusPicker by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var unlocked by remember { mutableStateOf(false) }
    var onboardingDismissed by remember { mutableStateOf(false) }
    val context = LocalContext.current

    fun exportCsv() = vm.allCheckins { checkins ->
        Export.share(context, "baseline-${Format.today()}.csv", Export.csv(checkins), "text/csv")
    }

    fun exportJson() = vm.allCheckins { checkins ->
        vm.allExperiments { experiments ->
            Export.share(
                context,
                "baseline-${Format.today()}.json",
                Export.json(checkins, experiments),
                "application/json"
            )
        }
    }

    fun exportSummary() = vm.allCheckins { checkins ->
        vm.allExperiments { experiments ->
            Export.share(
                context,
                "baseline-summary-${Format.today()}.txt",
                Export.summary(checkins, experiments, state.profile),
                "text/plain"
            )
        }
    }

    if (state.lockEnabled && !unlocked) {
        LockScreen(onUnlocked = { unlocked = true })
        return
    }

    // First run: personalisation is offered once, never forced.
    if (state.needsOnboarding && !onboardingDismissed && !state.loading) {
        OnboardingScreen(
            onDone = { profile ->
                vm.saveProfile(profile)
                onboardingDismissed = true
            },
            onSkip = { onboardingDismissed = true }
        )
        return
    }

    BackHandler(enabled = screen != Screen.Tabs) { screen = Screen.Tabs }

    when (val current = screen) {
        Screen.Checkin -> FullScreen { CheckinScreen(
            existing = state.todayCheckin,
            date = state.today,
            onSave = { checkin ->
                vm.saveCheckin(checkin)
                screen = Screen.Tabs
            },
            onCancel = { screen = Screen.Tabs }
        ) }

        Screen.NewExperiment -> FullScreen { NewExperimentScreen(
            onStart = { experiment ->
                vm.startExperiment(experiment)
                screen = Screen.Tabs
                tab = 2
            },
            onCancel = { screen = Screen.Tabs }
        ) }

        Screen.Coach -> FullScreen { CoachScreen(
            state = state,
            vm = vm,
            onOpenEvidence = { id ->
                evidenceLibrary.firstOrNull { it.id == id }?.let { screen = Screen.Evidence(it) }
            },
            onOpenSettings = { screen = Screen.Settings }
        ) }

        Screen.Settings -> FullScreen { SettingsScreen(
            state = state,
            vm = vm,
            onClose = { screen = Screen.Tabs },
            onOpenCoach = { screen = Screen.Coach },
            onExportCsv = { exportCsv() },
            onExportJson = { exportJson() },
            onExportSummary = { exportSummary() },
            onEditProfile = { screen = Screen.EditProfile }
        ) }

        Screen.EditProfile -> FullScreen { OnboardingScreen(
            initial = state.profile,
            onDone = { profile ->
                vm.saveProfile(profile)
                screen = Screen.Tabs
            },
            onSkip = { screen = Screen.Tabs }
        ) }

        is Screen.ExperimentDetail -> FullScreen { ExperimentDetailScreen(
            state = state,
            experimentId = current.id,
            onStop = { vm.stopExperiment(it) },
            onBack = { screen = Screen.Tabs }
        ) }

        is Screen.Evidence -> FullScreen { EvidenceDetailSheet(current.item) { screen = Screen.Tabs } }

        Screen.Tabs -> Scaffold(
            containerColor = Palette.Bg,
            topBar = {
                Row(
                    modifier = Modifier
                        // A plain Row does not consume insets the way TopAppBar
                        // does — this is exactly why the header sat under the clock.
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(22.dp).border(2.dp, Palette.Accent, RoundedCornerShape(6.dp))
                    )
                    Text(
                        "BASELINE", color = Palette.Text, fontSize = 16.sp, letterSpacing = 1.6.sp,
                        modifier = Modifier.padding(start = 9.dp)
                    )
                    Box(Modifier.weight(1f))
                    IconButton(onClick = { screen = Screen.Coach }) {
                        Icon(
                            Icons.Filled.AutoAwesome, contentDescription = "Coach",
                            tint = if (state.coachConfigured) Palette.Accent else Palette.TextDim
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreHoriz, contentDescription = "Menu", tint = Palette.TextDim)
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = Palette.SurfaceAlt
                        ) {
                            DropdownMenuItem(
                                text = { Text("Settings", color = Palette.Text) },
                                onClick = { menuOpen = false; screen = Screen.Settings }
                            )
                            DropdownMenuItem(
                                text = { Text("Focus metric", color = Palette.Text) },
                                onClick = { menuOpen = false; showFocusPicker = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Wellness summary", color = Palette.Text) },
                                onClick = { menuOpen = false; exportSummary() }
                            )
                            DropdownMenuItem(
                                text = { Text("Export CSV", color = Palette.Text) },
                                onClick = { menuOpen = false; exportCsv() }
                            )
                            DropdownMenuItem(
                                text = { Text("Export JSON (backup)", color = Palette.Text) },
                                onClick = { menuOpen = false; exportJson() }
                            )
                        }
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Palette.Surface,
                    contentColor = Palette.Text,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    NavItem("Today", Icons.Filled.RadioButtonChecked, Icons.Filled.RadioButtonUnchecked, tab == 0) { tab = 0 }
                    NavItem("Baseline", Icons.Filled.ShowChart, Icons.Filled.ShowChart, tab == 1) { tab = 1 }
                    NavItem("Trials", Icons.Filled.Science, Icons.Filled.Science, tab == 2) { tab = 2 }
                    NavItem("Programs", Icons.Filled.CheckBox, Icons.Filled.CheckBoxOutlineBlank, tab == 3) { tab = 3 }
                    NavItem("Evidence", Icons.Filled.MenuBook, Icons.Filled.MenuBook, tab == 4) { tab = 4 }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
                    0 -> TodayScreen(
                        state = state,
                        onLog = { screen = Screen.Checkin },
                        onOpenEvidence = { id ->
                            evidenceLibrary.firstOrNull { it.id == id }?.let { screen = Screen.Evidence(it) }
                        }
                    )
                    1 -> BaselineTab(state)
                    2 -> ExperimentsScreen(
                        state = state,
                        onNew = { screen = Screen.NewExperiment },
                        onOpen = { id -> screen = Screen.ExperimentDetail(id) }
                    )
                    3 -> ProgramsScreen(
                        state = state,
                        onStart = { vm.startProgram(it) },
                        onMarkDay = { day, done -> vm.markProgramDay(day, done) },
                        onLeave = { vm.leaveProgram() }
                    )
                    else -> EvidenceScreen { item -> screen = Screen.Evidence(item) }
                }
            }
        }
    }

    if (showFocusPicker) {
        AlertDialog(
            onDismissRequest = { showFocusPicker = false },
            containerColor = Palette.SurfaceAlt,
            title = { Text("Focus metric", color = Palette.Text) },
            text = {
                Column {
                    Text(
                        "The one number on the home screen. Pick the pillar you are working on.",
                        color = Palette.TextDim, fontSize = 12.sp, lineHeight = 18.sp
                    )
                    Column(Modifier.padding(top = 12.dp)) {
                        Insights.PILLARS.forEach { m: Metric ->
                            Row(
                                modifier = Modifier
                                    .width(240.dp)
                                    .clickable { vm.setFocus(m); showFocusPicker = false }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(m.label, color = Palette.Text, fontSize = 14.sp)
                                Box(Modifier.weight(1f))
                                if (m == state.focus) Text("✓", color = Palette.Accent, fontSize = 14.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFocusPicker = false }) {
                    Text("Done", color = Palette.Accent)
                }
            }
        )
    }
}

/**
 * Full-screen routes draw their own background edge-to-edge, so each one needs the
 * system insets applied. Wrapping them centrally keeps this to a single definition
 * instead of drifting per screen.
 */
@Composable
private fun FullScreen(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) { content() }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavItem(
    label: String,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                modifier = Modifier.size(22.dp)
            )
        },
        label = { Text(label, fontSize = 9.5.sp) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Palette.Accent,
            selectedTextColor = Palette.Accent,
            unselectedIconColor = Palette.TextDim,
            unselectedTextColor = Palette.TextDim,
            indicatorColor = Palette.AccentDim.copy(alpha = 0.5f)
        )
    )
}
