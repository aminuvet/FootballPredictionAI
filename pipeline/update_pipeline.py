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

def fetch_api(endpoint):
    url = f"{BASE_URL}/{endpoint}"
    req = urllib.request.Request(url, headers={"X-Auth-Token": API_KEY})
    try:
        with urllib.request.urlopen(req, timeout=12) as response:
            return json.loads(response.read().decode("utf-8", errors="ignore"), strict=False)
    except Exception as e:
        print(f"Fetch failed for {endpoint}: {e}")
        return None

def calculate_elo_update(r_home, r_away, score_h, score_a, k=24.0, home_adv=65.0):
    dr = (r_home + home_adv) - r_away
    e_home = 1.0 / (1.0 + math.pow(10.0, -dr / 400.0))
    e_away = 1.0 - e_home

    if score_h > score_a:
        s_home, s_away = 1.0, 0.0
    elif score_h == score_a:
        s_home, s_away = 0.5, 0.5
    else:
        s_home, s_away = 0.0, 1.0

    goal_diff = abs(score_h - score_a)
    mov = 1.0 if goal_diff <= 1 else 1.5 if goal_diff == 2 else (11.0 + goal_diff) / 8.0

    r_home_new = r_home + k * mov * (s_home - e_home)
    r_away_new = r_away + k * mov * (s_away - e_away)
    return r_home_new, r_away_new

def run():
    print("Starting Football Prediction Model update pipeline...")

    base_file = "app/src/main/assets/model_parameters.json"
    if os.path.exists(base_file):
        with open(base_file, "r", encoding="utf-8", errors="ignore") as f:
            raw_content = f.read()
            # Clean non-printable control characters
            clean_content = "".join(ch for ch in raw_content if ch >= " " or ch in "\n\r\t")
            data = json.loads(clean_content, strict=False)
    else:
        print("Base parameters file missing.")
        return

    data["generatedAt"] = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    data["dataSource"] = "Football-Data.org Cloud Engine"

    upcoming_fixtures = []

    if API_KEY:
        print("API Key detected. Fetching live fixtures and results...")
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
        print("No API Key configured. Running parameter validation and structure check...")

    data["upcomingFixtures"] = upcoming_fixtures[:60]

    os.makedirs("pipeline/data", exist_ok=True)
    out_file = "pipeline/data/model_parameters.json"
    with open(out_file, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)

    print(f"Pipeline successfully completed. Output written to {out_file}")

if __name__ == "__main__":
    run()
    
