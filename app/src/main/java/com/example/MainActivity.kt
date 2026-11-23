package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppMainScreen
import com.example.ui.MasjidMapViewModel
import com.example.ui.theme.MasjidMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val masjidViewModel: MasjidMapViewModel = viewModel()
            val state by masjidViewModel.uiState.collectAsStateWithLifecycle()

            MasjidMapTheme(
                themeMode = state.themeMode,
                arabicFont = state.arabicFont,
                arabicFontSizeSp = state.arabicFontSizeSp,
                englishFont = state.englishFont,
                englishFontSizeSp = state.englishFontSizeSp
            ) {
                AppMainScreen(masjidViewModel = masjidViewModel)
            }
        }
    }
}
