package pl.pw.geoapp

import pl.pw.geoapp.data.model.FinalBeacon
import kotlin.math.*

object Multilateration {
    fun calculate(beacons: List<FinalBeacon>): Pair<Double, Double>? {
        if (beacons.size < 3) return null

        val refLat = beacons[0].x
        val refLon = beacons[0].y

        // Convert all beacon coordinates to ENU
        val enuBeacons = beacons.map { beacon ->
            val enu = wgs84ToENU(refLat, refLon, beacon.x, beacon.y)
            Triple(enu.first, enu.second, beacon.distance)
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

        // Convert the ENU result back to WGS84
        return enuToWGS84(refLat, refLon, enuResult)
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

    // WGS84 to ENU conversion
    private fun wgs84ToENU(refLat: Double, refLon: Double, lat: Double, lon: Double): Pair<Double, Double> {
        val earthRadius = 6378137.0

        val dLat = Math.toRadians(lat - refLat)
        val dLon = Math.toRadians(lon - refLon)

        val meanLat = Math.toRadians((lat + refLat) / 2.0)

        val east = dLon * earthRadius * cos(meanLat)
        val north = dLat * earthRadius

        return Pair(east, north)
    }

    // ENU to WGS84 conversion
    private fun enuToWGS84(refLat: Double, refLon: Double, enu: Pair<Double, Double>): Pair<Double, Double> {
        val earthRadius = 6378137.0

        val dLat = enu.second / earthRadius
        val dLon = enu.first / (earthRadius * cos(Math.toRadians(refLat)))

        val lat = refLat + Math.toDegrees(dLat)
        val lon = refLon + Math.toDegrees(dLon)

        return Pair(lat, lon)
    }
}
