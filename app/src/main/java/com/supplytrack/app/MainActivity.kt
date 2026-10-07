package com.supplytrack.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.supplytrack.app.ui.navigation.SupplyTrackNavHost
import com.supplytrack.app.ui.theme.SupplyTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SupplyTrackTheme {
                SupplyTrackNavHost()
            }
        }
    }
}
