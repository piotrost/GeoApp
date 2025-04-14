package pl.pw.geoapp

import pl.pw.geoapp.data.model.FinalBeacon
import kotlin.math.*

object PositioningAlgorithm {
    private const val distanceMultiplier: Double = 5.0

    fun multilateration(beacons: List<FinalBeacon>): Pair<Double, Double>? {
        if (beacons.size <= 1) return null
        else if (beacons.size <= 4) return weightedMean(beacons)

        val refLat = beacons[0].x
        val refLon = beacons[0].y

        val enuBeacons = beacons.map { beacon ->
            val enu = wgs84ToENU(refLat, refLon, beacon.x, beacon.y)
            Triple(enu.first, enu.second, beacon.distance * distanceMultiplier)
        }

        val A = mutableListOf<DoubleArray>()
        val B = mutableListOf<Double>()

        val (x1, y1, d1) = enuBeacons[0]

        for (i in 1 until enuBeacons.size) {
            val (x2, y2, d2) = enuBeacons[i]

            A.add(doubleArrayOf(2 * (x2 - x1), 2 * (y2 - y1)))
            B.add(d1.pow(2) - d2.pow(2) - x1.pow(2) + x2.pow(2) - y1.pow(2) + y2.pow(2))
        }

        val enuResult = solveLeastSquares(A.toTypedArray(), B.toDoubleArray()) ?: return null

        return enuToWGS84(refLat, refLon, enuResult)
    }

    fun weightedMean(beacons: List<FinalBeacon>): Pair<Double, Double>? {
        if (beacons.size < 2) return null

        var sumWeightsX = 0.0
        var weightedX = 0.0
        var sumWeightsY = 0.0
        var weightedY = 0.0

        for (beacon in beacons) {
            val weight = if (beacon.distance < 1e-10) 1e10 else 1.0 / beacon.distance

            sumWeightsX += weight
            weightedX += beacon.x * weight
            sumWeightsY += weight
            weightedY += beacon.y * weight
        }

        val resultX = weightedX / sumWeightsX
        val resultY = weightedY / sumWeightsY

        return Pair(resultX, resultY)
    }

    private fun solveLeastSquares(A: Array<DoubleArray>, B: DoubleArray): Pair<Double, Double>? {
        val ata = Array(2) { DoubleArray(2) }
        val atb = DoubleArray(2)

        for (i in A.indices) {
            ata[0][0] += A[i][0] * A[i][0]
            ata[0][1] += A[i][0] * A[i][1]
            ata[1][0] += A[i][1] * A[i][0]
            ata[1][1] += A[i][1] * A[i][1]
            atb[0] += A[i][0] * B[i]
            atb[1] += A[i][1] * B[i]
        }

        val det = ata[0][0] * ata[1][1] - ata[0][1] * ata[1][0]
        if (det == 0.0) return null

        return Pair(
            (atb[0] * ata[1][1] - atb[1] * ata[0][1]) / det,
            (atb[1] * ata[0][0] - atb[0] * ata[1][0]) / det
        )
    }

    private fun wgs84ToENU(refLat: Double, refLon: Double, lat: Double, lon: Double): Pair<Double, Double> {
        val earthRadius = 6378137.0

        val dLat = Math.toRadians(lat - refLat)
        val dLon = Math.toRadians(lon - refLon)

        val east = dLon * earthRadius * cos(refLat)
        val north = dLat * earthRadius

        return Pair(east, north)
    }

    private fun enuToWGS84(refLat: Double, refLon: Double, enu: Pair<Double, Double>): Pair<Double, Double> {
        val earthRadius = 6378137.0

        val dLat = enu.second / earthRadius
        val dLon = enu.first / (earthRadius * cos(Math.toRadians(refLat)))

        val lat = refLat + Math.toDegrees(dLat)
        val lon = refLon + Math.toDegrees(dLon)

        return Pair(lat, lon)
    }
}