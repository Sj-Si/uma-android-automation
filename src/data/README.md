# Game Data Update Instructions

This directory contains Python scripts for scraping and updating the game data used by the Uma Musume Android Automation bot.

## Prerequisites

- **Python 3.10+**: Ensure you have Python installed and added to your PATH.
- **Google Chrome**: Required for scraping data via Selenium.
- **Chrome Driver**: Selenium will attempt to manage this automatically, but ensure your Chrome is up to date.

## Installation

Install the required Python dependencies using `pip`:

```bash
pip install -r requirements.txt
```

## Updating Game Data

To update all game data files (`skills.json`, `characters.json`, `supports.json`, and `races.json`), run the following command:

```bash
python main.py
```

### What this script does:

1.  **Skills**: Scrapes skill data, evaluation points (from Umamusume Wiki), and tier lists (from Game8).
2.  **Characters**: Scrapes character-specific training events and "After a Race" events.
3.  **Support Cards**: Scrapes support card training events and effects.
4.  **Races**: Scrapes race information and calculates turn numbers for the in-game calendar.

> [!NOTE]
> The script uses **Delta Scraping** by default (defined by `IS_DELTA = True` in `main.py`). This means it will only fetch new or updated items to save time. If you need a full refresh, set `IS_DELTA = False` in `main.py`.

## Utility Scripts

### `imageDetection.py`

This script is used to help find screen coordinates for new UI elements. It provides interactive windows with sliders to tune OpenCV detection parameters (Blur, Canny, Thresholds, etc.).

- Run it with: `python imageDetection.py`
- It uses the sample images in this directory (e.g., `imageDetectionSample.png`) to test detection logic.

## Data Files

- `characters.json`: Training events and options for all characters.
- `races.json`: Race calendar data.
- `skills.json`: Skill IDs, names, costs, and tier rankings.
- `supports.json`: Support card event data.
- `scenarios.json`: Scenario-specific data (e.g., URA, Unity Cup). This is updated manually whenever support for a new scenario is added.
