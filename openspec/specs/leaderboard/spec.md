# leaderboard Specification

## Purpose

Record how long a winning run took and rank the fastest runs, persisted locally in the browser
so scores survive between sessions without any backend.

## Requirements

### Requirement: Timed runs ranked fastest first

The game SHALL measure the elapsed time of each run and rank winning entries from fastest to
slowest.

#### Scenario: A faster run ranks higher

- **WHEN** a run finishing in 40.0s and a run finishing in 73.5s are both recorded
- **THEN** the 40.0s run is listed above the 73.5s run

### Requirement: Local-only persistence

The game SHALL store the leaderboard in browser local storage and use no backend or network.

#### Scenario: Scores survive a reload

- **WHEN** a player records a time and later reopens the game
- **THEN** the previously recorded times are still shown

### Requirement: Keep only the fastest runs

The game SHALL retain at most the 10 fastest entries, discarding slower ones when the list is
full.

#### Scenario: A slow run does not displace the top ten

- **WHEN** the list already holds 10 entries all faster than a new run
- **THEN** the new run is not added

### Requirement: Name entry for a winning run

The game SHALL let a winner type a name of up to 8 characters to attach to their time, and SHALL
use a default name when none is entered.

#### Scenario: Entering a name

- **WHEN** the player types a name and confirms
- **THEN** that name is stored next to their time

#### Scenario: Leaving the name blank

- **WHEN** the player confirms without typing a name
- **THEN** a default name is stored instead
