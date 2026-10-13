package com.pickuphelper

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pickuphelper.service.FloatingWindowService
import com.pickuphelper.ui.screens.ParcelListScreen
import com.pickuphelper.ui.screens.SettingsScreen
import com.pickuphelper.ui.screens.StatsScreen
import com.pickuphelper.ui.theme.PickupHelperTheme
import com.pickuphelper.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = (application as PickupHelperApp).database

        // Start floating service if overlay permission already granted AND floating ball enabled
        if (Settings.canDrawOverlays(this) && com.pickuphelper.data.provider.ProviderConfig.getFloatingBallEnabled(this)) {
            FloatingWindowService.start(this)
        }

        setContent {
            PickupHelperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        database = database,
                        applicationContext = applicationContext
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check after returning from permission settings
        if (Settings.canDrawOverlays(this) && com.pickuphelper.data.provider.ProviderConfig.getFloatingBallEnabled(this)) {
            FloatingWindowService.start(this)
        }
    }
}

private enum class BottomNavItem(val label: String, val icon: ImageVector) {
    ParcelList("包裹列表", Icons.Default.Inventory),
    Stats("统计", Icons.Default.BarChart),
    Settings("设置", Icons.Default.Settings)
}

@Composable
private fun MainScreen(
    database: com.pickuphelper.data.local.AppDatabase,
    applicationContext: android.content.Context
) {
    val vm: MainViewModel = viewModel(
        factory = MainViewModel.Factory(database, applicationContext)
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavItem.entries.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> ParcelListScreen(vm = vm)
                1 -> StatsScreen(vm = vm)
                else -> SettingsScreen(vm = vm)
            }
        }
    }
}
