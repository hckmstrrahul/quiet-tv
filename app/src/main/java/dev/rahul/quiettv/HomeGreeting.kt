package dev.rahul.quiettv

object HomeGreeting {
    fun forHour(hour: Int): String = when (hour.coerceIn(0,23) / 4) {
        0 -> "Hello, night owl."
        1 -> "Good morning."
        2 -> "Enjoy your morning."
        3 -> "Good afternoon."
        4 -> "Good evening."
        else -> "Time to unwind."
    }
}
