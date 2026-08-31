package com.footballai.prediction.engine

import com.footballai.prediction.model.*
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow

object DixonColesEngine {

    private const val MAX_SCORE = 8

    private fun factorial(n: Int): Double {
        var result = 1.0
        for (i in 2..n) result *= i
        return result
    }

    fun poissonProb(k: Int, lambda: Double): Double {
        if (lambda <= 0.0) return if (k == 0) 1.0 else 0.0
        return (lambda.pow(k) * exp(-lambda)) / factorial(k)
    }

    fun tau(x: Int, y: Int, lambda: Double, mu: Double, rho: Double): Double {
        return when {
            x == 0 && y == 0 -> 1.0 - (lambda * mu * rho)
            x == 0 && y == 1 -> 1.0 + (lambda * rho)
            x == 1 && y == 0 -> 1.0 + (mu * rho)
            x == 1 && y == 1 -> 1.0 - rho
            else -> 1.0
        }
    }

    fun computeMatchPrediction(
        home: TeamStats,
        away: TeamStats,
        leagueName: String,
        homeAdvantage: Double,
        rho: Double,
        generatedAt: String = "Live / Local Cache",
        dataSource: String = "Football-Data.org Cloud Pipeline"
    ): MatchPrediction {
        // Elo strength differential modifier (logistic sigmoid scaling)
        val eloDiff = (home.elo + 65.0) - away.elo
        val eloMultiplierHome = 1.0 / (1.0 + exp(-eloDiff / 400.0)) * 2.0
        val eloMultiplierAway = 2.0 - eloMultiplierHome

        // Recent rolling form adjustment factor
        val formFactorHome = 1.0 + ((home.formPointsLast5 - 7.5) * 0.015)
        val formFactorAway = 1.0 + ((away.formPointsLast5 - 7.5) * 0.015)

        // Rest days fatigue adjustment (slight penalty if <= 3 days rest)
        val restFactorHome = if (home.restDays <= 3) 0.95 else 1.0
        val restFactorAway = if (away.restDays <= 3) 0.95 else 1.0

        // Dynamic expected goals (lambda and mu)
        val lambda = max(
            home.attack * away.defense * homeAdvantage * (eloMultiplierHome * 0.5 + 0.5) * formFactorHome * restFactorHome,
            0.05
        )
        val mu = max(
            away.attack * home.defense * (eloMultiplierAway * 0.5 + 0.5) * formFactorAway * restFactorAway,
            0.05
        )

        val scoreMatrix = Array(MAX_SCORE + 1) { DoubleArray(MAX_SCORE + 1) }
        var totalProbabilityMass = 0.0

        for (x in 0..MAX_SCORE) {
            for (y in 0..MAX_SCORE) {
                val tauCorr = max(tau(x, y, lambda, mu, rho), 0.0001)
                val rawProb = tauCorr * poissonProb(x, lambda) * poissonProb(y, mu)
                scoreMatrix[x][y] = rawProb
                totalProbabilityMass += rawProb
            }
        }

        // Probability mass normalization
        for (x in 0..MAX_SCORE) {
            for (y in 0..MAX_SCORE) {
                scoreMatrix[x][y] /= totalProbabilityMass
            }
        }

        var pHomeWin = 0.0
        var pDraw = 0.0
        var pAwayWin = 0.0
        var pBttsYes = 0.0
        var pOver15 = 0.0
        var pOver25 = 0.0
        var pOver35 = 0.0
        var pHomeCleanSheet = 0.0
        var pAwayCleanSheet = 0.0

        val scoreList = mutableListOf<ScoreProbability>()

        for (x in 0..MAX_SCORE) {
            for (y in 0..MAX_SCORE) {
                val p = scoreMatrix[x][y]
                scoreList.add(ScoreProbability(x, y, p))

                when {
                    x > y -> pHomeWin += p
                    x == y -> pDraw += p
                    else -> pAwayWin += p
                }

                if (x > 0 && y > 0) pBttsYes += p
                if (y == 0) pHomeCleanSheet += p
                if (x == 0) pAwayCleanSheet += p

                val totalG = x + y
                if (totalG > 1.5) pOver15 += p
                if (totalG > 2.5) pOver25 += p
                if (totalG > 3.5) pOver35 += p
            }
        }

        val top5Scores = scoreList.sortedByDescending { it.probability }.take(5)

        val winProbSeparation = abs(pHomeWin - pAwayWin)
        val (confidenceLabel, confidenceGrade) = when {
            winProbSeparation >= 0.35 || abs(eloDiff) >= 170.0 -> "HIGH" to "A (Strong Signal)"
            winProbSeparation >= 0.16 || abs(eloDiff) >= 75.0 -> "MEDIUM" to "B (Moderate Edge)"
            else -> "LOW" to "C (High Variance)"
        }

        val factors = mutableListOf<String>()
        val netElo = (home.elo - away.elo).toInt()
        if (netElo > 60) {
            factors.add("Elo index advantage (+${netElo} pts) favors ${home.name}.")
        } else if (netElo < -60) {
            factors.add("Away team holds baseline Elo superiority (+${abs(netElo)} pts).")
        } else {
            factors.add("Balanced Elo ratings (within ${abs(netElo)} pts difference).")
        }

        if (home.formPointsLast5 >= 10) {
            factors.add("${home.name} in strong recent form (${home.formPointsLast5}/15 pts in last 5).")
        }
        if (away.formPointsLast5 >= 10) {
            factors.add("${away.name} in strong recent form (${away.formPointsLast5}/15 pts in last 5).")
        }

        if (lambda + mu >= 2.9) {
            factors.add("High scoring environment projected (${String.format("%.2f", lambda + mu)} model xG).")
        } else if (lambda + mu <= 2.25) {
            factors.add("Low scoring/defensive encounter favored (under 2.5 line).")
        }

        factors.add("Home advantage coefficient (+${((homeAdvantage - 1.0) * 100).toInt()}%) applied.")

        return MatchPrediction(
            homeTeam = home.name,
            awayTeam = away.name,
            leagueName = leagueName,
            homeWinProb = pHomeWin * 100.0,
            drawProb = pDraw * 100.0,
            awayWinProb = pAwayWin * 100.0,
            expHomeGoals = lambda,
            expAwayGoals = mu,
            expTotalGoals = lambda + mu,
            topScores = top5Scores,
            overUnder15 = OverUnder(1.5, pOver15 * 100.0, (1.0 - pOver15) * 100.0),
            overUnder25 = OverUnder(2.5, pOver25 * 100.0, (1.0 - pOver25) * 100.0),
            overUnder35 = OverUnder(3.5, pOver35 * 100.0, (1.0 - pOver35) * 100.0),
            bttsYesProb = pBttsYes * 100.0,
            bttsNoProb = (1.0 - pBttsYes) * 100.0,
            cleanSheets = CleanSheetProbabilities(
                homeCleanSheetProb = pHomeCleanSheet * 100.0,
                awayCleanSheetProb = pAwayCleanSheet * 100.0
            ),
            confidence = confidenceLabel,
            confidenceGrade = confidenceGrade,
            keyFactors = factors.take(4),
            dataFreshness = generatedAt,
            dataSource = dataSource
        )
    }
}

        var pHomeWin = 0.0
        var pDraw = 0.0
        var pAwayWin = 0.0
        var pBttsYes = 0.0
        var pOver15 = 0.0
        var pOver25 = 0.0
        var pOver35 = 0.0

        val scoreList = mutableListOf<ScoreProbability>()

        for (x in 0..MAX_SCORE) {
            for (y in 0..MAX_SCORE) {
                val p = scoreMatrix[x][y]
                scoreList.add(ScoreProbability(x, y, p))

                when {
                    x > y -> pHomeWin += p
                    x == y -> pDraw += p
                    else -> pAwayWin += p
                }

                if (x > 0 && y > 0) pBttsYes += p
                val totalG = x + y
                if (totalG > 1.5) pOver15 += p
                if (totalG > 2.5) pOver25 += p
                if (totalG > 3.5) pOver35 += p
            }
        }

        val top5Scores = scoreList.sortedByDescending { it.probability }.take(5)

        val eloDiscrepancy = (home.elo + 65.0) - away.elo
        val winProbSeparation = abs(pHomeWin - pAwayWin)
        val confidence = when {
            winProbSeparation >= 0.35 || abs(eloDiscrepancy) >= 170.0 -> "HIGH"
            winProbSeparation >= 0.16 || abs(eloDiscrepancy) >= 75.0 -> "MEDIUM"
            else -> "LOW"
        }

        val factors = mutableListOf<String>()
        val eloDiff = (home.elo - away.elo).toInt()
        if (eloDiff > 80) {
            factors.add("Clear Elo strength index advantage (+${eloDiff} pts) for ${home.name}.")
        } else if (eloDiff < -80) {
            factors.add("Away team holds superior baseline Elo rating (+${abs(eloDiff)} pts).")
        } else {
            factors.add("Evenly balanced baseline Elo strength ratings (difference < 80 pts).")
        }

        if (home.attack >= 1.35) {
            factors.add("${home.name} exhibits top-tier offensive output efficiency (${home.attack}x factor).")
        }
        if (away.defense >= 1.12) {
            factors.add("${away.name} concedes higher defensive conversion opportunities (${away.defense}x factor).")
        }
        if (lambda + mu >= 2.9) {
            factors.add("Elevated scoring environment projected (${String.format("%.2f", lambda + mu)} model xG).")
        } else if (lambda + mu <= 2.25) {
            factors.add("Tight defensive encounter favored under 2.5 goals line.")
        }
        factors.add("Home advantage coefficient (+${((homeAdvantage - 1.0) * 100).toInt()}%) factored into ${home.name}'s lambda.")

        return MatchPrediction(
            homeTeam = home.name,
            awayTeam = away.name,
            leagueName = leagueName,
            homeWinProb = pHomeWin * 100.0,
            drawProb = pDraw * 100.0,
            awayWinProb = pAwayWin * 100.0,
            expHomeGoals = lambda,
            expAwayGoals = mu,
            expTotalGoals = lambda + mu,
            topScores = top5Scores,
            overUnder15 = OverUnder(1.5, pOver15 * 100.0, (1.0 - pOver15) * 100.0),
            overUnder25 = OverUnder(2.5, pOver25 * 100.0, (1.0 - pOver25) * 100.0),
            overUnder35 = OverUnder(3.5, pOver35 * 100.0, (1.0 - pOver35) * 100.0),
            bttsYesProb = pBttsYes * 100.0,
            bttsNoProb = (1.0 - pBttsYes) * 100.0,
            confidence = confidence,
            keyFactors = factors.take(4)
        )
    }
}
