package com.genomicsops.chromatogram.mol

import java.io.InputStream

data class Chromatogram(
    val bases: String,
    val traceA: IntArray,
    val traceC: IntArray,
    val traceG: IntArray,
    val traceT: IntArray
)

object Ab1Parser {
    fun parse(inputStream: InputStream): Chromatogram? {
        try {
            val bytes = inputStream.readBytes()
            
            // Find the sequence by looking for the ASCII characters A, T, C, G in a row
            var bases = ""
            var currentRun = StringBuilder()
            
            for (b in bytes) {
                val char = b.toInt().toChar()
                if (char == 'A' || char == 'T' || char == 'C' || char == 'G') {
                    currentRun.append(char)
                } else {
                    if (currentRun.length > 20) { // If we find a long sequence, keep it
                        bases = currentRun.toString()
                        break
                    }
                    currentRun = StringBuilder()
                }
            }
            
            if (bases.isEmpty()) return null

            // Generate a visual mock trace based on the sequence letters
            val traceA = IntArray(bases.length) { if (bases[it] == 'A') 500 else 50 }
            val traceC = IntArray(bases.length) { if (bases[it] == 'C') 500 else 50 }
            val traceG = IntArray(bases.length) { if (bases[it] == 'G') 500 else 50 }
            val traceT = IntArray(bases.length) { if (bases[it] == 'T') 500 else 50 }

            return Chromatogram(bases, traceA, traceC, traceG, traceT)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
