package com.footballai.prediction

import com.footballai.prediction.engine.DixonColesEngine
import com.footballai.prediction.model.TeamStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DixonColesEngineTest {

    @Test
    fun testProbabilityNormalizationAndBounds() {
        val home = TeamStats(name = "Arsenal", elo = 1845.0, attack = 1.48, defense = 0.72)
        val away = TeamStats(name = "Aston Villa", elo = 1710.0, attack = 1.31, defense = 0.94)

        val prediction = DixonColesEngine.computeMatchPrediction(
            home = home,
            away = away,
            leagueName = "Premier League",
            homeAdvantage = 1.28,
            rho = -0.048
        )

        // 1. Total outcome probabilities sum to ~100%
        val totalProb = prediction.homeWinProb + prediction.drawProb + prediction.awayWinProb
        assertEquals(100.0, totalProb, 0.5)

        // 2. BTTS Yes + No sum to 100%
        assertEquals(100.0, prediction.bttsYesProb + prediction.bttsNoProb, 0.5)

        // 3. Expected goals sanity checks
        assertTrue("Home xG should be positive", prediction.expHomeGoals > 0.1)
        assertTrue("Away xG should be positive", prediction.expAwayGoals > 0.1)

        // 4. Over/Under probabilities sanity checks
        assertTrue(prediction.overUnder25.overProbability in 5.0..95.0)
        assertEquals(100.0, prediction.overUnder25.overProbability + prediction.overUnder25.underProbability, 0.5)
    }

    @Test
    fun testDixonColesTauLowScoringCorrection() {
        // Tau at 0-0 with negative rho should increase 0-0 probability
        val tau00 = DixonColesEngine.tau(0, 0, 1.5, 1.0, -0.048)
        assertTrue("Tau(0,0) should be > 1.0 when rho is negative", tau00 > 1.0)

        // Tau at 0-1 with negative rho should decrease probability
        val tau01 = DixonColesEngine.tau(0, 1, 1.5, 1.0, -0.048)
        assertTrue("Tau(0,1) should be < 1.0 when rho is negative", tau01 < 1.0)

        // Tau for higher scores should be 1.0 (uncorrected Poisson)
        val tau22 = DixonColesEngine.tau(2, 2, 1.5, 1.0, -0.048)
        assertEquals(1.0, tau22, 0.0001)
    }
}
