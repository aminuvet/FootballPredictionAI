import os
import json
import math
import urllib.request
from datetime import datetime, timezone

API_KEY = os.environ.get("FOOTBALL_DATA_API_KEY", "")
BASE_URL = "https://api.football-data.org/v4"

COMPETITIONS = [
    {"code": "PL", "name": "Premier League", "id": 2021},
    {"code": "ELC", "name": "Championship", "id": 2016},
    {"code": "PD", "name": "La Liga", "id": 2014},
    {"code": "BL1", "name": "Bundesliga", "id": 2002},
    {"code": "SA", "name": "Serie A", "id": 2019},
    {"code": "FL1", "name": "Ligue 1", "id": 2015}
]

BASE_DATABASE = {
  "version": "2.0.0",
  "dixon_coles_rho": -0.048,
  "leagues": {
    "Premier League": {
      "id": 2021, "code": "PL", "home_advantage": 1.28, "league_avg_goals": 2.82,
      "teams": {
        "Arsenal": {"id": 57, "elo": 1845, "attack": 1.48, "defense": 0.72, "formPointsLast5": 11, "formGoalsScoredLast5": 10, "formGoalsConcededLast5": 4, "restDays": 6},
        "Aston Villa": {"id": 58, "elo": 1710, "attack": 1.31, "defense": 0.94, "formPointsLast5": 8, "formGoalsScoredLast5": 7, "formGoalsConcededLast5": 6, "restDays": 7},
        "Bournemouth": {"id": 1044, "elo": 1560, "attack": 1.08, "defense": 1.15, "formPointsLast5": 6, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 8, "restDays": 7},
        "Brentford": {"id": 402, "elo": 1580, "attack": 1.12, "defense": 1.10, "formPointsLast5": 7, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 7, "restDays": 7},
        "Brighton": {"id": 397, "elo": 1635, "attack": 1.22, "defense": 1.04, "formPointsLast5": 8, "formGoalsScoredLast5": 8, "formGoalsConcededLast5": 6, "restDays": 6},
        "Chelsea": {"id": 61, "elo": 1735, "attack": 1.38, "defense": 0.90, "formPointsLast5": 9, "formGoalsScoredLast5": 9, "formGoalsConcededLast5": 5, "restDays": 7},
        "Crystal Palace": {"id": 354, "elo": 1545, "attack": 0.96, "defense": 1.08, "formPointsLast5": 6, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 6, "restDays": 7},
        "Everton": {"id": 62, "elo": 1520, "attack": 0.91, "defense": 1.09, "formPointsLast5": 5, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 7, "restDays": 7},
        "Fulham": {"id": 63, "elo": 1565, "attack": 1.05, "defense": 1.07, "formPointsLast5": 7, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 6, "restDays": 7},
        "Ipswich": {"id": 349, "elo": 1450, "attack": 0.88, "defense": 1.34, "formPointsLast5": 4, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 10, "restDays": 7},
        "Leicester": {"id": 338, "elo": 1505, "attack": 0.98, "defense": 1.26, "formPointsLast5": 5, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 9, "restDays": 7},
        "Liverpool": {"id": 64, "elo": 1850, "attack": 1.54, "defense": 0.69, "formPointsLast5": 12, "formGoalsScoredLast5": 11, "formGoalsConcededLast5": 3, "restDays": 6},
        "Man City": {"id": 65, "elo": 1885, "attack": 1.62, "defense": 0.66, "formPointsLast5": 13, "formGoalsScoredLast5": 12, "formGoalsConcededLast5": 3, "restDays": 6},
        "Man United": {"id": 66, "elo": 1680, "attack": 1.21, "defense": 1.02, "formPointsLast5": 7, "formGoalsScoredLast5": 7, "formGoalsConcededLast5": 7, "restDays": 7},
        "Newcastle": {"id": 67, "elo": 1675, "attack": 1.29, "defense": 0.97, "formPointsLast5": 8, "formGoalsScoredLast5": 8, "formGoalsConcededLast5": 6, "restDays": 7},
        "Nottingham Forest": {"id": 351, "elo": 1530, "attack": 0.94, "defense": 1.12, "formPointsLast5": 6, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 7, "restDays": 7},
        "Southampton": {"id": 340, "elo": 1440, "attack": 0.84, "defense": 1.38, "formPointsLast5": 3, "formGoalsScoredLast5": 3, "formGoalsConcededLast5": 11, "restDays": 7},
        "Tottenham": {"id": 73, "elo": 1705, "attack": 1.40, "defense": 1.06, "formPointsLast5": 8, "formGoalsScoredLast5": 9, "formGoalsConcededLast5": 8, "restDays": 6},
        "West Ham": {"id": 74, "elo": 1595, "attack": 1.14, "defense": 1.16, "formPointsLast5": 7, "formGoalsScoredLast5": 7, "formGoalsConcededLast5": 8, "restDays": 7},
        "Wolves": {"id": 76, "elo": 1515, "attack": 0.95, "defense": 1.21, "formPointsLast5": 5, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 9, "restDays": 7}
      }
    },
    "Championship": {
      "id": 2016, "code": "ELC", "home_advantage": 1.24, "league_avg_goals": 2.58,
      "teams": {
        "Blackburn": {"id": 59, "elo": 1440, "attack": 1.04, "defense": 1.12, "formPointsLast5": 7, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 7, "restDays": 6},
        "Bristol City": {"id": 387, "elo": 1435, "attack": 0.98, "defense": 1.05, "formPointsLast5": 6, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 5, "restDays": 7},
        "Burnley": {"id": 328, "elo": 1535, "attack": 1.22, "defense": 0.84, "formPointsLast5": 10, "formGoalsScoredLast5": 8, "formGoalsConcededLast5": 4, "restDays": 7},
        "Cardiff": {"id": 355, "elo": 1420, "attack": 0.94, "defense": 1.14, "formPointsLast5": 5, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 7, "restDays": 7},
        "Coventry": {"id": 1076, "elo": 1455, "attack": 1.08, "defense": 1.06, "formPointsLast5": 7, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 6, "restDays": 6},
        "Derby": {"id": 342, "elo": 1410, "attack": 0.92, "defense": 1.18, "formPointsLast5": 5, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 8, "restDays": 7},
        "Hull City": {"id": 322, "elo": 1425, "attack": 0.96, "defense": 1.10, "formPointsLast5": 6, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 6, "restDays": 7},
        "Leeds": {"id": 341, "elo": 1565, "attack": 1.28, "defense": 0.86, "formPointsLast5": 11, "formGoalsScoredLast5": 9, "formGoalsConcededLast5": 4, "restDays": 7},
        "Luton": {"id": 389, "elo": 1470, "attack": 1.06, "defense": 1.08, "formPointsLast5": 7, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 6, "restDays": 7},
        "Middlesbrough": {"id": 343, "elo": 1475, "attack": 1.10, "defense": 1.01, "formPointsLast5": 8, "formGoalsScoredLast5": 7, "formGoalsConcededLast5": 5, "restDays": 7},
        "Millwall": {"id": 384, "elo": 1430, "attack": 0.92, "defense": 0.96, "formPointsLast5": 7, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 4, "restDays": 7},
        "Norwich": {"id": 68, "elo": 1460, "attack": 1.12, "defense": 1.09, "formPointsLast5": 7, "formGoalsScoredLast5": 7, "formGoalsConcededLast5": 7, "restDays": 7},
        "Oxford Utd": {"id": 1082, "elo": 1390, "attack": 0.88, "defense": 1.22, "formPointsLast5": 4, "formGoalsScoredLast5": 3, "formGoalsConcededLast5": 8, "restDays": 7},
        "Plymouth": {"id": 1138, "elo": 1395, "attack": 0.90, "defense": 1.24, "formPointsLast5": 4, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 9, "restDays": 7},
        "Portsmouth": {"id": 366, "elo": 1400, "attack": 0.91, "defense": 1.20, "formPointsLast5": 5, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 7, "restDays": 7},
        "Preston": {"id": 393, "elo": 1415, "attack": 0.93, "defense": 1.12, "formPointsLast5": 6, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 6, "restDays": 7},
        "QPR": {"id": 69, "elo": 1420, "attack": 0.95, "defense": 1.15, "formPointsLast5": 5, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 7, "restDays": 7},
        "Sheffield Utd": {"id": 356, "elo": 1525, "attack": 1.18, "defense": 0.89, "formPointsLast5": 9, "formGoalsScoredLast5": 7, "formGoalsConcededLast5": 4, "restDays": 7},
        "Sheffield Wed": {"id": 345, "elo": 1425, "attack": 0.97, "defense": 1.16, "formPointsLast5": 5, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 8, "restDays": 7},
        "Stoke": {"id": 70, "elo": 1430, "attack": 0.96, "defense": 1.11, "formPointsLast5": 6, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 6, "restDays": 7},
        "Sunderland": {"id": 71, "elo": 1495, "attack": 1.10, "defense": 0.97, "formPointsLast5": 9, "formGoalsScoredLast5": 7, "formGoalsConcededLast5": 5, "restDays": 7},
        "Swansea": {"id": 72, "elo": 1435, "attack": 0.97, "defense": 1.08, "formPointsLast5": 6, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 6, "restDays": 7},
        "Watford": {"id": 346, "elo": 1465, "attack": 1.06, "defense": 1.04, "formPointsLast5": 7, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 6, "restDays": 7},
        "West Brom": {"id": 75, "elo": 1485, "attack": 1.07, "defense": 0.94, "formPointsLast5": 8, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 5, "restDays": 7}
      }
    },
    "La Liga": {
      "id": 2014, "code": "PD", "home_advantage": 1.26, "league_avg_goals": 2.62,
      "teams": {
        "Alaves": {"id": 263, "elo": 1540, "attack": 0.92, "defense": 1.08, "formPointsLast5": 6, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 6, "restDays": 7},
        "Athletic Bilbao": {"id": 77, "elo": 1705, "attack": 1.24, "defense": 0.84, "formPointsLast5": 9, "formGoalsScoredLast5": 8, "formGoalsConcededLast5": 4, "restDays": 7},
        "Atletico Madrid": {"id": 78, "elo": 1780, "attack": 1.36, "defense": 0.74, "formPointsLast5": 10, "formGoalsScoredLast5": 9, "formGoalsConcededLast5": 4, "restDays": 6},
        "Barcelona": {"id": 81, "elo": 1870, "attack": 1.64, "defense": 0.67, "formPointsLast5": 12, "formGoalsScoredLast5": 13, "formGoalsConcededLast5": 4, "restDays": 6},
        "Celta Vigo": {"id": 558, "elo": 1570, "attack": 1.04, "defense": 1.14, "formPointsLast5": 6, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 8, "restDays": 7},
        "Espanyol": {"id": 80, "elo": 1510, "attack": 0.88, "defense": 1.20, "formPointsLast5": 5, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 8, "restDays": 7},
        "Getafe": {"id": 82, "elo": 1540, "attack": 0.88, "defense": 0.96, "formPointsLast5": 6, "formGoalsScoredLast5": 3, "formGoalsConcededLast5": 4, "restDays": 7},
        "Girona": {"id": 298, "elo": 1655, "attack": 1.28, "defense": 1.06, "formPointsLast5": 8, "formGoalsScoredLast5": 8, "formGoalsConcededLast5": 7, "restDays": 6},
        "Las Palmas": {"id": 275, "elo": 1520, "attack": 0.90, "defense": 1.16, "formPointsLast5": 5, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 7, "restDays": 7},
        "Leganes": {"id": 745, "elo": 1500, "attack": 0.84, "defense": 1.12, "formPointsLast5": 5, "formGoalsScoredLast5": 3, "formGoalsConcededLast5": 6, "restDays": 7},
        "Mallorca": {"id": 89, "elo": 1555, "attack": 0.92, "defense": 0.98, "formPointsLast5": 7, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 5, "restDays": 7},
        "Osasuna": {"id": 79, "elo": 1580, "attack": 1.02, "defense": 1.05, "formPointsLast5": 7, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 6, "restDays": 7},
        "Rayo Vallecano": {"id": 87, "elo": 1550, "attack": 0.94, "defense": 1.08, "formPointsLast5": 6, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 6, "restDays": 7},
        "Real Betis": {"id": 90, "elo": 1640, "attack": 1.14, "defense": 0.97, "formPointsLast5": 8, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 5, "restDays": 7},
        "Real Madrid": {"id": 86, "elo": 1895, "attack": 1.66, "defense": 0.64, "formPointsLast5": 13, "formGoalsScoredLast5": 12, "formGoalsConcededLast5": 3, "restDays": 6},
        "Real Sociedad": {"id": 92, "elo": 1675, "attack": 1.18, "defense": 0.86, "formPointsLast5": 8, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 4, "restDays": 6},
        "Real Valladolid": {"id": 250, "elo": 1490, "attack": 0.82, "defense": 1.28, "formPointsLast5": 4, "formGoalsScoredLast5": 3, "formGoalsConcededLast5": 9, "restDays": 7},
        "Sevilla": {"id": 559, "elo": 1605, "attack": 1.08, "defense": 1.08, "formPointsLast5": 7, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 6, "restDays": 7},
        "Valencia": {"id": 95, "elo": 1575, "attack": 1.01, "defense": 1.10, "formPointsLast5": 6, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 6, "restDays": 7},
        "Villarreal": {"id": 94, "elo": 1665, "attack": 1.30, "defense": 1.04, "formPointsLast5": 8, "formGoalsScoredLast5": 8, "formGoalsConcededLast5": 6, "restDays": 7}
      }
    },
    "Bundesliga": {
      "id": 2002, "code": "BL1", "home_advantage": 1.31, "league_avg_goals": 3.16,
      "teams": {
        "Augsburg": {"id": 16, "elo": 1550, "attack": 1.06, "defense": 1.18, "formPointsLast5": 6, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 8, "restDays": 7},
        "Bayer Leverkusen": {"id": 3, "elo": 1825, "attack": 1.58, "defense": 0.76, "formPointsLast5": 11, "formGoalsScoredLast5": 12, "formGoalsConcededLast5": 4, "restDays": 6},
        "Bayern Munich": {"id": 5, "elo": 1880, "attack": 1.72, "defense": 0.70, "formPointsLast5": 12, "formGoalsScoredLast5": 14, "formGoalsConcededLast5": 4, "restDays": 6},
        "Bochum": {"id": 36, "elo": 1495, "attack": 0.90, "defense": 1.36, "formPointsLast5": 4, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 11, "restDays": 7},
        "Borussia Dortmund": {"id": 4, "elo": 1750, "attack": 1.48, "defense": 0.90, "formPointsLast5": 9, "formGoalsScoredLast5": 10, "formGoalsConcededLast5": 6, "restDays": 6},
        "Eintracht Frankfurt": {"id": 19, "elo": 1655, "attack": 1.30, "defense": 1.04, "formPointsLast5": 8, "formGoalsScoredLast5": 8, "formGoalsConcededLast5": 6, "restDays": 6},
        "Freiburg": {"id": 17, "elo": 1605, "attack": 1.14, "defense": 1.06, "formPointsLast5": 7, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 6, "restDays": 7},
        "Heidenheim": {"id": 44, "elo": 1540, "attack": 1.02, "defense": 1.18, "formPointsLast5": 6, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 7, "restDays": 7},
        "Hoffenheim": {"id": 2, "elo": 1575, "attack": 1.20, "defense": 1.26, "formPointsLast5": 6, "formGoalsScoredLast5": 7, "formGoalsConcededLast5": 8, "restDays": 6},
        "Holstein Kiel": {"id": 720, "elo": 1470, "attack": 0.88, "defense": 1.38, "formPointsLast5": 3, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 11, "restDays": 7},
        "Mainz": {"id": 15, "elo": 1560, "attack": 1.04, "defense": 1.12, "formPointsLast5": 6, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 6, "restDays": 7},
        "Monchengladbach": {"id": 18, "elo": 1570, "attack": 1.18, "defense": 1.20, "formPointsLast5": 6, "formGoalsScoredLast5": 7, "formGoalsConcededLast5": 8, "restDays": 7},
        "RB Leipzig": {"id": 721, "elo": 1745, "attack": 1.42, "defense": 0.86, "formPointsLast5": 9, "formGoalsScoredLast5": 9, "formGoalsConcededLast5": 5, "restDays": 6},
        "St. Pauli": {"id": 29, "elo": 1485, "attack": 0.86, "defense": 1.22, "formPointsLast5": 4, "formGoalsScoredLast5": 3, "formGoalsConcededLast5": 8, "restDays": 7},
        "Stuttgart": {"id": 10, "elo": 1675, "attack": 1.34, "defense": 0.96, "formPointsLast5": 8, "formGoalsScoredLast5": 8, "formGoalsConcededLast5": 5, "restDays": 6},
        "Union Berlin": {"id": 28, "elo": 1565, "attack": 0.96, "defense": 1.04, "formPointsLast5": 7, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 5, "restDays": 7},
        "Werder Bremen": {"id": 12, "elo": 1555, "attack": 1.08, "defense": 1.16, "formPointsLast5": 6, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 7, "restDays": 7},
        "Wolfsburg": {"id": 11, "elo": 1585, "attack": 1.12, "defense": 1.14, "formPointsLast5": 7, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 6, "restDays": 7}
      }
    },
    "Serie A": {
      "id": 2019, "code": "SA", "home_advantage": 1.25, "league_avg_goals": 2.60,
      "teams": {
        "AC Milan": {"id": 98, "elo": 1750, "attack": 1.40, "defense": 0.86, "formPointsLast5": 9, "formGoalsScoredLast5": 8, "formGoalsConcededLast5": 5, "restDays": 6},
        "Atalanta": {"id": 102, "elo": 1740, "attack": 1.50, "defense": 0.92, "formPointsLast5": 9, "formGoalsScoredLast5": 10, "formGoalsConcededLast5": 5, "restDays": 6},
        "Bologna": {"id": 103, "elo": 1645, "attack": 1.16, "defense": 0.90, "formPointsLast5": 8, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 4, "restDays": 6},
        "Cagliari": {"id": 104, "elo": 1510, "attack": 0.90, "defense": 1.20, "formPointsLast5": 5, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 7, "restDays": 7},
        "Como": {"id": 1077, "elo": 1520, "attack": 0.94, "defense": 1.16, "formPointsLast5": 5, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 7, "restDays": 7},
        "Empoli": {"id": 445, "elo": 1525, "attack": 0.88, "defense": 1.08, "formPointsLast5": 6, "formGoalsScoredLast5": 3, "formGoalsConcededLast5": 5, "restDays": 7},
        "Fiorentina": {"id": 99, "elo": 1650, "attack": 1.20, "defense": 0.96, "formPointsLast5": 8, "formGoalsScoredLast5": 7, "formGoalsConcededLast5": 5, "restDays": 6},
        "Genoa": {"id": 107, "elo": 1540, "attack": 0.92, "defense": 1.08, "formPointsLast5": 6, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 5, "restDays": 7},
        "Inter": {"id": 108, "elo": 1855, "attack": 1.58, "defense": 0.66, "formPointsLast5": 12, "formGoalsScoredLast5": 11, "formGoalsConcededLast5": 3, "restDays": 6},
        "Juventus": {"id": 109, "elo": 1765, "attack": 1.30, "defense": 0.70, "formPointsLast5": 10, "formGoalsScoredLast5": 7, "formGoalsConcededLast5": 3, "restDays": 6},
        "Lazio": {"id": 110, "elo": 1670, "attack": 1.22, "defense": 0.96, "formPointsLast5": 8, "formGoalsScoredLast5": 7, "formGoalsConcededLast5": 5, "restDays": 6},
        "Lecce": {"id": 5890, "elo": 1490, "attack": 0.84, "defense": 1.22, "formPointsLast5": 4, "formGoalsScoredLast5": 3, "formGoalsConcededLast5": 8, "restDays": 7},
        "Monza": {"id": 5911, "elo": 1530, "attack": 0.92, "defense": 1.12, "formPointsLast5": 5, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 6, "restDays": 7},
        "Napoli": {"id": 113, "elo": 1735, "attack": 1.38, "defense": 0.80, "formPointsLast5": 9, "formGoalsScoredLast5": 8, "formGoalsConcededLast5": 4, "restDays": 7},
        "Parma": {"id": 112, "elo": 1515, "attack": 0.96, "defense": 1.22, "formPointsLast5": 5, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 8, "restDays": 7},
        "Roma": {"id": 100, "elo": 1680, "attack": 1.24, "defense": 0.94, "formPointsLast5": 8, "formGoalsScoredLast5": 7, "formGoalsConcededLast5": 5, "restDays": 6},
        "Torino": {"id": 586, "elo": 1575, "attack": 0.96, "defense": 1.00, "formPointsLast5": 7, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 5, "restDays": 7},
        "Udinese": {"id": 115, "elo": 1545, "attack": 0.98, "defense": 1.12, "formPointsLast5": 6, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 6, "restDays": 7},
        "Venezia": {"id": 454, "elo": 1475, "attack": 0.86, "defense": 1.30, "formPointsLast5": 4, "formGoalsScoredLast5": 3, "formGoalsConcededLast5": 9, "restDays": 7},
        "Verona": {"id": 450, "elo": 1500, "attack": 0.90, "defense": 1.24, "formPointsLast5": 5, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 8, "restDays": 7}
      }
    },
    "Ligue 1": {
      "id": 2015, "code": "FL1", "home_advantage": 1.27, "league_avg_goals": 2.74,
      "teams": {
        "Angers": {"id": 532, "elo": 1480, "attack": 0.86, "defense": 1.26, "formPointsLast5": 4, "formGoalsScoredLast5": 3, "formGoalsConcededLast5": 8, "restDays": 7},
        "Auxerre": {"id": 519, "elo": 1510, "attack": 0.94, "defense": 1.20, "formPointsLast5": 5, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 8, "restDays": 7},
        "Brest": {"id": 512, "elo": 1610, "attack": 1.10, "defense": 0.92, "formPointsLast5": 7, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 5, "restDays": 6},
        "Le Havre": {"id": 537, "elo": 1490, "attack": 0.84, "defense": 1.18, "formPointsLast5": 4, "formGoalsScoredLast5": 3, "formGoalsConcededLast5": 7, "restDays": 7},
        "Lens": {"id": 546, "elo": 1650, "attack": 1.20, "defense": 0.88, "formPointsLast5": 8, "formGoalsScoredLast5": 7, "formGoalsConcededLast5": 4, "restDays": 7},
       "Lille": {"id": 521, "elo": 1690, "attack": 1.28, "defense": 0.84, "formPointsLast5": 9, "formGoalsScoredLast5": 8, "formGoalsConcededLast5": 4, "restDays": 6},
        "Lyon": {"id": 523, "elo": 1670, "attack": 1.32, "defense": 1.02, "formPointsLast5": 8, "formGoalsScoredLast5": 8, "formGoalsConcededLast5": 6, "restDays": 6},
        "Marseille": {"id": 516, "elo": 1715, "attack": 1.38, "defense": 0.94, "formPointsLast5": 9, "formGoalsScoredLast5": 9, "formGoalsConcededLast5": 5, "restDays": 7},
        "Monaco": {"id": 548, "elo": 1725, "attack": 1.42, "defense": 0.90, "formPointsLast5": 9, "formGoalsScoredLast5": 9, "formGoalsConcededLast5": 5, "restDays": 6},
        "Montpellier": {"id": 518, "elo": 1500, "attack": 0.96, "defense": 1.32, "formPointsLast5": 4, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 10, "restDays": 7},
        "Nantes": {"id": 543, "elo": 1525, "attack": 0.92, "defense": 1.12, "formPointsLast5": 5, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 6, "restDays": 7},
        "Nice": {"id": 522, "elo": 1640, "attack": 1.14, "defense": 0.86, "formPointsLast5": 8, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 4, "restDays": 6},
        "Paris SG": {"id": 524, "elo": 1860, "attack": 1.68, "defense": 0.68, "formPointsLast5": 12, "formGoalsScoredLast5": 13, "formGoalsConcededLast5": 3, "restDays": 6},
        "Reims": {"id": 547, "elo": 1565, "attack": 1.05, "defense": 1.08, "formPointsLast5": 7, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 6, "restDays": 7},
        "Rennes": {"id": 529, "elo": 1615, "attack": 1.20, "defense": 1.06, "formPointsLast5": 7, "formGoalsScoredLast5": 7, "formGoalsConcededLast5": 6, "restDays": 7},
        "Saint-Etienne": {"id": 527, "elo": 1495, "attack": 0.88, "defense": 1.28, "formPointsLast5": 4, "formGoalsScoredLast5": 4, "formGoalsConcededLast5": 9, "restDays": 7},
        "Strasbourg": {"id": 576, "elo": 1550, "attack": 1.06, "defense": 1.16, "formPointsLast5": 6, "formGoalsScoredLast5": 6, "formGoalsConcededLast5": 7, "restDays": 7},
        "Toulouse": {"id": 511, "elo": 1540, "attack": 1.04, "defense": 1.14, "formPointsLast5": 6, "formGoalsScoredLast5": 5, "formGoalsConcededLast5": 6, "restDays": 7}
      }
    }
  }
}

