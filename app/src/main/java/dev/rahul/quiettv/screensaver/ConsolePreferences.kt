package dev.rahul.quiettv.screensaver

import android.content.Context
import org.json.JSONObject

object ConsolePreferences {
    private fun prefs(context: Context) = context.getSharedPreferences("screensaver", Context.MODE_PRIVATE)
    fun read(context: Context) = decode(prefs(context).getString("config", null))
    fun save(context: Context, config: ConsoleConfig): Boolean = prefs(context).edit().putString("config", encode(config.sanitized())).commit()
    fun encode(c: ConsoleConfig): String = JSONObject().put("theme", c.theme).put("topic", c.topic)
        .put("speed", c.speed).put("automatic", c.automatic).put("mixedPalettes", c.mixedPalettes)
        .put("density", c.density).put("cadence", c.cadence).put("fps", c.fps).put("brightness", c.brightness)
        .put("scanlines", c.scanlines).put("layoutSeconds", c.layoutSeconds).put("seed", c.seed)
        .put("replaceInstruments", c.replaceInstruments).toString()
    fun decode(value: String?): ConsoleConfig = try {
        val j = JSONObject(value ?: "{}")
        val defaults = ConsoleConfig()
        ConsoleConfig(j.optInt("theme", defaults.theme), j.optInt("topic", defaults.topic), j.optInt("speed", defaults.speed),
            j.optBoolean("automatic", defaults.automatic), j.optBoolean("mixedPalettes", defaults.mixedPalettes), j.optInt("density", defaults.density),
            j.optInt("cadence", defaults.cadence), j.optInt("fps", defaults.fps), j.optInt("brightness", defaults.brightness),
            j.optBoolean("scanlines", defaults.scanlines), j.optInt("layoutSeconds", defaults.layoutSeconds), j.optLong("seed", defaults.seed),
            j.optBoolean("replaceInstruments", defaults.replaceInstruments)).sanitized()
    } catch (_: Exception) { ConsoleConfig() }
}
