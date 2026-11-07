package com.genomicsops.chromatogram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
        var fileName by remember { mutableStateOf("Mock_Trace.ab1") }
        var seed by remember { mutableStateOf(0) } // Changes the wave shape

        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Loaded.ab1"
                seed = fileName.hashCode() // Generate a unique wave for each file
            }
        }
        Column(Modifier.fillMaxSize().background(GenomicsDark).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Chromatogram", color = GenomicsCyan, fontSize = 24.sp)
                IconButton(onClick = { filePicker.launch("*/*") }) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Load .ab1", tint = GenomicsTeal)
                }
            }
            Text(fileName, color = GenomicsMuted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
            
            Surface(color = GenomicsSurface, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(300.dp)) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val width = size.width
                    val height = size.height
                    val colors = listOf(Color.Green, Color.Blue, Color.DarkGray, Color.Red)
                    val random = Random(seed)
                    
                    for (traceIndex in 0..3) {
                        val path = Path()
                        for (x in 0..width.toInt() step 2) {
                            val base = sin(x * 0.02f + traceIndex) * 30f
                            val noise = random.nextFloat() * 10f
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
