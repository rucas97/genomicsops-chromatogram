package com.genomicsops.chromatogram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.genomicsops.chromatogram.ui.*
import kotlin.math.sin
import kotlin.random.Random

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
        Column(Modifier.fillMaxSize().background(GenomicsDark).padding(16.dp)) {
            Text("Sanger Chromatogram", color = GenomicsCyan, fontSize = 24.sp)
            Spacer(Modifier.height(16.dp))
            
            Surface(color = GenomicsSurface, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(300.dp)) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val width = size.width
                    val height = size.height
                    
                    // Draw 4 colored traces (A=Green, C=Blue, G=Black, T=Red)
                    val colors = listOf(Color.Green, Color.Blue, Color.DarkGray, Color.Red)
                    
                    for (traceIndex in 0..3) {
                        val path = Path()
                        for (x in 0..width.toInt() step 2) {
                            // Generate a fake wave for each trace
                            val base = sin(x * 0.02f + traceIndex) * 30f
                            val noise = Random.nextFloat() * 10f
                            val peak = if (x % 50 == 0) 80f else 0f
                            
                            val y = (height / 2) + base + noise - peak
                            if (x == 0) path.moveTo(x.toFloat(), y) else path.lineTo(x.toFloat(), y)
                        }
                        drawPath(path, color = colors[traceIndex], style = Stroke(width = 2f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Note: Mock trace rendering. .ab1 binary parsing coming soon.", color = GenomicsMuted, fontSize = 12.sp)
        }
    }
}
