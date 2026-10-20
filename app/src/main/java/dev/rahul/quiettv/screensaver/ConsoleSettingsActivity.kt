package dev.rahul.quiettv.screensaver

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import kotlin.random.Random
import dev.rahul.quiettv.R
import dev.rahul.quiettv.SettingsAccordion
import dev.rahul.quiettv.DialogUi

/** Draft changes are previewed locally; only Save & apply writes persistent settings. */
class ConsoleSettingsActivity : Activity() {
    private var draft = ConsoleConfig()
    private lateinit var preview: ConsoleView
    private lateinit var accordion: SettingsAccordion
    private var themeDialog:AlertDialog?=null
    private var resumed=false
    private val labels = mutableListOf<Pair<TextView, () -> String>>()
    private val ink = Color.rgb(233, 238, 242)
    private val muted = Color.rgb(141, 158, 174)
    private val accent = Color.rgb(170, 219, 203)
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun text(value: String, size: Float, color: Int = ink) = TextView(this).apply {
        this.text = value; textSize = size; setTextColor(color); includeFontPadding = false
    }
    private fun shape(focus: Boolean) = GradientDrawable().apply {
        setColor(if (focus) 0x30FFFFFF else 0x0CFFFFFF)
        cornerRadius = dp(9).toFloat()
    }
    private fun button(value: String, iconRes: Int, action: () -> Unit) = text(value, 13f).apply {
        setPadding(dp(13), dp(11), dp(13), dp(11)); gravity = Gravity.CENTER_VERTICAL
        isFocusable = true; isClickable = true; id = View.generateViewId(); background = shape(false)
        setOnFocusChangeListener { v, focused -> v.background = shape(focused) }
        compoundDrawablePadding = dp(12)
        setCompoundDrawablesRelative(icon(iconRes), null, null, null)
        setOnClickListener { action() }
    }
    private fun actionButton(value:String,iconRes:Int,action:()->Unit)=LinearLayout(this).apply {
        orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER
        setPadding(dp(13),dp(11),dp(13),dp(11))
        isFocusable=true;isClickable=true;id=View.generateViewId();background=shape(false)
        contentDescription=value
        setOnFocusChangeListener { v,focused->v.background=shape(focused) }
        setOnClickListener {action()}
        addView(ImageView(context).apply {setImageDrawable(icon(iconRes));importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO},LinearLayout.LayoutParams(dp(21),dp(21)).apply {marginEnd=dp(10)})
        addView(text(value,13f).apply {importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO})
    }
    private fun icon(resource: Int) = getDrawable(resource)!!.mutate().apply {
        setTint(accent); setBounds(0, 0, dp(21), dp(21))
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        draft = state?.getString("draft")?.let { ConsolePreferences.decode(it) } ?: ConsolePreferences.read(this)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val root = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(34), dp(26), dp(34), dp(22)); setBackgroundColor(Color.rgb(12,16,24)) }
        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0,0,dp(26),0) }
        root.addView(left, LinearLayout.LayoutParams(0, -1, 1.15f))
        left.addView(text("QUIET TV  /  SCREENSAVER", 11f, accent).apply { letterSpacing = .15f })
        left.addView(text("Omni-data console MK-IV", 28f).apply {
            setFontVariationSettings("'wght' 500")
            maxLines = 1
            setAutoSizeTextTypeUniformWithConfiguration(22,28,1,android.util.TypedValue.COMPLEX_UNIT_SP)
            setPadding(0,dp(10),0,dp(8))
        })
        left.addView(text("Procedural data-visualisation screensaver", 13f, muted))
        preview = ConsoleView(this, draft)
        left.addView(TvPreviewFrame(this, preview), LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(18); bottomMargin = dp(12) })
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(actionButton("Preview", R.drawable.huge_preview) { startActivity(Intent(this, ConsolePreviewActivity::class.java).putExtra("config", ConsolePreferences.encode(draft))) }, LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=dp(8) })
        actions.addView(actionButton("Save & apply", R.drawable.huge_save) {
            if (ConsolePreferences.save(this, draft)) Toast.makeText(this,"Screensaver settings saved",Toast.LENGTH_SHORT).show()
            else Toast.makeText(this,R.string.console_save_failed,Toast.LENGTH_LONG).show()
        }, LinearLayout.LayoutParams(0,-2,1f))
        left.addView(actions)
        val scroll = ScrollView(this).apply { isFillViewport=true; clipToPadding=false }
        root.addView(scroll, LinearLayout.LayoutParams(0,-1,1f))
        val controls = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        scroll.addView(controls)
        controls.addView(text("Make it yours", 21f).apply { setFontVariationSettings("'wght' 500");setPadding(0,0,0,dp(16)) })
        accordion=SettingsAccordion(this)
        controls.addView(accordion)
        val colorGroup=accordion.section("Color & mood",R.drawable.huge_colors)
        val layoutGroup=accordion.section("Layout & data",R.drawable.huge_density)
        val motionGroup=accordion.section("Motion",R.drawable.huge_evolution)
        val displayGroup=accordion.section("Display",R.drawable.huge_brightness)
        val systemGroup=accordion.section("System & reset",R.drawable.huge_settings)
        var destination=colorGroup
        fun control(iconRes: Int, label: () -> String, action: () -> Unit) {
            val b = button(label(),iconRes,action); labels.add(b to label)
            destination.addView(b, LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(7) })
        }
        destination=colorGroup
        control(R.drawable.huge_palette, { "Theme  ·  ${ConsoleCatalog.palettes[draft.theme].name}" }) { showThemes() }
        control(R.drawable.huge_forever, { randomColorsLabel() }) {
            change(draft.withRandomColorsForever(!draft.randomColorsForever))
        }
        control(R.drawable.huge_mode, { "Mode  ·  ${if(draft.automatic) "Ever-changing" else "Hold colors"}" }) { change(draft.copy(automatic=!draft.automatic)) }
        control(R.drawable.huge_colors, { "Panel palettes  ·  ${if(draft.mixedPalettes) "Random colors" else "Selected theme only"}" }) { change(draft.copy(mixedPalettes=!draft.mixedPalettes)) }
        destination=layoutGroup
        control(R.drawable.huge_source, { "Source  ·  ${ConsoleCatalog.topics[draft.topic].name}" }) { choose(R.drawable.huge_source, "Simulated data theme",ConsoleCatalog.topics.map { it.name },draft.topic) { change(draft.copy(topic=it)) } }
        control(R.drawable.huge_density, { "Density  ·  ${listOf("Spacious","Balanced","Dense")[draft.density]}" }) { choose(R.drawable.huge_density, "Grid density",listOf("Spacious · 8 × 4 grid","Balanced · 10 × 6 grid","Dense · 12 × 7 grid"),draft.density) { change(draft.copy(density=it)) } }
        control(R.drawable.huge_shuffle, { "Reshuffle layout  ·  ${if(draft.layoutSeconds==0) "Off" else "${draft.layoutSeconds}s"}" }) { val values=listOf(0,30,60,120); choose(R.drawable.huge_shuffle, "Automatic layout shuffle",listOf("Off","Every 30 seconds","Every minute","Every 2 minutes"),values.indexOf(draft.layoutSeconds)) { change(draft.copy(layoutSeconds=values[it])) } }
        control(R.drawable.huge_shuffle, { "Shuffle now" }) { change(draft.copy(seed=Random.nextLong())) }
        destination=motionGroup
        control(R.drawable.huge_speed, { "Speed  ·  ${ConsoleCatalog.speeds[draft.speed]}" }) { choose(R.drawable.huge_speed, "Animation speed",ConsoleCatalog.speeds,draft.speed) { change(draft.copy(speed=it)) } }
        control(R.drawable.huge_timer, { "Change interval  ·  ${listOf("Rapid · original feel","Gentle","Slow")[draft.cadence]}" }) { choose(R.drawable.huge_timer, "Evolve one panel at a time",listOf("Rapid · about 0.5 seconds","Gentle · about 2 seconds","Slow · about 5 seconds"),draft.cadence) { change(draft.copy(cadence=it)) } }
        control(R.drawable.huge_evolution, { "Evolution  ·  ${if(draft.replaceInstruments) "Colors + new instruments" else "Colors only"}" }) { change(draft.copy(replaceInstruments=!draft.replaceInstruments)) }
        destination=displayGroup
        control(R.drawable.huge_brightness, { "Brightness  ·  ${draft.brightness}%" }) { val values=listOf(35,50,70,85,100);choose(R.drawable.huge_brightness, "Artwork brightness",values.map { "$it%" },values.indexOf(draft.brightness)) { change(draft.copy(brightness=values[it])) } }
        control(R.drawable.huge_frames, { "Frame limit  ·  ${draft.fps} fps" }) { choose(R.drawable.huge_frames, "Frame limit",listOf("20 fps · lighter","30 fps · smoother"),if(draft.fps==20)0 else 1) { change(draft.copy(fps=if(it==0)20 else 30)) } }
        control(R.drawable.huge_scanlines, { "Scanlines  ·  ${if(draft.scanlines) "On" else "Off"}" }) { change(draft.copy(scanlines=!draft.scanlines)) }
        destination=systemGroup
        control(R.drawable.huge_reset, { "Restore default design" }) { change(ConsoleConfig()) }
        control(R.drawable.huge_settings, { "System screensaver settings" }) {
            try { startActivity(Intent(Settings.ACTION_DREAM_SETTINGS)) }
            catch (_: Exception) { Toast.makeText(this,"Open TV settings → Device Preferences → Screen saver",Toast.LENGTH_LONG).show() }
        }
        accordion.expand(state?.getInt("expanded",0) ?: 0)
        controls.addView(text("Created by @hckmstrrahul · MK-IV native adaptation",11f,muted).apply { setPadding(0,dp(8),0,dp(12)) })
        setContentView(root)
        accordion.focusFirst()
    }
    private fun choose(iconRes: Int, title: String, items: List<String>, selected: Int, apply: (Int) -> Unit) {
        DialogUi.choose(this,title,items,selected,iconRes,apply)
    }
    private fun showThemes() {
        if(themeDialog!=null)return
        val host=FrameLayout(this)
        val dialog=AlertDialog.Builder(this).setTitle("Theme").setView(host).setNegativeButton("Close",null).create()
        themeDialog=dialog;preview.stop()
        dialog.setOnDismissListener {themeDialog=null;if(resumed && !isFinishing)preview.start()}
        dialog.show();DialogUi.style(dialog,820)
        val gallery=ThemeGallery(this,draft.theme,dialog.getButton(AlertDialog.BUTTON_NEGATIVE).id) {index->
            change(draft.copy(theme=index));dialog.dismiss()
        }
        host.addView(gallery,FrameLayout.LayoutParams(-1,-1))
        val height=dp(520).coerceAtMost(resources.displayMetrics.heightPixels-dp(32))
        host.layoutParams=host.layoutParams.apply {this.height=height-dp(140)}
        dialog.window?.setLayout(dialog.window!!.attributes.width,height)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).nextFocusUpId=gallery.selectedCard.id
        gallery.selectedCard.requestFocus()
    }
    private fun randomColorsLabel() = "Random colors forever  ·  ${if (draft.randomColorsForever) "On" else "Off"}"
    private fun change(value: ConsoleConfig) {
        draft=value.sanitized();preview.configure(draft);labels.forEach { (view,label)->view.text=label() }
    }
    override fun onResume() { super.onResume();resumed=true;if(themeDialog==null)preview.start() }
    override fun onPause() { resumed=false;themeDialog?.dismiss();preview.stop();super.onPause() }
    override fun onDestroy() { preview.stop();super.onDestroy() }
    override fun onSaveInstanceState(out: Bundle) { out.putString("draft",ConsolePreferences.encode(draft));out.putInt("expanded",accordion.expandedIndex);super.onSaveInstanceState(out) }
    override fun dump(prefix: String, fd: java.io.FileDescriptor?, writer: java.io.PrintWriter, args: Array<out String>?) {
        super.dump(prefix,fd,writer,args);writer.println("${prefix}MKIV settings: animating=${preview.animating} frames=${preview.frameCount} ${preview.diagnostics()}")
    }
}
