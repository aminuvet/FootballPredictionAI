package com.footballai.prediction.model

data class TeamStats(
    val id: Int = 0,
    val name: String,
    val shortName: String = "",
    val elo: Double,
    val attack: Double,
    val defense: Double,
    val formPointsLast5: Int = 0,
    val formGoalsScoredLast5: Int = 0,
    val formGoalsConcededLast5: Int = 0,
    val restDays: Int = 7
)

data class LeagueData(
    val id: Int = 0,
    val code: String = "",
    val homeAdvantage: Double,
    val leagueAvgGoals: Double,
    val teams: Map<String, TeamStats>
)

data class Fixture(
    val id: Int,
    val leagueName: String,
    val leagueCode: String,
    val homeTeam: String,
    val awayTeam: String,
    val utcDate: String,
    val status: String,
    val currentHomeScore: Int? = null,
    val currentAwayScore: Int? = null,
    val minute: Int? = null
)

data class ModelParameters(
    val version: String,
    val generatedAt: String,
    val dataSource: String,
    val dixonColesRho: Double,
    val leagues: Map<String, LeagueData>,
    val upcomingFixtures: List<Fixture> = emptyList()
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

data class CleanSheetProbabilities(
    val homeCleanSheetProb: Double,
    val awayCleanSheetProb: Double
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
    val cleanSheets: CleanSheetProbabilities,
    val confidence: String,
    val confidenceGrade: String,
    val keyFactors: List<String>,
    val dataFreshness: String,
    val dataSource: String
)
