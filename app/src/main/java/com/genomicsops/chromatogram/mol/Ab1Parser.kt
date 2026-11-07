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
            buffer.getInt() // unused
            
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
                val tagType = buffer.getShort()
                buffer.getShort() // element length
                val dataLen = buffer.getInt()
                val dataOffset = buffer.getInt()
                buffer.getInt() // FIX: Skip the 4-byte DataHandle to align the pointer!
                
                directory["$name$tagNumber"] = Pair(dataLen, dataOffset)
            }
            
            directory["PBAS9"]?.let { (len, offset) ->
                buffer.position(offset)
                val b = ByteArray(len)
                buffer.get(b)
                bases = String(b)
            }
            
            fun getTrace(key: String): IntArray {
                val (len, offset) = directory[key] ?: return IntArray(0)
                buffer.position(offset)
                val array = IntArray(len)
                for (i in 0 until len) {
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
