package com.baseline.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baseline.app.ui.BaselineTheme
import com.baseline.app.ui.Palette

/**
 * Required by Health Connect: linked from the permissions screen so the user can
 * read what the app does with their data before granting access.
 */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BaselineTheme {
                Box(
                    Modifier.fillMaxSize().background(Palette.Bg),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column(
                        Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp)
                    ) {
                        Text(
                            "Why Baseline asks for this",
                            color = Palette.Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(16.dp))
                        Body(
                            "Baseline reads two things from Health Connect: your sleep sessions and " +
                                "your exercise sessions."
                        )
                        Body(
                            "It uses them to pre-fill the sleep and training fields when you log a day. " +
                                "Nothing is saved until you press Save, and you can edit or ignore every " +
                                "value it suggests."
                        )
                        Body(
                            "Baseline asks for read access only. It never writes to Health Connect, never " +
                                "reads your data in the background, and never sends it anywhere. The app has " +
                                "no accounts and no analytics."
                        )
                        Body(
                            "Sleep quality is not taken from the watch. Health Connect does not carry " +
                                "Garmin's sleep score, so quality is estimated from sleep efficiency " +
                                "(time asleep versus time in bed), and it is labelled that way."
                        )
                        Body(
                            "You can revoke access at any time in Android Settings → Health Connect."
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Baseline is a wellness tracker. It does not diagnose or treat anything.",
                            color = Palette.TextDim, fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Body(text: String) {
        Text(
            text,
            color = Palette.TextDim,
            fontSize = 13.5.sp,
            lineHeight = 21.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}
