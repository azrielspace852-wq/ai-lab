package dev.axion.miniailab.model

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Reader/writer for Mini AI Lab .bin v1. Header is UTF-8 JSON; tensor payloads are little-endian Float32.
 * Keeps the web format's MALAB001 magic and tensor offsets (measured in float32 values).
 */
object ModelBin {
    private val magic = "MALAB001".toByteArray(Charsets.US_ASCII)
    data class Header(val json: JSONObject, val tensors: Map<String, FloatArray>)

    fun read(bytes: ByteArray): Header {
        require(bytes.size >= 12) { "File .bin terlalu pendek." }
        require(bytes.copyOfRange(0, 8).contentEquals(magic)) { "Header .bin tidak dikenali." }
        val headerSize = ByteBuffer.wrap(bytes, 8, 4).order(ByteOrder.LITTLE_ENDIAN).int
        require(headerSize in 2..(bytes.size - 12)) { "Header .bin rusak." }
        val json = JSONObject(bytes.copyOfRange(12, 12 + headerSize).toString(Charsets.UTF_8))
        require(json.optString("format") == "MiniAILabModel" && json.optInt("version") == 1) { "Versi/format model tidak didukung." }
        val tensorMeta = json.getJSONObject("tensors")
        val payload = bytes.copyOfRange(12 + headerSize, bytes.size)
        val result = linkedMapOf<String, FloatArray>()
        listOf("E", "W1", "b1", "W2", "b2").forEach { key ->
            val meta = tensorMeta.getJSONObject(key)
            val offset = meta.getInt("offset")
            val length = meta.getInt("length")
            require(offset >= 0 && length > 0 && offset.toLong() * 4 + length.toLong() * 4 <= payload.size) { "Tensor tidak valid/terpotong: $key" }
            val bb = ByteBuffer.wrap(payload, offset * 4, length * 4).order(ByteOrder.LITTLE_ENDIAN)
            val values = FloatArray(length) { bb.float }
            require(values.all { it.isFinite() }) { "Tensor berisi nilai non-finite: $key" }
            result[key] = values
        }
        return Header(json, result)
    }

    fun write(header: JSONObject, tensors: Map<String, FloatArray>): ByteArray {
        val meta = JSONObject()
        val payload = ByteArrayOutputStream()
        listOf("E", "W1", "b1", "W2", "b2").forEach { key ->
            val arr = requireNotNull(tensors[key]) { "Tensor hilang: $key" }
            val offset = payload.size() / 4
            val bb = ByteBuffer.allocate(arr.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            arr.forEach { bb.putFloat(it) }
            payload.write(bb.array())
            meta.put(key, JSONObject().put("offset", offset).put("length", arr.size))
        }
        header.put("tensors", meta)
        val head = header.toString().toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream()
        out.write(magic)
        out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(head.size).array())
        out.write(head); out.write(payload.toByteArray())
        return out.toByteArray()
    }
}