def fetch_api(endpoint):
    url = f"{BASE_URL}/{endpoint}"
    req = urllib.request.Request(url, headers={"X-Auth-Token": API_KEY})
    try:
        with urllib.request.urlopen(req, timeout=12) as response:
            return json.loads(response.read().decode("utf-8", errors="ignore"))
    except Exception as e:
        print(f"Fetch note for {endpoint}: {e}")
        return None

def run():
    print("Executing Football Prediction Engine update pipeline...")

    data = BASE_DATABASE.copy()
    data["generatedAt"] = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    data["dataSource"] = "Football-Data.org Cloud Engine"

    upcoming_fixtures = []

    if API_KEY:
        print("API Key present. Fetching live schedules and scores...")
        for comp in COMPETITIONS:
            res = fetch_api(f"competitions/{comp['code']}/matches?status=SCHEDULED,IN_PLAY,FINISHED")
            if not res or "matches" not in res:
                continue

            league_key = comp["name"]
            if league_key not in data["leagues"]:
                continue

            for m in res["matches"]:
                status = m.get("status")
                h_name = m.get("homeTeam", {}).get("name")
                a_name = m.get("awayTeam", {}).get("name")
                m_date = m.get("utcDate", "")

                if status in ["SCHEDULED", "TIMED", "IN_PLAY"]:
                    score = m.get("score", {}).get("fullTime", {})
                    upcoming_fixtures.append({
                        "id": m.get("id"),
                        "leagueName": league_key,
                        "leagueCode": comp["code"],
                        "homeTeam": h_name,
                        "awayTeam": a_name,
                        "utcDate": m_date,
                        "status": status,
                        "currentHomeScore": score.get("home"),
                        "currentAwayScore": score.get("away")
                    })
    else:
        print("Running in pipeline mode with built-in parameter dictionary...")

    data["upcomingFixtures"] = upcoming_fixtures[:60]

    # Write out sanitized files
    os.makedirs("pipeline/data", exist_ok=True)
    out_file = "pipeline/data/model_parameters.json"
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)

    asset_file = "app/src/main/assets/model_parameters.json"
    os.makedirs("app/src/main/assets", exist_ok=True)
    with open(asset_file, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)

    print(f"Pipeline executed successfully. Synchronized {out_file} and {asset_file}.")

if __name__ == "__main__":
    run()
