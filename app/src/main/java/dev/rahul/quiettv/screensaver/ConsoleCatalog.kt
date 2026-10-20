package dev.rahul.quiettv.screensaver

// Palettes and simulated data labels ported from Rahul's MK-IV console.
data class ConsolePalette(val name: String, val bg: String, val fg: String, val accent: String, val grid: String)
data class ConsoleTopic(val name: String, val labels: List<String>)
object ConsoleCatalog {
    val palettes = listOf(
        ConsolePalette("ENGINEERING", "#f0f0f0", "#111111", "#ff3300", "#e0e0e0"),
        ConsolePalette("BLUEPRINT", "#0033cc", "#ffffff", "#ffff00", "#0044ee"),
        ConsolePalette("TERMINAL", "#0a0a0a", "#00ff41", "#008f11", "#112211"),
        ConsolePalette("MIDNIGHT", "#111111", "#eeeeee", "#666666", "#222222"),
        ConsolePalette("PAPER", "#fdfbf7", "#2d2d2d", "#d9534f", "#e8e6e1"),
        ConsolePalette("BRAUN", "#222222", "#e0e0e0", "#f4b400", "#333333"),
        ConsolePalette("VAPORWAVE", "#190a36", "#00ffff", "#ff69b4", "#331a66"),
        ConsolePalette("FOREST", "#1c3020", "#c4d7b2", "#a54a2a", "#2e4f3a"),
        ConsolePalette("MONOCHROME", "#292929", "#ffffff", "#888888", "#4a4a4a"),
        ConsolePalette("SOLARIS", "#002b36", "#fdf6e3", "#dc322f", "#073642"),
        ConsolePalette("NEON GRID", "#08081a", "#e0e0ff", "#ccff00", "#333355"),
        ConsolePalette("ACID PROTOCOL", "#111111", "#39ff14", "#ff00ff", "#333333"),
        ConsolePalette("BIOS ERROR", "#000084", "#ffffff", "#ffff00", "#0000aa"),
        ConsolePalette("HAZARD ZONE", "#f2c500", "#000000", "#ff0000", "#d9b000"),
        ConsolePalette("VIRTUAL RED", "#1a0000", "#ff0000", "#aa0000", "#4d0000"),
        ConsolePalette("CANDY POP", "#ffecf2", "#ff00cc", "#00ccff", "#ffdbe8"),
        ConsolePalette("ULTRAVIOLET", "#0f001a", "#bf00ff", "#00ff00", "#2a004d")
    )
    val topics = listOf(
        ConsoleTopic("AEROSPACE TELEMETRY", listOf("ALTITUDE", "PITCH", "YAW", "ROLL", "THRUST", "O2 LEVEL", "G-FORCE", "APOGEE", "PERIGEE", "BURN RATE", "PSI", "TEMP A", "TEMP B", "RADAR", "VELOCITY", "DRAG", "LIFT", "MACH", "SAT LOCK")),
        ConsoleTopic("SERVER INFRASTRUCTURE", listOf("CPU LOAD", "MEM USAGE", "I/O WAIT", "NET IN", "NET OUT", "DISK A", "DISK B", "REQS/SEC", "LATENCY", "UPTIME", "ERR RATE", "CACHE", "SWAP", "THREADS", "PIPES", "SOCKETS", "TEMP", "FAN RPM")),
        ConsoleTopic("MARKET ANALYTICS", listOf("NASDAQ", "DOW", "YIELD", "VOLATILITY", "ROI", "MARGIN", "VOLUME", "ASK", "BID", "SPREAD", "LIQUIDITY", "BONDS", "FUTURES", "OPTIONS", "FOREX", "CRYPTO", "INDEX", "DELTA")),
        ConsoleTopic("BIO-METRICS", listOf("BPM", "SPO2", "SYS BP", "DIA BP", "TEMP", "RESP RATE", "GLUCOSE", "LACTATE", "REM", "DEEP", "STEPS", "CALS", "EKG", "EEG", "EMG", "PH", "H2O"))
    )
    val speeds = listOf("Slow", "Normal", "Fast")
    val visualizations = listOf("Sine wave", "Histogram", "Gauge", "Radar", "Rolling line", "Matrix", "Wireframe cube", "Binary stream", "Target rings", "Hex dump", "Scatter", "Horizon", "Pie", "DNA helix", "Progress", "Spectrum", "Globe", "Terminal", "Vector field", "Circular buffer", "Waterfall", "Network", "Barcode", "Compass", "Liquid", "Seismic", "Orbit", "Fan", "Crosshairs", "Scanner")
}
