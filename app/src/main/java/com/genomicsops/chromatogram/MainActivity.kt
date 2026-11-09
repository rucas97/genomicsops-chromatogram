package com.genomicsops.chromatogram

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.genomicsops.chromatogram.mol.Ab1Parser
import com.genomicsops.chromatogram.mol.Chromatogram
import com.genomicsops.chromatogram.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var chromatogram by remember { mutableStateOf<Chromatogram?>(null) }
        var fileName by remember { mutableStateOf("No file loaded") }
        var isLoading by remember { mutableStateOf(false) }

        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                isLoading = true
                scope.launch {
                    val parsed = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { Ab1Parser.parse(it) }
                        } catch (e: Exception) { null }
                    }
                    if (parsed == null) {
                        Toast.makeText(context, "Invalid .ab1 file. Ensure it is a true ABIF binary file.", Toast.LENGTH_LONG).show()
                    } else {
                        chromatogram = parsed
                        fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Loaded .ab1"
                    }
                    isLoading = false
                }
            }
        }

        Column(Modifier.fillMaxSize().background(GenomicsDark).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Chromatogram", color = GenomicsCyan, fontSize = 24.sp)
                IconButton(onClick = { filePicker.launch("*/*") }, enabled = !isLoading) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Load .ab1", tint = GenomicsTeal)
                }
            }
            Text(fileName, color = GenomicsMuted, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
            
            if (isLoading) {
                CircularProgressIndicator(color = GenomicsCyan, modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
            } else if (chromatogram == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tap the folder icon to load a real .ab1 file", color = GenomicsMuted)
                }
            } else {
                Surface(color = GenomicsSurface, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().height(350.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        val width = size.width
                        val height = size.height
                        val cg = chromatogram!!
                        
                        fun drawTrace(trace: IntArray, color: Color) {
                            if (trace.isEmpty()) return
                            val path = Path()
                            val maxVal = trace.maxOrNull()?.toFloat() ?: 1f
                            for (i in trace.indices) {
                                val x = (i.toFloat() / trace.size) * width
                                val y = height - ((trace[i] / maxVal) * height * 0.9f) - (height * 0.05f)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(path, color = color, style = Stroke(width = 3f))
                        }
                        
                        drawTrace(cg.traceA, Color.Green)
                        drawTrace(cg.traceC, Color.Blue)
                        drawTrace(cg.traceG, Color.DarkGray)
                        drawTrace(cg.traceT, Color.Red)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Base Calls:", color = GenomicsMuted, fontSize = 12.sp)
                Text(chromatogram!!.bases.take(150), color = GenomicsCyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }
}
