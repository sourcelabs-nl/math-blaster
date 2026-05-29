# presentation Specification

## Purpose

Deliver an 80s arcade look and sound. The whole experience is built from code and a single
font, with the audio synthesized at runtime so the game ships with no sound asset files.

## Requirements

### Requirement: Retro visual theme

The game SHALL use a single chunky pixel arcade font and a small neon-on-black color palette
across every screen, over an animated starfield backdrop.

#### Scenario: Consistent arcade styling

- **WHEN** any screen is shown
- **THEN** text uses the pixel arcade font
- **AND** the palette stays within the neon-on-black theme

### Requirement: Synthesized sound and music

The game SHALL generate its sound effects and looping music from waveforms at runtime rather
than loading audio files.

#### Scenario: Shooting and popping

- **WHEN** the player fires or destroys a falling number
- **THEN** a synthesized sound effect plays

### Requirement: Audio starts on first interaction

The game SHALL begin the background music only after the player's first interaction, so it
complies with browsers that block audio until a user gesture.

#### Scenario: Music begins when the player starts

- **WHEN** the player starts a game from the intro
- **THEN** the background music begins to loop

### Requirement: Mute control

The game SHALL let the player mute and unmute all audio at any time with a single key, and
SHALL resume the music in place when unmuted.

#### Scenario: Muting all audio

- **WHEN** the player presses the mute key
- **THEN** the music and sound effects fall silent until the player unmutes
