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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var showSplash by rememberSaveable { mutableStateOf(true) }
    if (showSplash) SplashScreen { showSplash = false }
    else {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var chromatogram by remember { mutableStateOf<Chromatogram?>(null) }
        var fileName by rememberSaveable { mutableStateOf("No file loaded") }
        var isLoading by remember { mutableStateOf(false) }

        val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                isLoading = true
                scope.launch {
                    val parsed = withContext(Dispatchers.IO) {
                        try {
                            context.contentResolver.openInputStream(uri)?.use { Ab1Parser.parse(it) }
                        } catch (e: Exception) { null }
                    }
                    if (parsed == null) {
                        Toast.makeText(context, "Invalid .ab1 file.", Toast.LENGTH_LONG).show()
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
                val scrollState = rememberScrollState()
                val bases = chromatogram!!.bases
                val baseWidth = 12.dp // Width of 1 base letter on screen
                val totalWidth = (bases.length * baseWidth.value).dp

                Column(Modifier.fillMaxSize().horizontalScroll(scrollState)) {
                    
                    // 1. The Trace Canvas
                    Surface(
                        color = GenomicsSurface, 
                        shape = RoundedCornerShape(8.dp), 
                        modifier = Modifier.width(totalWidth).height(320.dp) // INCREASED HEIGHT
                    ) {
                        Canvas(
                            modifier = Modifier.fillMaxSize().pointerInput(bases, baseWidth) {
                                detectTapGestures { offset ->
                                    val pxPerBase = size.width.toFloat() / bases.length
                                    val index = (offset.x / pxPerBase).toInt().coerceIn(0, bases.length - 1)
                                    android.widget.Toast.makeText(
                                        context, 
                                        "Base: ${bases[index]}  |  Position: ${index + 1}", 
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        ) {
                            val maxVal = maxOf(
                                chromatogram!!.traceA.maxOrNull() ?: 1,
                                chromatogram!!.traceC.maxOrNull() ?: 1,
                                chromatogram!!.traceG.maxOrNull() ?: 1,
                                chromatogram!!.traceT.maxOrNull() ?: 1
                            ).toFloat()

                            fun drawTrace(trace: IntArray, color: Color) {
                                if (trace.isEmpty()) return
                                val path = Path()
                                // Map the trace points to the screen width
                                for (i in trace.indices) {
                                    val x = (i.toFloat() / trace.size) * size.width
                                    // ADDED VERTICAL PADDING so peaks don't touch top/bottom
                                    val y = size.height - ((trace[i] / maxVal) * (size.height * 0.8f)) - (size.height * 0.1f)
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                // ROUNDED CAPS AND JOINS FOR SMOOTHER CURVES
                                drawPath(
                                    path, 
                                    color = color, 
                                    style = Stroke(
                                        width = 3f, 
                                        cap = StrokeCap.Round, 
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                            
                            // Draw the 4 traces
                            drawTrace(chromatogram!!.traceA, Color(0xFF00FF00)) // A = Green
                            drawTrace(chromatogram!!.traceC, Color(0xFF0000FF)) // C = Blue
                            drawTrace(chromatogram!!.traceG, Color(0xFF808080)) // G = Black/Grey
                            drawTrace(chromatogram!!.traceT, Color(0xFFFF0000)) // T = Red
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    // 2. The Base Call Strip
                    Surface(
                        color = GenomicsSurface, 
                        shape = RoundedCornerShape(8.dp), 
                        modifier = Modifier.width(totalWidth).height(40.dp)
                    ) {
                        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                            bases.forEach { base ->
                                Text(
                                    text = base.toString(),
                                    color = when(base) {
                                        'A' -> Color(0xFF00FF00)
                                        'C' -> Color(0xFF0000FF)
                                        'G' -> Color(0xFF808080)
                                        'T' -> Color(0xFFFF0000)
                                        else -> Color.White
                                    },
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(baseWidth)
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    // 3. Text summary
                    Text("Total Bases: ${bases.length}", color = GenomicsMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                }
            }
        }
    }
}
