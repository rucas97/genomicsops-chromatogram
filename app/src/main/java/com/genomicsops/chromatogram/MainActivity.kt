package com.genomicsops.chromatogram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genomicsops.chromatogram.ui.GenomicsTheme
import com.genomicsops.chromatogram.ui.SplashScreen
import com.genomicsops.chromatogram.ui.GenomicsCyan
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GenomicsTheme { AppRoot() } }
    }
}

@Composable
fun AppRoot() {
    var showSplash by remember { mutableStateOf(true) }
    if (showSplash) SplashScreen { showSplash = false }
    else {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Chromatogram Viewer", color = GenomicsCyan, fontSize = 24.sp)
            Spacer(Modifier.height(16.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                val path = Path()
                val width = size.width
                val height = size.height
                // Draw a fake DNA trace wave
                for (x in 0..width.toInt()) {
                    val y = (height / 2) + (sin(x * 0.05f) * 50f) + (sin(x * 0.1f) * 20f)
                    if (x == 0) path.moveTo(x.toFloat(), y) else path.lineTo(x.toFloat(), y)
                }
                drawPath(path, color = Color.Green, style = Stroke(width = 4f))
            }
        }
    }
}
