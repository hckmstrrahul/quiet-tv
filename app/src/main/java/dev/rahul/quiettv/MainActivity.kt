package dev.rahul.quiettv

import android.app.Activity
import android.app.AlertDialog
import android.content.*
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import org.json.JSONArray
import java.util.*

/** Native TV launcher with an opt-in, foreground-only LAN editor. */
class MainActivity : Activity() {
    private lateinit var companion: CompanionEditor
    private var pairingDialog: AlertDialog? = null
    private val connectTile = "quiet-tv:connect"
    private val screensaverTile = "quiet-tv:screensaver"
    private val prefs by lazy { getSharedPreferences("home", MODE_PRIVATE) }
    private val handler = Handler(Looper.getMainLooper())
    private val visuals = ForegroundTasks { task -> handler.post { task() } }
    private var resumed = false
    private val navigationSound by lazy { HomeNavigationSound(this) }
    private var hoveredItem:View?=null
    private var navigationKeyPending=false
    private val homeFocusListener=ViewTreeObserver.OnGlobalFocusChangeListener {old,new->
        if(navigationKeyPending && resumed && hasWindowFocus() && new!=null && new!==old) {
            navigationKeyPending=false;navigationSound.play()
        }
    }
    private var generation = 0
    private var renderGeneration = 0
    private var renderCount = 0
    private var selectedPackage: String? = null
    private var apps = listOf<TvApp>()
    private lateinit var root: FrameLayout
    private lateinit var body: LinearLayout
    private var music: MusicWidget? = null
    private var weather: WeatherWidget? = null
    private var refreshInputPills: (() -> Unit)? = null
    private val inputManager by lazy { getSystemService(TV_INPUT_SERVICE) as TvInputManager }
    private val inputCallback=object:TvInputManager.TvInputCallback() {
        override fun onInputStateChanged(id:String,state:Int) { refreshInputPills?.invoke() }
        override fun onInputAdded(id:String) { refreshInputPills?.invoke() }
        override fun onInputRemoved(id:String) { refreshInputPills?.invoke() }
        override fun onInputUpdated(id:String) { refreshInputPills?.invoke() }
        override fun onTvInputInfoUpdated(info:TvInputInfo) { refreshInputPills?.invoke() }
    }
    private lateinit var clock: HomeTimeWidget
    private var tick: Runnable? = null
    private var backdrop:HomeBackdrop?=null
    private var homeSettings:AlertDialog?=null
    private val tileEffects=mutableListOf<TileFocusEffect>()
    private var inputRows=listOf<HdmiMenu.Entry>()
    private val white = Color.rgb(239, 243, 247)
    private val muted = Color.rgb(154, 169, 185)
    private val accents = listOf("Sage" to "#AADBCB", "Ice" to "#94C8FF", "Lavender" to "#C9B7F5", "Amber" to "#EACA92")
    private val accent get() = Color.parseColor(prefs.getString("accent", "#AADBCB"))
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private data class TvApp(val pkg: String, val label: String, val info: ResolveInfo?)
    private fun strings(key: String): MutableList<String> = try {
        val a = JSONArray(prefs.getString(key, "[]")); MutableList(a.length()) { a.getString(it) }
    } catch (_: Exception) { mutableListOf() }
    private fun save(key: String, values: List<String>) { prefs.edit().putString(key, JSONArray(values).toString()).apply() }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        if (!prefs.getBoolean("dense_tiles_v1",false)) prefs.edit().putInt("columns",6).putBoolean("dense_tiles_v1",true).apply()
        prefs.edit().apply { prefs.all.keys.filter { it=="favorites" || it=="groups" || it.startsWith("group:") }.forEach { remove(it) } }.apply()
        if(!prefs.getBoolean("gallery_v1",false))prefs.edit().putBoolean("gallery_v1",true).putString("wallpaperId",WallpaperCatalog.defaultId).putBoolean("wallpaperShuffle",false).putBoolean("photo",false).apply()
        selectedPackage = state?.getString("selected")
        companion = CompanionEditor(this, { apps.map { it.pkg to it.label } }, { companionPreview() }) {
            pairingDialog?.dismiss()
            render()
        }
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.viewTreeObserver.addOnGlobalFocusChangeListener(homeFocusListener)
    }
    internal fun companionPreview(): org.json.JSONObject {
        val order = strings("order")
        val sorted = apps.sortedBy { order.indexOf(it.pkg).let { index -> if (index < 0) Int.MAX_VALUE else index } }
        return HomePreview.capture(body, listOf(connectTile, screensaverTile) + sorted.map { it.pkg })
    }
    override fun onResume() {
        super.onResume()
        resumed = true
        navigationSound.start()
        visuals.start()
        inputManager.registerCallback(inputCallback,handler)
        loadApps()
    }
    override fun onPause() {
        resumed = false
        navigationSound.stop();hoveredItem=null;navigationKeyPending=false
        if(::clock.isInitialized)clock.stop()
        inputManager.unregisterCallback(inputCallback);refreshInputPills=null;weather?.stop()
        backdrop?.pause();music?.stop();homeSettings?.dismiss();homeSettings=null
        tileEffects.forEach { it.stop() }
        generation++
        renderGeneration++
        visuals.stop()
        companion.stop()
        pairingDialog?.dismiss()
        pairingDialog = null
        tick?.let { handler.removeCallbacks(it) }
        tick = null
        super.onPause()
    }
    override fun onStop() {
        // Fully hidden: release the frozen backdrop and widget artwork.
        backdrop?.stop();music?.release();weather?.stop();if(::clock.isInitialized)clock.stop()
        // Home remains on the back stack during playback; release its image views.
        if (::body.isInitialized) body.removeAllViews()
        if (::root.isInitialized) root.removeAllViews()
        super.onStop()
    }
    override fun onDestroy() {
        window.decorView.viewTreeObserver.removeOnGlobalFocusChangeListener(homeFocusListener)
        navigationSound.stop()
        visuals.stop()
        companion.stop()
        generation++
        backdrop?.stop();music?.release();weather?.stop();if(::clock.isInitialized)clock.stop()
        super.onDestroy()
    }
    override fun dump(prefix: String, fd: java.io.FileDescriptor?, writer: java.io.PrintWriter, args: Array<out String>?) {
        super.dump(prefix, fd, writer, args)
        writer.println("${prefix}Clock: ${if(::clock.isInitialized)clock.diagnostics() else "not initialized"}")
        writer.println("${prefix}Weather: ${weather?.diagnostics()}")
        writer.println("${prefix}Music: ${music?.diagnostics()}")
        writer.println("${prefix}Navigation sound: ${navigationSound.diagnostics()}")
        writer.println("${prefix}Backdrop: ${backdrop?.diagnostics()}")
        writer.println("${prefix}HDMI menu: $inputRows")
        writer.println("${prefix}QuietTV: resumed=$resumed visualWorkerActive=${visuals.isActive} clockScheduled=${tick != null} renders=$renderCount viewChildren=${if (::root.isInitialized) root.childCount else 0} companionActive=${companion.server != null}")
    }
    override fun onSaveInstanceState(out: Bundle) {
        out.putString("selected", selectedPackage); super.onSaveInstanceState(out)
    }
    override fun dispatchKeyEvent(event:KeyEvent):Boolean {
        if(event.action==KeyEvent.ACTION_DOWN && event.keyCode in intArrayOf(KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN,KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT)) {
            navigationKeyPending=true
            // ViewRoot performs unhandled arrow-key focus search after Activity dispatch returns.
            handler.post {navigationKeyPending=false}
        }
        return super.dispatchKeyEvent(event)
    }
    override fun dispatchGenericMotionEvent(event:MotionEvent):Boolean {
        if(resumed && hasWindowFocus() && ::root.isInitialized && event.isFromSource(InputDevice.SOURCE_MOUSE)) {
            when(event.actionMasked) {
                MotionEvent.ACTION_HOVER_ENTER,MotionEvent.ACTION_HOVER_MOVE -> {
                    val item=hoverTarget(root,event.rawX.toInt(),event.rawY.toInt())
                    if(item!==hoveredItem) {hoveredItem=item;if(item!=null)navigationSound.play()}
                }
                MotionEvent.ACTION_HOVER_EXIT -> hoveredItem=null
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }
    override fun onWindowFocusChanged(hasFocus:Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(!hasFocus)hoveredItem=null
    }
    private fun hoverTarget(view:View,x:Int,y:Int):View? {
        val bounds=android.graphics.Rect()
        if(!view.isShown || !view.getGlobalVisibleRect(bounds) || !bounds.contains(x,y))return null
        if(view is ViewGroup)for(i in view.childCount-1 downTo 0)hoverTarget(view.getChildAt(i),x,y)?.let {return it}
        return view.takeIf {it.isFocusable && it.isEnabled && (it.isClickable || it is SeekBar)}
    }
    private fun useHomeSound(view:View) {
        view.isSoundEffectsEnabled=false
        if(view is ViewGroup)for(i in 0 until view.childCount)useHomeSound(view.getChildAt(i))
    }
    private fun loadApps() {
        val request = ++generation
        visuals.submit({
            packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)
                .filter { it.activityInfo.packageName != packageName }
                .distinctBy { it.activityInfo.packageName }
                .map { TvApp(it.activityInfo.packageName, it.loadLabel(packageManager).toString(), it) }
                .sortedBy { it.label.lowercase(Locale.getDefault()) }
        }) { result -> if (resumed && !isDestroyed && request == generation) { apps = result; render() } }
    }
    private fun shape(color: Int) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(9).toFloat()
    }
    private fun label(text: String, size: Float, color: Int = white) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color); includeFontPadding = false
    }
    private fun focusable(view: View, base: Int = 0x14FFFFFF, onFocus: (() -> Unit)? = null) {
        view.id = View.generateViewId(); view.isFocusable = true; view.isClickable = true
        view.background = shape(base)
        view.setOnFocusChangeListener { v, focused ->
            v.background = shape(if (focused) 0x30FFFFFF else base)
            v.elevation = 0f
            if (focused) onFocus?.invoke()
        }
    }
    private fun button(text: String, click: () -> Unit) = label(text, 14f).apply {
        gravity = Gravity.CENTER; setPadding(dp(17), dp(12), dp(17), dp(12)); focusable(this)
        setOnClickListener { click() }
    }
    private fun headerButton(title:String,icon:Int,input:HdmiMenu.Entry?=null,click:()->Unit)=FrameLayout(this).apply {
        id=View.generateViewId();isFocusable=true;isClickable=true;clipChildren=false;clipToPadding=false
        contentDescription=title+if(input==null)"" else if(input.connected)", connected" else ", disconnected"
        val pill=row().apply {gravity=Gravity.CENTER;setPadding(dp(if(input==null)0 else 16),0,dp(if(input==null)0 else 16),0)}
        addView(pill,FrameLayout.LayoutParams(-1,-1))
        val hint=if(input==null)label(title,8.5f).apply {
            gravity=Gravity.CENTER;isSingleLine=true;visibility=View.INVISIBLE
            importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
            addView(this,FrameLayout.LayoutParams(dp(80),dp(16)).apply {leftMargin=dp(-20);topMargin=dp(43)})
        } else null
        fun fill() {
            val active=hasFocus() || isHovered
            pill.background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                if(active)intArrayOf(0xDC545660.toInt(),0xDC3D3F49.toInt())
                else intArrayOf(0xC43A3C46.toInt(),0xC4282A33.toInt())).apply {cornerRadius=dp(24).toFloat()}
            hint?.visibility=if(active)View.VISIBLE else View.INVISIBLE
        }
        if(input==null)pill.addView(ImageView(context).apply {
            setImageResource(icon);setColorFilter(white);importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
        },LinearLayout.LayoutParams(dp(21),dp(21)))
        else {
            if(input.connected)addView(View(context).apply {
                background=GradientDrawable().apply {shape=GradientDrawable.OVAL;setColor(0xFF79D7A5.toInt());setStroke(dp(1),0xFF263139.toInt())}
                importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },FrameLayout.LayoutParams(dp(7),dp(7),Gravity.TOP or Gravity.END).apply {topMargin=dp(2);marginEnd=dp(2)})
            val labels=LinearLayout(context).apply {orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER}
            fun line(value:String,size:Float,color:Int)=label(value,size,color).apply {
                gravity=Gravity.CENTER;setFontVariationSettings("'wght' 500");isSingleLine=true;maxWidth=dp(100)
                ellipsize=android.text.TextUtils.TruncateAt.END;importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            if(input.connected) {
                val names=title.split(" · ",limit=2)
                labels.addView(line(names.first(),9f,0xFFC2C8D0.toInt()))
                labels.addView(line(names.getOrElse(1){"Connected"},11f,white),LinearLayout.LayoutParams(-2,-2).apply {topMargin=dp(2)})
            } else labels.addView(line(title,10.5f,white))
            pill.addView(labels)
        }
        fill();setOnFocusChangeListener {_,_->fill()}
        setOnHoverListener {_,event->
            when(event.actionMasked) {MotionEvent.ACTION_HOVER_ENTER->{isHovered=true;fill()};MotionEvent.ACTION_HOVER_EXIT->{isHovered=false;fill()}}
            false
        }
        setOnClickListener {click()}
    }
    private fun row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun addSpaced(parent: LinearLayout, view: View, width: Int = -2, height: Int = -2) {
        parent.addView(view, LinearLayout.LayoutParams(width, height).apply { marginEnd = dp(10) })
    }
    private fun render(focusPkg: String? = selectedPackage) {
        if (!resumed || isDestroyed) return
        val renderRequest = ++renderGeneration
        renderCount++
        hoveredItem=null
        tileEffects.forEach { it.stop() };tileEffects.clear()
        tick?.let { handler.removeCallbacks(it) }
        backdrop?.stop();music?.release();weather?.stop();if(::clock.isInitialized)clock.stop()
        root = FrameLayout(this).apply {setBackgroundColor(Color.rgb(10,14,21))}
        setContentView(root)
        backdrop=HomeBackdrop(this,prefs).also {root.addView(it,FrameLayout.LayoutParams(-1,-1));it.start()}
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false; isVerticalScrollBarEnabled=false }
        root.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; clipChildren=false; clipToPadding=false; setPadding(dp(42), dp(30), dp(42), dp(28)) }
        scroll.addView(body)
        val header = row().apply {clipChildren=false;clipToPadding=false}
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL;clipChildren=false;clipToPadding=false }
        val greeting=label("",10f,accent).apply {letterSpacing=.2f}
        brand.addView(greeting)
        val dateTime=label("",26f).apply { setFontVariationSettings("'wght' 500");setPadding(0,dp(6),0,0);isSingleLine=true;setAutoSizeTextTypeUniformWithConfiguration(20,26,1,android.util.TypedValue.COMPLEX_UNIT_SP) }
        brand.addView(dateTime)
        val actions = row().apply {setPadding(0,dp(14),0,0);clipChildren=false;clipToPadding=false}
        addSpaced(actions, headerButton("Appearance", R.drawable.huge_palette) { appearance() }, dp(40), dp(40))
        addSpaced(actions, headerButton("TV settings", R.drawable.huge_settings) { openSettings() }, dp(40), dp(40))
        inputRows=readInputs()
        inputRows.forEach { entry -> addSpaced(actions,headerButton(entry.label,0,entry) {openInput(entry)},-2,dp(40)) }
        fun spaceActions() {for(i in 0 until actions.childCount) {
            (actions.getChildAt(i).layoutParams as LinearLayout.LayoutParams).marginEnd=if(i==actions.childCount-1)0 else dp(7)
        }}
        spaceActions()
        brand.addView(actions)
        header.addView(brand, LinearLayout.LayoutParams(0, -2, 1f))
        music = MusicWidget(this).also {
            header.addView(it,LinearLayout.LayoutParams(dp(181),dp(110)).apply {marginEnd=dp(12)})
            it.start()
        }
        clock = HomeTimeWidget(this, accent)
        header.addView(clock, LinearLayout.LayoutParams(dp(110), dp(110)))
        clock.start()
        weather=WeatherWidget(this).also {header.addView(it,LinearLayout.LayoutParams(dp(110),dp(110)).apply {marginStart=dp(12)});it.start()}
        body.addView(header, LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(66) })
        tick = object : Runnable {
            override fun run() {
                if (!resumed || renderRequest != renderGeneration) return
                clock.refresh()
                val now=Date();val locale=Locale.getDefault()
                greeting.text=HomeGreeting.forHour(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)).trimEnd('.').uppercase(locale)
                weather?.refreshIfDue()
                dateTime.text=getString(R.string.home_date_time,java.text.SimpleDateFormat("h:mm a",locale).format(now).uppercase(locale),java.text.SimpleDateFormat("EEE, d MMM",locale).format(now))
                handler.postDelayed(this, 60000 - System.currentTimeMillis() % 60000)
            }
        }.also { it.run() }
        val order = strings("order")
        val sorted = apps.sortedBy { order.indexOf(it.pkg).let { index -> if (index < 0) Int.MAX_VALUE else index } }
        val visible = listOf(TvApp(connectTile, "Connect device", null), TvApp(screensaverTile, "MK-IV Screensaver", null)) + sorted
        val columns = prefs.getInt("columns", 6)
        val width = ((resources.displayMetrics.widthPixels / resources.displayMetrics.density - 84 - 10 * (columns - 1)) / columns).toInt()
        var preferred: View? = null
        var first: View? = null
        val gridRows = mutableListOf<List<View>>()
        visible.chunked(columns).forEach { chunk ->
            val line = row().apply { gravity = Gravity.TOP; clipChildren=false; clipToPadding=false; setPadding(0, 0, 0, dp(4)) }
            val cells = chunk.map { app ->
                val cell = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;clipChildren=false }
                val tile = FrameLayout(this).apply {
                    id=View.generateViewId();isFocusable=true;isClickable=true
                    background=shape(0xFF17222C.toInt());clipToOutline=true
                    contentDescription=app.label
                    setOnClickListener { if (app.pkg == connectTile) showConnect() else if (app.pkg == screensaverTile) openScreensaver() else launchApp(app) }
                    setOnLongClickListener { if (app.pkg == connectTile) showConnect() else if (app.pkg == screensaverTile) openScreensaver() else editApp(app); true }
                    setOnKeyListener { _, key, event ->
                        if (key == KeyEvent.KEYCODE_MENU && event.action == KeyEvent.ACTION_UP) {
                            if (app.pkg == connectTile) showConnect() else if (app.pkg == screensaverTile) openScreensaver() else editApp(app)
                            true
                        } else false
                    }
                }
                val image=ImageView(this).apply {
                    scaleType=ImageView.ScaleType.FIT_XY
                    importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    alpha=.86f
                }
                val glow=View(this).apply { setBackgroundColor(Color.WHITE);alpha=0f;importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO }
                tile.addView(image,FrameLayout.LayoutParams(-1,-1))
                tile.addView(glow,FrameLayout.LayoutParams(-1,-1))
                val caption=label(app.label,11f).apply {
                    setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,textSize-3f);setFontVariationSettings("'wght' 500")
                    gravity=Gravity.TOP or Gravity.CENTER_HORIZONTAL;maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END
                    visibility=View.INVISIBLE;importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO
                    setPadding(0,dp(3),0,0)
                }
                tileEffects.add(TileFocusEffect(tile,image,glow,caption) {selectedPackage=app.pkg})
                val artwork=when(app.pkg) {
                    connectTile->R.drawable.connect
                    screensaverTile->R.drawable.console
                    "com.netflix.ninja"->R.drawable.netflix_banner
                    "com.oneplus.tv.screensaver"->R.drawable.tile_pictorial
                    "com.oneplus.tv.userguide"->R.drawable.tile_manual
                    "com.heytap.tv.livetv"->R.drawable.tile_live_tv
                    "com.heytap.tv.btspeaker"->R.drawable.tile_bluetooth
                    else->null
                }
                if(artwork!=null)image.setImageResource(artwork)
                else visuals.submit({
                    try {
                        if(app.pkg=="com.oneplus.tv.launcher")app.info?.loadIcon(packageManager)
                        else app.info?.activityInfo?.loadBanner(packageManager) ?: app.info?.loadIcon(packageManager)
                    } catch (_: Exception) { null }
                }) { drawable ->
                    if(renderRequest==renderGeneration && tile.isAttachedToWindow) {
                        if(app.pkg=="com.oneplus.tv.launcher") {
                            // Official icon includes its own white margin; preserve it at native aspect.
                            image.scaleType=ImageView.ScaleType.FIT_CENTER
                            image.setBackgroundColor(Color.WHITE)
                        } else {
                            image.scaleType=if(drawable!=null && drawable.intrinsicWidth>drawable.intrinsicHeight*1.3f)ImageView.ScaleType.FIT_XY else ImageView.ScaleType.FIT_CENTER
                            if(image.scaleType==ImageView.ScaleType.FIT_CENTER)image.setPadding(dp(16),dp(10),dp(16),dp(10))
                        }
                        image.setImageDrawable(drawable)
                    }
                }
                cell.addView(tile,LinearLayout.LayoutParams(-1,dp(width*9/16)))
                cell.addView(caption,LinearLayout.LayoutParams(-1,dp(24)))
                addSpaced(line,cell,dp(width),-2)
                if (first == null) first = tile
                if (app.pkg == focusPkg) preferred = tile
                tile
            }
            gridRows.add(cells); body.addView(line)
        }
        fun linkHeader() {
        gridRows.forEachIndexed { r, cells -> cells.forEachIndexed { c, cell ->
            cell.nextFocusLeftId = cells.getOrNull(c - 1)?.id ?: cell.id
            cell.nextFocusRightId = cells.getOrNull(c + 1)?.id ?: cell.id
            cell.nextFocusUpId = if (r == 0) actions.getChildAt(c.coerceAtMost(actions.childCount-1)).id else gridRows[r - 1][minOf(c, gridRows[r - 1].lastIndex)].id
            cell.nextFocusDownId = if (r == gridRows.lastIndex) cell.id else gridRows[r + 1][minOf(c, gridRows[r + 1].lastIndex)].id
        } }
        // Route directly between the header pills and app grid.
        for (i in 0 until actions.childCount) {
            val action = actions.getChildAt(i)
            action.nextFocusDownId = gridRows.firstOrNull()?.let {it[minOf(i,it.lastIndex)].id} ?: action.id
            action.nextFocusUpId = action.id
            action.nextFocusLeftId = actions.getChildAt((i-1).coerceAtLeast(0)).id
            action.nextFocusRightId = if(i==actions.childCount-1) music!!.firstControlId else actions.getChildAt(i+1).id
        }
        music?.linkNavigation(actions.getChildAt(actions.childCount-1).id,gridRows.first().getOrElse(3){gridRows.first().last()}.id,weather!!.id)
            weather?.nextFocusLeftId=music!!.id
            weather?.nextFocusDownId=gridRows.first().last().id
            weather?.nextFocusUpId=weather!!.id
            weather?.nextFocusRightId=weather!!.id
        }
        linkHeader()
        refreshInputPills={
            val updated=readInputs()
            if(resumed && updated!=inputRows) {
                val focused=currentFocus
                val oldIndex=(0 until actions.childCount).firstOrNull {actions.getChildAt(it)===focused}
                val target=oldIndex?.takeIf {it>=2}?.let {inputRows.getOrNull(it-2)?.targetId}
                inputRows=updated
                if(actions.childCount>2)actions.removeViews(2,actions.childCount-2)
                updated.forEach { entry -> addSpaced(actions,headerButton(entry.label,0,entry) {openInput(entry)},-2,dp(40)) }
                spaceActions();linkHeader();useHomeSound(actions)
                if(oldIndex!=null && oldIndex>=2)actions.getChildAt(updated.indexOfFirst {it.targetId==target}.takeIf {it>=0}?.plus(2) ?: 1).requestFocus()
            }
        }
        useHomeSound(window.decorView)
        (preferred ?: first ?: actions.getChildAt(0))?.requestFocus()
    }
    private fun openScreensaver() = safeStart(Intent(this, dev.rahul.quiettv.screensaver.ConsoleSettingsActivity::class.java))
    private fun showConnect() {
        val server = try { companion.start() } catch (e: Exception) {
            toast(e.message ?: "Couldn't start connection. Check the TV's network."); return
        }
        val panel = row().apply { setPadding(dp(24), dp(8), dp(24), dp(12)); gravity = Gravity.CENTER_VERTICAL }
        val bits = com.google.zxing.qrcode.QRCodeWriter().encode(server.url, com.google.zxing.BarcodeFormat.QR_CODE, 480, 480)
        val pixels = IntArray(480 * 480) { i -> if (bits[i % 480, i / 480]) Color.BLACK else Color.WHITE }
        val bitmap = android.graphics.Bitmap.createBitmap(pixels, 480, 480, android.graphics.Bitmap.Config.ARGB_8888)
        panel.addView(ImageView(this).apply { setImageBitmap(bitmap); contentDescription = "Scan to open Quiet TV on your phone" },
            LinearLayout.LayoutParams(dp(220), dp(220)).apply { marginEnd = dp(26) })
        val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        text.addView(label("Your TV, in your hands.", 24f, accent))
        text.addView(label("1. Join the same Wi-Fi as this TV.\n2. Scan the QR code, or open this address:", 15f).apply { setPadding(0, dp(14), 0, dp(8)) })
        text.addView(label(server.url, 17f, accent))
        text.addView(label("3. Enter this pairing code:", 15f).apply { setPadding(0, dp(16), 0, dp(6)) })
        text.addView(label(server.code.chunked(3).joinToString(" "), 32f).apply {
            letterSpacing = .16f; contentDescription = "Pairing code ${server.code}"
        })
        text.addView(label("Edit app order, colors and backgrounds.\nKeep Quiet TV open while editing. Pair within 10 minutes.\n${server.pairedCount} paired device(s) · Local Wi-Fi only", 12f, muted).apply { setPadding(0, dp(14), 0, 0) })
        panel.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
        pairingDialog = AlertDialog.Builder(this).setCustomTitle(DialogUi.compactTitle(this,"Connect device")).setView(panel)
            .setPositiveButton("Keep editing on TV", null)
            .setNeutralButton("New pairing code") { _, _ -> companion.stop(); showConnect() }
            .setNegativeButton("Disconnect all") { _, _ -> companion.stop(); toast("All companion devices disconnected.") }
            .create().also { dialog ->
                dialog.show();DialogUi.style(dialog,800)
                dialog.window?.setLayout(dp(800).coerceAtMost(resources.displayMetrics.widthPixels - dp(60)), -2)
            }
    }
    private fun launchApp(app: TvApp) {
        val intent = packageManager.getLeanbackLaunchIntentForPackage(app.pkg)
        if (intent == null) toast("This app no longer has a TV launch activity. Refreshing…").also { loadApps() }
        else safeStart(intent)
    }
    private fun safeStart(intent: Intent) {
        try { startActivity(intent) }
        catch (_: ActivityNotFoundException) { toast("This action is not available on this TV firmware.") }
        catch (_: SecurityException) { toast("This TV firmware restricts access to this action.") }
    }
    private fun choose(title:String,choices:List<String>,selected:Int=-1,icon:Int=R.drawable.huge_settings,action:(Int)->Unit) {
        DialogUi.choose(this,title,choices,selected,icon,action)
    }
    private fun editApp(app: TvApp) {
        choose(app.label, listOf("Move earlier", "Move later", "App information")) { index ->
            when (index) {
                0, 1 -> {
                    val saved = strings("order")
                    val full = apps.sortedBy { saved.indexOf(it.pkg).let { n -> if (n < 0) Int.MAX_VALUE else n } }.map { it.pkg }.toMutableList()
                    val at = full.indexOf(app.pkg); val to = at + if (index == 0) -1 else 1
                    if (to in full.indices) { Collections.swap(full, at, to); save("order", full); render(app.pkg) }
                    else toast("Already at the ${if (index == 0) "start" else "end"}.")
                }
                2 -> safeStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pkg}")))
            }
        }
    }
    private fun appearance() {
        val panel=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;setPadding(dp(16),dp(12),dp(16),dp(16)) }
        val navigation=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(0,0,dp(16),0) }
        val content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(10),dp(14),dp(12));background=shape(0x08FFFFFF) }
        panel.addView(navigation,LinearLayout.LayoutParams(dp(195),-1))
        panel.addView(content,LinearLayout.LayoutParams(0,-1,1f))
        val dialog=AlertDialog.Builder(this).setTitle("Home settings").setView(panel).setNegativeButton("Close",null).create()
        dialog.setCanceledOnTouchOutside(true)
        homeSettings=dialog
        dialog.setOnDismissListener { if(homeSettings===dialog)homeSettings=null }
        val titles=listOf("Background","Colors","Layout","Sound")
        val icons=listOf(R.drawable.huge_preview,R.drawable.huge_palette,R.drawable.huge_density,R.drawable.huge_music)
        val menus=mutableListOf<TextView>()
        var selected=-1
        var firstOption:View?=null
        fun decorate(view:TextView,resource:Int) {
            view.gravity=Gravity.CENTER_VERTICAL
            val icon=getDrawable(resource)!!.mutate().apply {setTint(accent);setBounds(0,0,dp(21),dp(21))}
            view.compoundDrawablePadding=dp(12);view.setCompoundDrawablesRelative(icon,null,null,null)
        }
        fun select(index:Int) {
            if(selected==index)return
            selected=index;content.removeAllViews();firstOption=null
            menus.forEachIndexed { n,v->v.background=shape(if(v.hasFocus())0x30FFFFFF else if(n==index)0x1CFFFFFF else 0x08FFFFFF);v.isSelected=n==index }
            content.addView(label(titles[index],22f).apply {setPadding(0,0,0,dp(if(index==0)6 else 18))})
            if(index==0) {
                val gallery=WallpaperGallery(this,prefs,menus[index].id,dialog.getButton(AlertDialog.BUTTON_NEGATIVE).id) {backdrop?.start()}
                content.addView(gallery,LinearLayout.LayoutParams(-1,0,1f))
                firstOption=gallery.firstOption
                menus.forEach {it.nextFocusRightId=firstOption!!.id}
                return
            }
            val options=when(index) {
                1->listOf(Triple("Accent color",R.drawable.huge_palette,0))
                2->listOf(Triple("Tile size",R.drawable.huge_density,2))
                3->listOf(Triple("Navigation sounds",R.drawable.huge_music,3),Triple("Sound volume",R.drawable.huge_music,4))
                else->emptyList()
            }
            val rows=options.map { (_,icon,action)->
                lateinit var option:TextView
                option=button(appearanceValue(action)) {appearanceAction(action){option.text=appearanceValue(action)}}.apply {
                    decorate(this,icon);nextFocusLeftId=menus[index].id
                    setOnKeyListener { _,key,event->
                        if(key==KeyEvent.KEYCODE_DPAD_LEFT) {if(event.action==KeyEvent.ACTION_DOWN)menus[index].requestFocus();true} else false
                    }
                    content.addView(this,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(8)})
                }
                option
            }
            rows.forEachIndexed { n,v->
                v.nextFocusUpId=rows.getOrElse(n-1){v}.id
                v.nextFocusDownId=rows.getOrElse(n+1){dialog.getButton(AlertDialog.BUTTON_NEGATIVE)}.id
            }
            firstOption=rows.firstOrNull()
            menus.forEach {it.nextFocusRightId=firstOption?.id ?: it.id}
        }
        titles.forEachIndexed { index,title->
            val menu=button(title) {select(index);firstOption?.requestFocus()}
            decorate(menu,icons[index]);menus.add(menu)
            menu.setOnFocusChangeListener { v,focused->
                if(focused)select(index)
                v.background=shape(if(focused)0x30FFFFFF else if(index==selected)0x1CFFFFFF else 0x08FFFFFF)
            }
            menu.setOnKeyListener { _,key,event->
                if(key==KeyEvent.KEYCODE_DPAD_RIGHT) {if(event.action==KeyEvent.ACTION_DOWN)firstOption?.requestFocus();true} else false
            }
            navigation.addView(menu,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(8)})
        }
        dialog.show();DialogUi.style(dialog,820)
        panel.layoutParams=panel.layoutParams.apply {height=dp(380)}
        dialog.window?.apply {setBackgroundDrawable(shape(Color.rgb(15,21,30)));setLayout(dp(820),dp(520))}
        menus.forEachIndexed { index,v->
            v.nextFocusUpId=menus.getOrElse(index-1){v}.id
            v.nextFocusDownId=menus.getOrElse(index+1){dialog.getButton(AlertDialog.BUTTON_NEGATIVE)}.id
        }
        select(0);menus[0].requestFocus()
    }
    private fun appearanceValue(index:Int):String=when(index) {
        0->"Accent color  ·  "+(accents.firstOrNull {it.second.equals(prefs.getString("accent","#AADBCB"),true)}?.first ?: prefs.getString("accent","#AADBCB"))
        2->"Tile size  ·  ${listOf("Large","Medium","Small")[(prefs.getInt("columns",6)-4).coerceIn(0,2)]} · ${prefs.getInt("columns",6)} columns"
        3->"Navigation sounds  ·  ${if(navigationSound.enabled)"On" else "Off"}"
        4->"Sound volume  ·  ${navigationSound.volume}%"
        else->""
    }
    private fun appearanceAction(index:Int,changed:()->Unit={}) {
            when (index) {
                0 -> choose("Accent color", accents.map { it.first } + "Custom hex color",accents.indexOfFirst {it.second.equals(prefs.getString("accent","#AADBCB"),true)},R.drawable.huge_palette) { n ->
                    if (n == accents.size) customColor("accent",changed) else { prefs.edit().putString("accent", accents[n].second).apply(); changed();render() }
                }
                2 -> choose("Tile size", listOf("Large · 4 columns", "Medium · 5 columns", "Small · 6 columns"),prefs.getInt("columns",6)-4,R.drawable.huge_density) { n -> prefs.edit().putInt("columns", n + 4).apply(); changed();render() }
                3 -> {navigationSound.enabled=!navigationSound.enabled;changed()}
                4 -> choose("Sound volume",(0..100 step 10).map {if(it==0)"0% · Muted" else "$it%"},navigationSound.volume/10,R.drawable.huge_music) { n ->
                    navigationSound.volume=n*10;changed()
                    // A single sample makes the selected level audible; Off and 0% stay silent.
                    navigationSound.play()
                }

            }
    }
    private fun customColor(key: String,changed:()->Unit={}) {
        val field = EditText(this).apply { setSingleLine(); hint = "#RRGGBB"; setText(prefs.getString(key, if (key == "accent") "#AADBCB" else "#0C1018")) }
        val fieldPanel=LinearLayout(this).apply {setPadding(dp(24),dp(8),dp(24),dp(12));addView(field,LinearLayout.LayoutParams(-1,-2))}
        val dialog = AlertDialog.Builder(this).setTitle("Custom color").setView(fieldPanel).setPositiveButton("Save", null).setNegativeButton("Cancel", null).create()
        dialog.setOnShowListener { DialogUi.style(dialog);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val value = field.text.toString().trim()
            if (!Regex("#[0-9a-fA-F]{6}").matches(value)) { field.error = "Use #RRGGBB"; return@setOnClickListener }
            prefs.edit().putString(key, value).apply { if (key == "background") putBoolean("photo", false) }.apply()
            dialog.dismiss(); changed();render()
        } }; dialog.show()
    }
    private fun openSettings() { safeStart(Intent(Settings.ACTION_SETTINGS)) }
    private fun readInputs():List<HdmiMenu.Entry> = try {
        HdmiMenu.entries(inputManager.tvInputList.filter {it.type==TvInputInfo.TYPE_HDMI && !it.isHidden(this)}.map {
            HdmiMenu.Input(it.id,it.parentId,(it.loadCustomLabel(this) ?: it.loadLabel(this)).toString(),inputManager.getInputState(it.id)==TvInputManager.INPUT_STATE_CONNECTED)
        })
    } catch (_:Exception) {emptyList()}
    private fun openInput(entry:HdmiMenu.Entry) {
        val uri=TvContract.buildChannelUriForPassthroughInput(entry.targetId)
        val intent=Intent(Intent.ACTION_VIEW,uri)
        if(intent.resolveActivity(packageManager)==null)intent.setDataAndType(uri,TvContract.Channels.CONTENT_ITEM_TYPE)
        safeStart(intent)
    }
}
