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

    // Replace with your GitHub raw URL or CDN endpoint once published
    private val remoteDataUrl =
        "https://raw.githubusercontent.com/aminuvet/FootballPredictionAI/main/pipeline/data/model_parameters.json"

    suspend fun syncRemoteParameters(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(remoteDataUrl).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonString = response.body?.string() ?: return@withContext false
                val cacheFile = File(context.filesDir, cacheFileName)
                cacheFile.writeText(jsonString)
                cachedParameters = parseJsonString(jsonString)
                return@withContext true
            }
        } catch (_: Exception) {
            // Gracefully fall back to local disk cache or bundled asset
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
                cachedParameters = parsed
                return parsed
            } catch (_: Exception) {
                cacheFile.delete()
            }
        }

        val inputStream = context.assets.open("model_parameters.json")
        val parsed = parseJsonString(InputStreamReader(inputStream).readText())
        cachedParameters = parsed
        return parsed
    }

    private fun parseJsonString(jsonString: String): ModelParameters {
        val jsonObject = gson.fromJson(jsonString, JsonObject::class.java)
        val version = jsonObject.get("version")?.asString ?: "2.0.0"
        val generatedAt = jsonObject.get("generatedAt")?.asString ?: "Bundled Asset Fallback"
        val dataSource = jsonObject.get("dataSource")?.asString ?: "Football-Data.org Cloud"
        val rho = jsonObject.get("dixon_coles_rho")?.asDouble ?: -0.048
        val leaguesJson = jsonObject.getAsJsonObject("leagues")

        val leaguesMap = mutableMapOf<String, LeagueData>()

        for (leagueKey in leaguesJson.keySet()) {
            val lObj = leaguesJson.getAsJsonObject(leagueKey)
            val leagueId = lObj.get("id")?.asInt ?: 0
            val leagueCode = lObj.get("code")?.asString ?: ""
            val homeAdv = lObj.get("home_advantage")?.asDouble ?: 1.25
            val avgGoals = lObj.get("league_avg_goals")?.asDouble ?: 2.70
            val teamsObj = lObj.getAsJsonObject("teams")

            val teamsMap = mutableMapOf<String, TeamStats>()
            for (teamKey in teamsObj.keySet()) {
                val tObj = teamsObj.getAsJsonObject(teamKey)
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
            for (elem in fixturesArray) {
                val fObj = elem.asJsonObject
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

        return ModelParameters(version, generatedAt, dataSource, rho, leaguesMap, fixturesList)
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
