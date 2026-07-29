package com.yishenghuang.skry

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.yishenghuang.skry.ui.SkryApp
import com.yishenghuang.skry.ui.splash.SkrySplashGate
import com.yishenghuang.skry.ui.theme.SkryTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SkryTheme {
                SkrySplashGate {
                    SkryApp()
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun AppPreview() {
    SkryTheme {
        SkryApp()
    }
}
