package com.yishenghuang.skry.domain

import org.json.JSONArray
import org.json.JSONObject

object FindingsJson {
    fun encode(findings: List<Finding>): String {
        val array = JSONArray()
        findings.forEach { finding ->
            array.put(
                JSONObject().apply {
                    put("type", finding.type.name)
                    put("label", finding.label)
                    put("confidence", finding.confidence.toDouble())
                    put("snippet", finding.snippet)
                    put("boxLeft", finding.boxLeft?.toDouble())
                    put("boxTop", finding.boxTop?.toDouble())
                    put("boxRight", finding.boxRight?.toDouble())
                    put("boxBottom", finding.boxBottom?.toDouble())
                }
            )
        }
        return array.toString()
    }

    fun decode(raw: String): List<Finding> {
        if (raw.isBlank() || raw == "[]") return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val type = runCatching {
                        FindingType.valueOf(obj.getString("type"))
                    }.getOrNull() ?: continue
                    add(
                        Finding(
                            type = type,
                            label = obj.optString("label", type.name),
                            confidence = obj.optDouble("confidence", 0.5).toFloat(),
                            snippet = obj.optString("snippet").takeIf { it.isNotBlank() },
                            boxLeft = obj.nullableFloat("boxLeft"),
                            boxTop = obj.nullableFloat("boxTop"),
                            boxRight = obj.nullableFloat("boxRight"),
                            boxBottom = obj.nullableFloat("boxBottom")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun JSONObject.nullableFloat(key: String): Float? {
        if (!has(key) || isNull(key)) return null
        return optDouble(key).toFloat()
    }
}
