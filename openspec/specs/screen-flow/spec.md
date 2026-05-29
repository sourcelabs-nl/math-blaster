# screen-flow Specification

## Purpose

Move the player between the intro, active play, a mid-run leaderboard pause, a quit
confirmation, and the finish screen, with clear keyboard controls throughout.

## Requirements

### Requirement: Intro screen

The game SHALL open on an intro screen that previews the first target, shows the current
leaderboard, lists the controls, and offers Start and Quit.

#### Scenario: Starting from the intro

- **WHEN** the player presses Start (or Enter/Space)
- **THEN** the game begins playing and the run timer starts

### Requirement: Pause to view the leaderboard mid-run

The game SHALL let the player open the leaderboard during play, pausing the action and the run
timer, and resume on the next key press.

#### Scenario: Peeking at the leaderboard

- **WHEN** the player opens the leaderboard during play
- **THEN** the action and timer pause
- **AND** pressing any key resumes the run where it left off

### Requirement: Quit confirmation

The game SHALL ask for confirmation before quitting and return the player to where they were if
they decline.

#### Scenario: Declining the quit prompt

- **WHEN** the player chooses No on the quit prompt
- **THEN** the game returns to the screen the prompt was raised from

### Requirement: Finish screen

The game SHALL show a finish screen on both winning and game over that displays the leaderboard
and lets the player start a new run.

#### Scenario: Playing again after finishing

- **WHEN** the finish screen is shown and the player presses R
- **THEN** a fresh run begins
