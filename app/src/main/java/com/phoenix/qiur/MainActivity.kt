package com.phoenix.qiur

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.phoenix.qiur.screen.AppNavigation
import com.phoenix.qiur.ui.theme.QiuRTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QiuRTheme {
                AppNavigation()
            }
        }
    }
}
