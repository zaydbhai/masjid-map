package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppMainScreen
import com.example.ui.MasjidMapViewModel
import com.example.ui.theme.MasjidMapTheme
import com.example.widget.PrayerTimesWidget

class MainActivity : ComponentActivity() {

    private val pendingWidgetDest = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingWidgetDest.value = intent?.getStringExtra(PrayerTimesWidget.EXTRA_WIDGET_DEST)

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
                AppMainScreen(
                    masjidViewModel = masjidViewModel,
                    pendingWidgetDest = pendingWidgetDest.value,
                    onConsumeWidgetDest = { pendingWidgetDest.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingWidgetDest.value = intent.getStringExtra(PrayerTimesWidget.EXTRA_WIDGET_DEST)
    }
}
