package com.footballai.prediction.model

data class TeamStats(
    val name: String,
    val elo: Double,
    val attack: Double,
    val defense: Double
)

data class LeagueData(
    val homeAdvantage: Double,
    val leagueAvgGoals: Double,
    val teams: Map<String, TeamStats>
)

data class ModelParameters(
    val version: String,
    val dixonColesRho: Double,
    val leagues: Map<String, LeagueData>
)

data class ScoreProbability(
    val homeGoals: Int,
    val awayGoals: Int,
    val probability: Double
)

data class OverUnder(
    val line: Double,
    val overProbability: Double,
    val underProbability: Double
)

data class MatchPrediction(
    val homeTeam: String,
    val awayTeam: String,
    val leagueName: String,
    val homeWinProb: Double,
    val drawProb: Double,
    val awayWinProb: Double,
    val expHomeGoals: Double,
    val expAwayGoals: Double,
    val expTotalGoals: Double,
    val topScores: List<ScoreProbability>,
    val overUnder15: OverUnder,
    val overUnder25: OverUnder,
    val overUnder35: OverUnder,
    val bttsYesProb: Double,
    val bttsNoProb: Double,
    val confidence: String,
    val keyFactors: List<String>
)
