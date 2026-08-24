package com.footballai.prediction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.footballai.prediction.data.PredictionRepository
import com.footballai.prediction.model.MatchPrediction
import com.footballai.prediction.model.TeamStats
import com.footballai.prediction.ui.theme.*

class MainActivity : ComponentActivity() {

    private lateinit var repository: PredictionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = PredictionRepository(applicationContext)

        setContent {
            FootballPredictionAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PredictionMainScreen(repository)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionMainScreen(repository: PredictionRepository) {
    val leagues = remember { repository.getSupportedLeagues() }
    var selectedLeague by remember { mutableStateOf(leagues.firstOrNull() ?: "Premier League") }

    val teams = remember(selectedLeague) { repository.getTeamsForLeague(selectedLeague) }
    var homeTeam by remember(selectedLeague) { mutableStateOf(teams.getOrNull(0)) }
    var awayTeam by remember(selectedLeague) { mutableStateOf(teams.getOrNull(1)) }

    var predictionResult by remember { mutableStateOf<MatchPrediction?>(null) }

    var leagueDropdownExpanded by remember { mutableStateOf(false) }
    var homeDropdownExpanded by remember { mutableStateOf(false) }
    var awayDropdownExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚽ FOOTBALL PREDICTION AI",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "Probability-Based Football Match Analysis",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        Text(
            text = "LEAGUE",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )
        ExposedDropdownMenuBox(
            expanded = leagueDropdownExpanded,
            onExpandedChange = { leagueDropdownExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedLeague,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = leagueDropdownExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = SurfaceDark,
                    focusedContainerColor = SurfaceDark
                )
            )
            ExposedDropdownMenu(
                expanded = leagueDropdownExpanded,
                onDismissRequest = { leagueDropdownExpanded = false }
            ) {
                leagues.forEach { league ->
                    DropdownMenuItem(
                        text = { Text(league, fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            selectedLeague = league
                            leagueDropdownExpanded = false
                            predictionResult = null
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "HOME TEAM",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )
        ExposedDropdownMenuBox(
            expanded = homeDropdownExpanded,
            onExpandedChange = { homeDropdownExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = homeTeam?.name ?: "Select Home Team",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = homeDropdownExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = SurfaceDark,
                    focusedContainerColor = SurfaceDark
                )
            )
            ExposedDropdownMenu(
                expanded = homeDropdownExpanded,
                onDismissRequest = { homeDropdownExpanded = false }
            ) {
                teams.forEach { team ->
                    DropdownMenuItem(
                        text = { Text(team.name) },
                        onClick = {
                            homeTeam = team
                            homeDropdownExpanded = false
                        },
                        enabled = team.name != awayTeam?.name
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "AWAY TEAM",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
        )
        ExposedDropdownMenuBox(
            expanded = awayDropdownExpanded,
            onExpandedChange = { awayDropdownExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = awayTeam?.name ?: "Select Away Team",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = awayDropdownExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = SurfaceDark,
                    focusedContainerColor = SurfaceDark
                )
            )
            ExposedDropdownMenu(
                expanded = awayDropdownExpanded,
                onDismissRequest = { awayDropdownExpanded = false }
            ) {
                teams.forEach { team ->
                    DropdownMenuItem(
                        text = { Text(team.name) },
                        onClick = {
                            awayTeam = team
                            awayDropdownExpanded = false
                        },
                        enabled = team.name != homeTeam?.name
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (homeTeam != null && awayTeam != null && homeTeam?.name != awayTeam?.name) {
                    predictionResult = repository.predictMatch(
                        leagueName = selectedLeague,
                        homeTeamName = homeTeam!!.name,
                        awayTeamName = awayTeam!!.name
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "⚽ PREDICT MATCH",
                color = BackgroundDark,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        predictionResult?.let { res ->
            PredictionOutputCard(res)
        }
    }
}

@Composable
fun PredictionOutputCard(res: MatchPrediction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "${res.homeTeam.uppercase()} vs ${res.awayTeam.uppercase()}",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GreenPrimary
            )
            Text(
                text = res.leagueName,
                fontSize = 12.sp,
                color = TextSecondary
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF334155))

            Text(
                text = "FULL-TIME PROBABILITY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Home Win", fontSize = 11.sp, color = TextSecondary)
                    Text("${String.format("%.1f", res.homeWinProb)}%", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Draw", fontSize = 11.sp, color = TextSecondary)
                    Text("${String.format("%.1f", res.drawProb)}%", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Away Win", fontSize = 11.sp, color = TextSecondary)
                    Text("${String.format("%.1f", res.awayWinProb)}%", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF334155))

            Text(
                text = "MODEL EXPECTED GOALS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Home: ${String.format("%.2f", res.expHomeGoals)}", fontSize = 13.sp)
                Text("Away: ${String.format("%.2f", res.expAwayGoals)}", fontSize = 13.sp)
                Text("Total: ${String.format("%.2f", res.expTotalGoals)}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF334155))

            Text(
                text = "MOST LIKELY SCORELINES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            res.topScores.forEachIndexed { idx, score ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${idx + 1}.  ${score.homeGoals} - ${score.awayGoals}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("${String.format("%.1f", score.probability * 100.0)}%", fontSize = 13.sp, color = TextSecondary)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF334155))

            Text(
                text = "GOAL TOTALS & BTTS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Over 1.5: ${String.format("%.1f", res.overUnder15.overProbability)}%  |  Under 1.5: ${String.format("%.1f", res.overUnder15.underProbability)}%", fontSize = 12.sp)
            Text("Over 2.5: ${String.format("%.1f", res.overUnder25.overProbability)}%  |  Under 2.5: ${String.format("%.1f", res.overUnder25.underProbability)}%", fontSize = 12.sp)
            Text("Over 3.5: ${String.format("%.1f", res.overUnder35.overProbability)}%  |  Under 3.5: ${String.format("%.1f", res.overUnder35.underProbability)}%", fontSize = 12.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text("BTTS YES: ${String.format("%.1f", res.bttsYesProb)}%  |  NO: ${String.format("%.1f", res.bttsNoProb)}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BlueSecondary)

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF334155))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MODEL CONFIDENCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                val (color, label) = when (res.confidence) {
                    "HIGH" -> HighConfidenceColor to "HIGH"
                    "MEDIUM" -> MedConfidenceColor to "MEDIUM"
                    else -> LowConfidenceColor to "LOW"
                }
                Text(
                    text = label,
                    color = color,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF334155))

            Text(
                text = "KEY STATISTICAL FACTORS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            res.keyFactors.forEach { factor ->
                Text("• $factor", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
}
