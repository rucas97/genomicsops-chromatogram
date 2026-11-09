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
            
            // 1. Read Header (128 bytes)
            val magic = ByteArray(4)
            buffer.get(magic)
            if (String(magic) != "ABIF") return null
            
            buffer.getShort() // version
            buffer.getInt() // unused
            val dirOffset = buffer.getInt() // Directory offset
            buffer.getInt() // unused
            
            // 2. Read Directory
            buffer.position(dirOffset)
            val entryCount = buffer.getInt()
            
            var bases = ""
            var traceA = IntArray(0)
            var traceC = IntArray(0)
            var traceG = IntArray(0)
            var traceT = IntArray(0)

            val directory = mutableMapOf<String, Pair<Int, Int>>() 
            
            for (i in 0 until entryCount) {
                val nameBytes = ByteArray(4)
                buffer.get(nameBytes)
                val name = String(nameBytes)
                val tagNumber = buffer.getInt()
                buffer.getShort() // element type
                buffer.getShort() // element size
                val dataLen = buffer.getInt() // number of elements
                val dataSize = buffer.getInt() // total size in bytes
                val dataOffset = buffer.getInt()
                buffer.getInt() // FIX: DataHandle (4 bytes, always 0) -> Makes entry 28 bytes!
                
                directory["$name$tagNumber"] = Pair(dataSize, dataOffset)
            }
            
            // 3. Extract Base Calls (PBAS)
            directory["PBAS9"]?.let { (size, offset) ->
                buffer.position(offset)
                val b = ByteArray(size)
                buffer.get(b)
                bases = String(b)
            }
            
            // 4. Extract Traces (DATA)
            fun getTrace(key: String): IntArray {
                val (size, offset) = directory[key] ?: return IntArray(0)
                buffer.position(offset)
                val array = IntArray(size / 2) // Size is in bytes, each 'short' is 2 bytes
                for (i in array.indices) {
                    array[i] = buffer.getShort().toInt() and 0xFFFF
                }
                return array
            }
            
            traceA = getTrace("DATA9")
            traceC = getTrace("DATA10")
            traceG = getTrace("DATA11")
            traceT = getTrace("DATA12")
            
            return Chromatogram(bases, traceA, traceC, traceG, traceT)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
