package com.footballai.prediction

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.footballai.prediction.model.Fixture
import com.footballai.prediction.model.MatchPrediction
import com.footballai.prediction.model.TeamStats
import com.footballai.prediction.ui.theme.*
import kotlinx.coroutines.launch

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
    val coroutineScope = rememberCoroutineScope()
    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    val leagues = remember { repository.getSupportedLeagues() }
    var selectedLeague by remember { mutableStateOf(leagues.firstOrNull() ?: "Premier League") }

    val teams = remember(selectedLeague) { repository.getTeamsForLeague(selectedLeague) }
    var homeTeam by remember(selectedLeague) { mutableStateOf(teams.getOrNull(0)) }
    var awayTeam by remember(selectedLeague) { mutableStateOf(teams.getOrNull(1)) }

    var selectedTab by remember { mutableStateOf(0) } // 0: Upcoming Fixtures, 1: Custom Matchup
    var predictionResult by remember { mutableStateOf<MatchPrediction?>(null) }

    var leagueDropdownExpanded by remember { mutableStateOf(false) }
    var homeDropdownExpanded by remember { mutableStateOf(false) }
    var awayDropdownExpanded by remember { mutableStateOf(false) }

    val upcomingFixtures = remember(selectedLeague, isSyncing) {
        repository.getUpcomingFixtures(selectedLeague)
    }
    val (freshnessDate, dataSourceName) = repository.getDataFreshness()

    LaunchedEffect(Unit) {
        isSyncing = true
        val success = repository.syncRemoteParameters()
        syncMessage = if (success) "Live Parameters Synced" else "Using Local Cached Parameters"
        isSyncing = false
    }

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
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Updated: $freshnessDate",
                fontSize = 11.sp,
                color = TextSecondary
            )
            Text(
                text = if (isSyncing) "SYNCING..." else "ONLINE / CACHED",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSyncing) MedConfidenceColor else GreenPrimary
            )
        }

        // Mode Switcher Tab
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SurfaceDark,
            contentColor = GreenPrimary,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Upcoming Fixtures", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Custom Match", fontWeight = FontWeight.Bold) }
            )
        }

        Text(
            text = "SELECT COMPETITION",
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

        if (selectedTab == 0) {
            if (upcomingFixtures.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No live schedule fixtures found for $selectedLeague.", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Switch to 'Custom Match' tab above to predict any matchup.", fontSize = 12.sp, color = GreenPrimary)
                    }
                }
            } else {
                Text(
                    text = "SCHEDULED FIXTURES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                )
                upcomingFixtures.forEach { fixture ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                predictionResult = repository.predictMatch(
                                    leagueName = selectedLeague,
                                    homeTeamName = fixture.homeTeam,
                                    awayTeamName = fixture.awayTeam
                                )
                            },
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${fixture.homeTeam} vs ${fixture.awayTeam}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Date: ${fixture.utcDate.take(10)} | Status: ${fixture.status}", fontSize = 11.sp, color = TextSecondary)
                            }
                            Text("TAP TO PREDICT", fontSize = 11.sp, color = GreenPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
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

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

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
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "⚽ GENERATE PREDICTION",
                    color = BackgroundDark,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
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
                text = "${res.leagueName} • Dynamic Poisson Model",
                fontSize = 12.sp,
                color = TextSecondary
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFF334155))

            Text(
                text = "FULL-TIME OUTCOME PROBABILITY",
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
                    Text("${String.format("%.1f", res.homeWinProb)}%", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Draw", fontSize = 11.sp, color = TextSecondary)
                    Text("${String.format("%.1f", res.drawProb)}%", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Away Win", fontSize = 11.sp, color = TextSecondary)
                    Text("${String.format("%.1f", res.awayWinProb)}%", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF334155))

            Text(
                text = "EXPECTED GOALS (xG)",
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
                text = "MOST LIKELY EXACT SCORES",
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
                    Text("${idx + 1}.   ${score.homeGoals} - ${score.awayGoals}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("${String.format("%.1f", score.probability * 100.0)}%", fontSize = 13.sp, color = TextSecondary)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF334155))

            Text(
                text = "GOAL TOTALS, BTTS & CLEAN SHEETS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Over 1.5: ${String.format("%.1f", res.overUnder15.overProbability)}%   |   Under 1.5: ${String.format("%.1f", res.overUnder15.underProbability)}%", fontSize = 12.sp)
            Text("Over 2.5: ${String.format("%.1f", res.overUnder25.overProbability)}%   |   Under 2.5: ${String.format("%.1f", res.overUnder25.underProbability)}%", fontSize = 12.sp)
            Text("Over 3.5: ${String.format("%.1f", res.overUnder35.overProbability)}%   |   Under 3.5: ${String.format("%.1f", res.overUnder35.underProbability)}%", fontSize = 12.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text("BTTS YES: ${String.format("%.1f", res.bttsYesProb)}%   |   NO: ${String.format("%.1f", res.bttsNoProb)}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BlueSecondary)
            Text("${res.homeTeam} Clean Sheet: ${String.format("%.1f", res.cleanSheets.homeCleanSheetProb)}%   |   ${res.awayTeam}: ${String.format("%.1f", res.cleanSheets.awayCleanSheetProb)}%", fontSize = 12.sp)

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFF334155))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) 
