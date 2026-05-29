package game.model

/** One leaderboard row: a player [name] and how long their winning run took, in [timeMillis]. */
data class LeaderboardEntry(val name: String, val timeMillis: Int)
