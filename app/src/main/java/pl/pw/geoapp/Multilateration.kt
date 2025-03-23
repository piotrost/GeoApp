package pl.pw.geoapp

import pl.pw.geoapp.data.model.FinalBeacon
import kotlin.math.pow

object Multilateration {
    fun calculate(beacons: List<FinalBeacon>): Pair<Double, Double>? {
        if (beacons.size < 3) return null

        val A = mutableListOf<DoubleArray>()
        val B = mutableListOf<Double>()

        val x1 = beacons[0].x
        val y1 = beacons[0].y
        val d1 = beacons[0].distance

        for (i in 1 until beacons.size) {
            val x2 = beacons[i].x
            val y2 = beacons[i].y
            val d2 = beacons[i].distance

            A.add(doubleArrayOf(2 * (x2 - x1), 2 * (y2 - y1)))
            B.add(d1.pow(2) - d2.pow(2) - x1.pow(2) + x2.pow(2) - y1.pow(2) + y2.pow(2))
        }

        return solveLeastSquares(A.toTypedArray(), B.toDoubleArray())
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
}
