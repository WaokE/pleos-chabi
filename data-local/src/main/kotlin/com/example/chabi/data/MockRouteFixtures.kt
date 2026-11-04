package com.example.chabi.data

import com.example.chabi.domain.GeoPoint
import org.json.JSONObject
import java.io.InputStream

object MockRouteFixtures {
    fun read(input: InputStream): Map<String, List<GeoPoint>> {
        val features = JSONObject(input.bufferedReader().use { it.readText() }).getJSONArray("features")
        return (0 until features.length()).associate { index ->
            val feature = features.getJSONObject(index)
            val coordinates = feature.getJSONObject("geometry").getJSONArray("coordinates")
            feature.getJSONObject("properties").getString("tripId") to (0 until coordinates.length()).map { p ->
                val coordinate = coordinates.getJSONArray(p)
                GeoPoint(coordinate.getDouble(1), coordinate.getDouble(0))
            }
        }
    }
}
