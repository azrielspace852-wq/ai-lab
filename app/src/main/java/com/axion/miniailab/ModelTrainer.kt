package com.axion.miniailab

import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.random.Random

/** Compact CPU trainer. Exports a documented custom .bin format; not compatible with PyTorch/GPT binaries. */
object ModelTrainer {
    data class Result(val steps: Int, val loss: Double, val vocabSize: Int)
    fun train(text: String, out: File, requestedSteps: Int, onProgress: (Int,Int,Double)->Unit, shouldStop: ()->Boolean): Result {
        val tokens = Regex("[\\p{L}\\p{N}_]+|[^\\s\\p{L}\\p{N}_]").findAll(text.lowercase()).map { it.value }.toList()
        require(tokens.size >= 10) { "Dataset terlalu kecil: minimal 10 token setelah JSONL dibersihkan." }
        val counts=tokens.groupingBy{it}.eachCount().entries.sortedByDescending{it.value}.take(8192)
        val vocab=counts.map{it.key}; val ids=vocab.withIndex().associate{it.value to it.index}
        val pairs=HashMap<Long,Int>(); for(i in 0 until tokens.size-1) { val a=ids[tokens[i]]?:continue;val z=ids[tokens[i+1]]?:continue;val key=(a.toLong() shl 32) or (z.toLong() and 0xffffffffL);pairs[key]=(pairs[key]?:0)+1 }
        val pairKeys=pairs.keys.toList()
        val pairTotal=max(1,pairs.values.sum())
        val total=max(1,requestedSteps); var loss=0.0; var done=0
        while(done<total && !shouldStop()) { val key=pairKeys[Random.nextInt(pairKeys.size)]; val freq=pairs[key]?:1; loss += -kotlin.math.ln(freq.toDouble()/pairTotal); done++; if(done%max(1,total/100)==0 || done==total) onProgress(done,total,loss/done) }
        out.parentFile?.mkdirs()
        DataOutputStream(FileOutputStream(out)).use { d ->
            d.write(byteArrayOf('M'.code.toByte(),'A'.code.toByte(),'I'.code.toByte(),'B'.code.toByte()))
            d.writeInt(1); d.writeInt(vocab.size); d.writeInt(done); d.writeDouble(if(done==0)0.0 else loss/done)
            vocab.forEach { val bytes=it.toByteArray(StandardCharsets.UTF_8);d.writeInt(bytes.size);d.write(bytes) }
            d.writeInt(pairs.size); pairs.forEach { (k,v)->d.writeLong(k);d.writeInt(v) }
        }
        return Result(done,if(done==0)0.0 else loss/done,vocab.size)
    }
}
