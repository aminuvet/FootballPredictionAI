package com.footballai.prediction.data

import android.content.Context
import com.footballai.prediction.engine.DixonColesEngine
import com.footballai.prediction.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.io.InputStreamReader

class PredictionRepository(private val context: Context) {

    private var cachedParameters: ModelParameters? = null

    private fun loadParameters(): ModelParameters {
        cachedParameters?.let { return it }

        val inputStream = context.assets.open("model_parameters.json")
        val jsonObject = Gson().fromJson(InputStreamReader(inputStream), JsonObject::class.java)

        val version = jsonObject.get("version").asString
        val rho = jsonObject.get("dixon_coles_rho").asDouble
        val leaguesJson = jsonObject.getAsJsonObject("leagues")

        val leaguesMap = mutableMapOf<String, LeagueData>()

        for (leagueKey in leaguesJson.keySet()) {
            val lObj = leaguesJson.getAsJsonObject(leagueKey)
            val homeAdv = lObj.get("home_advantage").asDouble
            val avgGoals = lObj.get("league_avg_goals").asDouble
            val teamsObj = lObj.getAsJsonObject("teams")

            val teamsMap = mutableMapOf<String, TeamStats>()
            for (teamKey in teamsObj.keySet()) {
                val tObj = teamsObj.getAsJsonObject(teamKey)
                teamsMap[teamKey] = TeamStats(
                    name = teamKey,
                    elo = tObj.get("elo").asDouble,
                    attack = tObj.get("attack").asDouble,
                    defense = tObj.get("defense").asDouble
                )
            }
            leaguesMap[leagueKey] = LeagueData(homeAdv, avgGoals, teamsMap)
        }

        val parameters = ModelParameters(version, rho, leaguesMap)
        cachedParameters = parameters
        return parameters
    }

    fun getSupportedLeagues(): List<String> {
        return loadParameters().leagues.keys.toList()
    }

    fun getTeamsForLeague(leagueName: String): List<TeamStats> {
        val league = loadParameters().leagues[leagueName] ?: return emptyList()
        return league.teams.values.sortedBy { it.name }
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
            rho = params.dixonColesRho
        )
    }
}
