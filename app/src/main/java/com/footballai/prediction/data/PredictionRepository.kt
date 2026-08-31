package com.footballai.prediction.data

import android.content.Context
import com.footballai.prediction.engine.DixonColesEngine
import com.footballai.prediction.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class PredictionRepository(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val cacheFileName = "remote_model_parameters.json"
    private var cachedParameters: ModelParameters? = null

    private val remoteDataUrl =
        "https://raw.githubusercontent.com/aminuvet/FootballPredictionAI/main/pipeline/data/model_parameters.json"

    suspend fun syncRemoteParameters(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(remoteDataUrl).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string() ?: return@withContext false
                val parsed = parseJsonString(jsonString)
                if (parsed != null && parsed.leagues.isNotEmpty()) {
                    val cacheFile = File(context.filesDir, cacheFileName)
                    cacheFile.writeText(jsonString)
                    cachedParameters = parsed
                    return@withContext true
                }
            }
        } catch (_: Exception) {
        }
        return@withContext false
    }

    private fun loadParameters(): ModelParameters {
        cachedParameters?.let { return it }

        val cacheFile = File(context.filesDir, cacheFileName)
        if (cacheFile.exists()) {
            try {
                val cachedJson = cacheFile.readText()
                val parsed = parseJsonString(cachedJson)
                if (parsed != null && parsed.leagues.isNotEmpty()) {
                    cachedParameters = parsed
                    return parsed
                }
            } catch (_: Exception) {
                cacheFile.delete()
            }
        }

        try {
            val inputStream = context.assets.open("model_parameters.json")
            val assetJson = InputStreamReader(inputStream).readText()
            val parsed = parseJsonString(assetJson)
            if (parsed != null && parsed.leagues.isNotEmpty()) {
                cachedParameters = parsed
                return parsed
            }
        } catch (_: Exception) {
        }

        val fallback = createFallbackParameters()
        cachedParameters = fallback
        return fallback
    }

    private fun parseJsonString(jsonString: String): ModelParameters? {
        return try {
            val jsonObject = gson.fromJson(jsonString, JsonObject::class.java) ?: return null
            val version = jsonObject.get("version")?.asString ?: "2.0.0"
            val generatedAt = jsonObject.get("generatedAt")?.asString ?: "Local Baseline"
            val dataSource = jsonObject.get("dataSource")?.asString ?: "Built-in Engine"
            val rho = jsonObject.get("dixon_coles_rho")?.asDouble ?: -0.048
            val leaguesJson = jsonObject.getAsJsonObject("leagues") ?: return null

            val leaguesMap = mutableMapOf<String, LeagueData>()

            for (leagueKey in leaguesJson.keySet()) {
                val lObj = leaguesJson.getAsJsonObject(leagueKey) ?: continue
                val leagueId = lObj.get("id")?.asInt ?: 0
                val leagueCode = lObj.get("code")?.asString ?: ""
                val homeAdv = lObj.get("home_advantage")?.asDouble ?: 1.25
                val avgGoals = lObj.get("league_avg_goals")?.asDouble ?: 2.70
                val teamsObj = lObj.getAsJsonObject("teams") ?: continue

                val teamsMap = mutableMapOf<String, TeamStats>()
                for (teamKey in teamsObj.keySet()) {
                    val tObj = teamsObj.getAsJsonObject(teamKey) ?: continue
                    teamsMap[teamKey] = TeamStats(
                        id = tObj.get("id")?.asInt ?: 0,
                        name = teamKey,
                        shortName = tObj.get("shortName")?.asString ?: teamKey,
                        elo = tObj.get("elo")?.asDouble ?: 1500.0,
                        attack = tObj.get("attack")?.asDouble ?: 1.0,
                        defense = tObj.get("defense")?.asDouble ?: 1.0,
                        formPointsLast5 = tObj.get("formPointsLast5")?.asInt ?: 7,
                        formGoalsScoredLast5 = tObj.get("formGoalsScoredLast5")?.asInt ?: 6,
                        formGoalsConcededLast5 = tObj.get("formGoalsConcededLast5")?.asInt ?: 6,
                        restDays = tObj.get("restDays")?.asInt ?: 7
                    )
                }
                leaguesMap[leagueKey] = LeagueData(leagueId, leagueCode, homeAdv, avgGoals, teamsMap)
            }

            val fixturesList = mutableListOf<Fixture>()
            if (jsonObject.has("upcomingFixtures")) {
                val fixturesArray = jsonObject.getAsJsonArray("upcomingFixtures")
                if (fixturesArray != null) {
                    for (elem in fixturesArray) {
                        val fObj = elem.asJsonObject ?: continue
                        fixturesList.add(
                            Fixture(
                                id = fObj.get("id")?.asInt ?: 0,
                                leagueName = fObj.get("leagueName")?.asString ?: "",
                                leagueCode = fObj.get("leagueCode")?.asString ?: "",
                                homeTeam = fObj.get("homeTeam")?.asString ?: "",
                                awayTeam = fObj.get("awayTeam")?.asString ?: "",
                                utcDate = fObj.get("utcDate")?.asString ?: "",
                                status = fObj.get("status")?.asString ?: "TIMED",
                                currentHomeScore = fObj.get("currentHomeScore")?.asInt,
                                currentAwayScore = fObj.get("currentAwayScore")?.asInt,
                                minute = fObj.get("minute")?.asInt
                            )
                        )
                    }
                }
            }

            ModelParameters(version, generatedAt, dataSource, rho, leaguesMap, fixturesList)
        } catch (_: Exception) {
            null
        }
    }

    private fun createFallbackParameters(): ModelParameters {
        val plTeams = mapOf(
            "Arsenal" to TeamStats(57, "Arsenal", elo = 1845.0, attack = 1.48, defense = 0.72, formPointsLast5 = 11, formGoalsScoredLast5 = 10, formGoalsConcededLast5 = 4),
            "Aston Villa" to TeamStats(58, "Aston Villa", elo = 1710.0, attack = 1.31, defense = 0.94, formPointsLast5 = 8, formGoalsScoredLast5 = 7, formGoalsConcededLast5 = 6),
            "Chelsea" to TeamStats(61, "Chelsea", elo = 1735.0, attack = 1.38, defense = 0.90, formPointsLast5 = 9, formGoalsScoredLast5 = 9, formGoalsConcededLast5 = 5),
            "Liverpool" to TeamStats(64, "Liverpool", elo = 1850.0, attack = 1.54, defense = 0.69, formPointsLast5 = 12, formGoalsScoredLast5 = 11, formGoalsConcededLast5 = 3),
            "Man City" to TeamStats(65, "Man City", elo = 1885.0, attack = 1.62, defense = 0.66, formPointsLast5 = 13, formGoalsScoredLast5 = 12, formGoalsConcededLast5 = 3),
            "Man United" to TeamStats(66, "Man United", elo = 1680.0, attack = 1.21, defense = 1.02, formPointsLast5 = 7, formGoalsScoredLast5 = 7, formGoalsConcededLast5 = 7),
            "Newcastle" to TeamStats(67, "Newcastle", elo = 1675.0, attack = 1.29, defense = 0.97, formPointsLast5 = 8, formGoalsScoredLast5 = 8, formGoalsConcededLast5 = 6),
            "Tottenham" to TeamStats(73, "Tottenham", elo = 1705.0, attack = 1.40, defense = 1.06, formPointsLast5 = 8, formGoalsScoredLast5 = 9, formGoalsConcededLast5 = 8)
        )

        val laLigaTeams = mapOf(
            "Real Madrid" to TeamStats(86, "Real Madrid", elo = 1895.0, attack = 1.66, defense = 0.64, formPointsLast5 = 13, formGoalsScoredLast5 = 12, formGoalsConcededLast5 = 3),
            "Barcelona" to TeamStats(81, "Barcelona", elo = 1870.0, attack = 1.64, defense = 0.67, formPointsLast5 = 12, formGoalsScoredLast5 = 13, formGoalsConcededLast5 = 4),
            "Atletico Madrid" to TeamStats(78, "Atletico Madrid", elo = 1780.0, attack = 1.36, defense = 0.74, formPointsLast5 = 10, formGoalsScoredLast5 = 9, formGoalsConcededLast5 = 4)
        )

        val leagues = mapOf(
            "Premier League" to LeagueData(2021, "PL", 1.28, 2.82, plTeams),
            "La Liga" to LeagueData(2014, "PD", 1.26, 2.62, laLigaTeams)
        )

        return ModelParameters("2.0.0", "Built-in Standalone Mode", "Offline Engine", -0.048, leagues, emptyList())
    }

    fun getSupportedLeagues(): List<String> = loadParameters().leagues.keys.toList()

    fun getTeamsForLeague(leagueName: String): List<TeamStats> {
        val league = loadParameters().leagues[leagueName] ?: return emptyList()
        return league.teams.values.sortedBy { it.name }
    }

    fun getUpcomingFixtures(leagueName: String): List<Fixture> {
        val allFixtures = loadParameters().upcomingFixtures
        return allFixtures.filter { it.leagueName.equals(leagueName, ignoreCase = true) }
    }

    fun getDataFreshness(): Pair<String, String> {
        val params = loadParameters()
        return params.generatedAt to params.dataSource
    }

    fun predictMatch(leagueName: String, homeTeamName: String, awayTeamName: String): MatchPrediction? {
        val params = loadParameters()
        val league = params.leagues[leagueName] ?: return null
        val home = league.teams[homeTeamName] ?: return null
        val away = league.teams[awayTeamName] ?: return null

        return DixonColesEngine.computeMatchPrediction(
            home = home,
            away = away,
            leagueName = leagueName,
            homeAdvantage = league.homeAdvantage,
            rho = params.dixonColesRho,
            generatedAt = params.generatedAt,
            dataSource = params.dataSource
        )
    }
}
