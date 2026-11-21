package com.example.mobilelab01

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.mobilelab01.navigation.TaskManagerApp
import com.example.mobilelab01.ui.theme.MobileLab01Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobileLab01Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TaskManagerApp()
                }
            }
        }
    }
}
