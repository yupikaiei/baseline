package com.baseline.app.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Device-credential lock. Deliberately uses KeyguardManager rather than a
 * biometric library: no extra dependency, and it falls back to the PIN/pattern
 * the phone already trusts.
 */
@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) onUnlocked()
    }

    fun requestUnlock() {
        val intent: Intent? = keyguard.createConfirmDeviceCredentialIntent(
            "Unlock Baseline",
            "Your health data is behind your device lock"
        )
        if (intent != null) {
            launcher.launch(intent)
        } else {
            // No device credential configured: never trap the user behind a lock
            // they cannot satisfy.
            onUnlocked()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Palette.Bg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                Modifier
                    .size(54.dp)
                    .background(Palette.SurfaceAlt, RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "BASELINE",
                color = Palette.Text, fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Locked. Your logs stay on this device.",
                color = Palette.TextDim, fontSize = 12.5.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = { requestUnlock() },
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Palette.Accent, contentColor = Color(0xFF06231A)
                )
            ) {
                Text("Unlock", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
