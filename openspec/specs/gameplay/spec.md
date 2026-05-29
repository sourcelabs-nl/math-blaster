# gameplay Specification

## Purpose

The core arithmetic shooter loop: the player reaches a target number by shooting falling
signed numbers that add to a running total, manages three lives, and wins by clearing enough
rounds. This spec describes the game as built.

## Requirements

### Requirement: Target and running total

The game SHALL present a target number and track a running total (the accumulator) that starts
at zero for each round.

#### Scenario: A new round starts

- **WHEN** a round begins
- **THEN** a fresh target between 5 and 20 is shown
- **AND** the running total is reset to 0

### Requirement: Shooting numbers adds to the total

The game SHALL add the value of each number the player shoots to the running total. Numbers are
signed: positives raise the total, negatives lower it.

#### Scenario: Shooting a positive number

- **WHEN** the total is 6 and the player shoots a +4
- **THEN** the total becomes 10

#### Scenario: Correcting an overshoot with a negative

- **WHEN** the total is 18, the target is 15, and the player shoots a -3
- **THEN** the total becomes 15

### Requirement: Clearing a round

The game SHALL clear the round when the running total equals the target exactly, award points,
and start a new round. Overshooting the target is allowed and carries no penalty.

#### Scenario: Landing on the target exactly

- **WHEN** a shot makes the total equal the target
- **THEN** the round is cleared and points are awarded
- **AND** a new target is shown and the total resets to 0

#### Scenario: Overshooting the target

- **WHEN** a shot makes the total exceed the target
- **THEN** the round is not cleared and no life is lost
- **AND** the player can shoot a negative number to come back down

### Requirement: Lives and game over

The game SHALL start the player with 3 lives and remove one life when a falling number reaches
the ship. The run ends when no lives remain.

#### Scenario: A falling number reaches the ship

- **WHEN** a falling number collides with the ship
- **THEN** one life is lost

#### Scenario: A falling number is missed harmlessly

- **WHEN** a falling number drifts off the bottom of the screen without hitting the ship
- **THEN** no life is lost

#### Scenario: Out of lives

- **WHEN** the last life is lost
- **THEN** the run ends and the game over screen is shown

### Requirement: Winning the run

The game SHALL end the run as a win once the player has cleared enough rounds to reach the win
score.

#### Scenario: Reaching the win score

- **WHEN** the player clears the final required round
- **THEN** the run is won and the player is invited to record their time

### Requirement: Difficulty levels

The game SHALL progress through two difficulty levels within a single run. Level 1 uses small
signed numbers (magnitude 1 to 10). After a set number of rounds are cleared the game SHALL
advance to Level 2, which keeps the same add-up-to-the-target loop but uses larger
denomination-style numbers (5, 10, 25, 50, 100, 250, 500), positive and negative, with larger
targets and a faster pace. The current level SHALL be shown to the player.

#### Scenario: Starting on Level 1

- **WHEN** a run begins
- **THEN** small signed numbers fall and the displayed level is 1

#### Scenario: Advancing to Level 2

- **WHEN** the player clears enough rounds to advance
- **THEN** the larger denomination numbers begin to fall and the displayed level is 2

### Requirement: Speed ramp and variety

The game SHALL make the falling numbers descend faster the longer a run lasts, up to the
current level's cap, and SHALL give individual numbers a small random speed variation so they
do not fall in lockstep.

#### Scenario: Speed ramps over time

- **WHEN** a run has been going for a while
- **THEN** numbers fall faster than at the start, without exceeding the cap
