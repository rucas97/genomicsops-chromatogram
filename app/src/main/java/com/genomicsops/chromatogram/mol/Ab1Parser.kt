package com.genomicsops.chromatogram.mol

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)
            
            val magic = ByteArray(4)
            buffer.get(magic)
            if (String(magic) != "ABIF") return null
            
            buffer.getShort() // version
            buffer.getInt() // unused
            val dirOffset = buffer.getInt() 
            val entryCount = buffer.getInt() // FIX: Read count from header, not directory
            
            var bases = ""
            val traces = mutableMapOf<Int, IntArray>()

            // Read directory entries
            for (i in 0 until entryCount) {
                buffer.position(dirOffset + (i * 28)) // 28-byte entries
                
                val nameBytes = ByteArray(4)
                buffer.get(nameBytes)
                val name = String(nameBytes)
                val tagNumber = buffer.getInt()
                buffer.getShort() // element type
                buffer.getShort() // element size
                val numElements = buffer.getInt()
                val dataSize = buffer.getInt()
                val dataOffset = buffer.getInt()
                buffer.getInt() // data handle (unused)

                // Rule: if dataSize <= 4, data is in the offset field itself
                if (name == "PBAS" && tagNumber == 9) {
                    if (dataSize <= 4) {
                        val d = ByteBuffer.allocate(4).putInt(dataOffset).array()
                        bases = String(d).substring(0, numElements)
                    } else {
                        buffer.position(dataOffset)
                        val b = ByteArray(numElements)
                        buffer.get(b)
                        bases = String(b)
                    }
                } else if (name == "DATA" && tagNumber in 9..12) {
                    val traceArray = IntArray(numElements)
                    if (dataSize <= 4) {
                        traceArray[0] = dataOffset
                    } else {
                        buffer.position(dataOffset)
                        for (j in 0 until numElements) {
                            traceArray[j] = buffer.getShort().toInt() and 0xFFFF
                        }
                    }
                    traces[tagNumber] = traceArray
                }
            }
            
            return Chromatogram(
                bases = bases,
                traceA = traces[9] ?: IntArray(0),  // Often A
                traceC = traces[10] ?: IntArray(0), // Often C
                traceG = traces[11] ?: IntArray(0), // Often G
                traceT = traces[12] ?: IntArray(0)  // Often T
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
