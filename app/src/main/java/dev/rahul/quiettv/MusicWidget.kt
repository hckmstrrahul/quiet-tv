package dev.rahul.quiettv

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.*

/** A small, native transport card. All observation/ticking stops when Home pauses. */
class MusicWidget(context:Context):LinearLayout(context) {
    private val handler=Handler(Looper.getMainLooper())
    private var running=false
    private var ticking=false
    private var controller:MediaController?=null
    private var access=false
    private var duration=0L
    private var dragging=false
    private var leftTarget=View.NO_ID
    private var downTarget=View.NO_ID
    private var rightTarget=View.NO_ID
    private val ink=0xFFF2F3F5.toInt()
    private val muted=0xFFB8B9BD.toInt()
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun text(size:Float)=TextView(context).apply {textSize=size;setTextColor(ink);includeFontPadding=false;isSingleLine=true;ellipsize=TextUtils.TruncateAt.END}
    private val cover=ImageView(context).apply {scaleType=ImageView.ScaleType.CENTER_CROP;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO;background=rounded(0xFF33363D.toInt(),7);clipToOutline=true}
    private val title=text(12f).apply {setFontVariationSettings("'wght' 650")}
    private val subtitle=text(9f).apply {setTextColor(muted)}
    private val elapsed=text(7.5f).apply {gravity=Gravity.CENTER_VERTICAL}
    private val total=text(7.5f).apply {gravity=Gravity.CENTER_VERTICAL or Gravity.END}
    private val progress=SeekBar(context).apply {
        id=generateViewId();max=1000;minimumHeight=0;setPadding(dp(5),0,dp(5),0)
        progressTintList=ColorStateList.valueOf(0xFFD8DADC.toInt());progressBackgroundTintList=ColorStateList.valueOf(0xFF666466.toInt())
        thumbTintList=ColorStateList.valueOf(Color.WHITE);splitTrack=false
        contentDescription="Playback position"
        setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(bar:SeekBar) {dragging=true}
            override fun onStopTrackingTouch(bar:SeekBar) {dragging=false;seek(bar.progress)}
            override fun onProgressChanged(bar:SeekBar,value:Int,fromUser:Boolean) {
                // D-pad changes have no start/stop tracking callbacks.
                if(fromUser && !dragging)seek(value)
            }
        })
    }
    private val previous=control("Previous",R.drawable.huge_previous) {send {skipToPrevious()} }
    private val play=control("Play",R.drawable.huge_play,true) {toggle()}
    private val next=control("Next",R.drawable.huge_next) {send {skipToNext()} }
    private val tracker=PlaybackTracker(context) {session,granted->controller=session;access=granted;refresh()}
    private val tick=object:Runnable {override fun run() {if(running){updateProgress();handler.postDelayed(this,1000)}}}
    val firstControlId get()=id

    init {
        id=generateViewId();orientation=VERTICAL;setPadding(dp(10),dp(9),dp(10),dp(8))
        isFocusable=true;isClickable=true;clipToOutline=true
        paintCard(false)
        setOnFocusChangeListener {_,focused->paintCard(focused)}
        setOnClickListener {if(!access)enableAccess() else if(controller!=null)toggle()}
        val meta=LinearLayout(context).apply {orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        meta.addView(cover,LayoutParams(dp(29),dp(29)).apply {marginEnd=dp(8)})
        val labels=LinearLayout(context).apply {orientation=VERTICAL;addView(title,LayoutParams(-1,-2));addView(subtitle,LayoutParams(-1,-2).apply {topMargin=dp(3)})}
        meta.addView(labels,LayoutParams(0,-2,1f));addView(meta,LayoutParams(-1,dp(34)))
        val timeline=LinearLayout(context).apply {orientation=HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        timeline.addView(elapsed,LayoutParams(dp(25),-1));timeline.addView(progress,LayoutParams(0,dp(22),1f));timeline.addView(total,LayoutParams(dp(25),-1))
        addView(timeline,LayoutParams(-1,dp(22)).apply {topMargin=dp(3)})
        val controls=LinearLayout(context).apply {orientation=HORIZONTAL;gravity=Gravity.CENTER}
        controls.addView(previous,LayoutParams(dp(30),dp(28)))
        controls.addView(play,LayoutParams(dp(31),dp(31)).apply {marginStart=dp(12);marginEnd=dp(12)})
        controls.addView(next,LayoutParams(dp(30),dp(28)))
        addView(controls,LayoutParams(-1,dp(32)))
        refresh()
    }
    private fun rounded(color:Int,radius:Int)=GradientDrawable().apply {setColor(color);cornerRadius=dp(radius).toFloat()}
    private fun paintCard(focused:Boolean) {
        background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            if(focused)intArrayOf(0xE3585758.toInt(),0xE31C1D20.toInt()) else intArrayOf(0xD0444345.toInt(),0xD00C0D10.toInt())).apply {
            cornerRadius=dp(12).toFloat();setStroke(dp(1),0x205D6064)
        }
    }
    private fun control(label:String,icon:Int,primary:Boolean=false,action:()->Unit)=ImageView(context).apply {
        id=generateViewId();isFocusable=true;isClickable=true;contentDescription=label
        setImageResource(icon);setColorFilter(if(primary)Color.BLACK else ink)
        setPadding(dp(if(primary)8 else 5),dp(if(primary)8 else 5),dp(if(primary)8 else 5),dp(if(primary)8 else 5))
        background=rounded(if(primary)Color.WHITE else Color.TRANSPARENT,30)
        setOnFocusChangeListener {v,focused->v.background=rounded(if(primary){if(focused)0xFFD3E8E1.toInt() else Color.WHITE}else if(focused)0x40FFFFFF else Color.TRANSPARENT,30)}
        setOnClickListener {action()}
    }
    fun linkNavigation(left:Int,down:Int,right:Int=View.NO_ID) {leftTarget=left;downTarget=down;rightTarget=right;navigation()}
    private fun navigation() {
        nextFocusLeftId=leftTarget;nextFocusUpId=id;nextFocusDownId=downTarget
        val controls=listOf(previous,play,next).filter {it.isEnabled}
        nextFocusRightId=controls.firstOrNull()?.id ?: rightTarget.takeIf {it!=View.NO_ID} ?: id
        controls.forEachIndexed {i,v->
            v.nextFocusLeftId=controls.getOrNull(i-1)?.id ?: id
            v.nextFocusRightId=controls.getOrNull(i+1)?.id ?: rightTarget.takeIf {it!=View.NO_ID} ?: v.id
            v.nextFocusUpId=if(progress.isEnabled)progress.id else id
            v.nextFocusDownId=downTarget
        }
        progress.nextFocusUpId=id;progress.nextFocusDownId=if(play.isEnabled)play.id else downTarget
    }
    fun start() {running=true;tracker.start()}
    fun stop() {running=false;ticking=false;handler.removeCallbacksAndMessages(null);tracker.stop();controller=null}
    fun release() {stop();cover.setImageDrawable(null)}
    private fun enabled(view:View,value:Boolean) {
        val hadFocus=view.hasFocus();view.isEnabled=value;view.isFocusable=value;view.alpha=if(value)1f else .3f
        if(hadFocus && !value)requestFocus()
    }
    private fun refresh() {
        ticking=false;handler.removeCallbacks(tick)
        val session=controller;val state=session?.playbackState;val metadata=session?.metadata
        val active=state?.state in listOf(PlaybackState.STATE_PLAYING,PlaybackState.STATE_BUFFERING,PlaybackState.STATE_CONNECTING,PlaybackState.STATE_FAST_FORWARDING,PlaybackState.STATE_REWINDING)
        val app=try {session?.packageName?.let {context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(it,0)).toString()}}catch(_:Exception){session?.packageName}
        title.text=if(!access)"Connect playback" else if(session==null)"Nothing playing" else metadata?.description?.title?.takeIf {it.isNotBlank()} ?: app ?: "Now playing"
        val artist=metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST)?.takeIf {it.isNotBlank()} ?: metadata?.description?.subtitle?.takeIf {it.isNotBlank()}
        subtitle.text=if(!access)"Enable media access" else if(session==null)"Play or cast on this TV" else listOfNotNull(artist,app?.takeIf {it!=artist?.toString()}).joinToString(" · ")
        val art=metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART) ?: metadata?.description?.iconBitmap
        if(art!=null){cover.clearColorFilter();cover.setPadding(0,0,0,0);cover.setImageBitmap(art)}
        else {cover.setImageResource(R.drawable.huge_music);cover.setColorFilter(muted);cover.setPadding(dp(6),dp(6),dp(6),dp(6))}
        duration=metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0) ?: 0L
        val actions=state?.actions ?: 0L
        fun supports(action:Long)=actions and action != 0L
        enabled(previous,supports(PlaybackState.ACTION_SKIP_TO_PREVIOUS))
        enabled(next,supports(PlaybackState.ACTION_SKIP_TO_NEXT))
        enabled(play,supports(if(active)PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY) || supports(PlaybackState.ACTION_PLAY_PAUSE))
        play.setImageResource(if(active)R.drawable.huge_pause else R.drawable.huge_play);play.contentDescription=if(active)"Pause" else "Play"
        enabled(progress,duration>0 && supports(PlaybackState.ACTION_SEEK_TO))
        contentDescription=if(!access)"Enable media access for the music widget" else "${title.text}. ${subtitle.text}. ${if(session==null)"" else if(active)"Playing" else "Paused"}"
        navigation();updateProgress()
        if(running && state?.state==PlaybackState.STATE_PLAYING){ticking=true;handler.postDelayed(tick,1000)}
    }
    private fun updateProgress() {
        val state=controller?.playbackState
        val position=PlaybackMath.position(state?.position ?: 0,state?.lastPositionUpdateTime ?: 0,SystemClock.elapsedRealtime(),state?.playbackSpeed ?: 0f,state?.state==PlaybackState.STATE_PLAYING,duration)
        elapsed.text=if(controller==null)"--" else PlaybackMath.time(position)
        total.text=if(duration>0)PlaybackMath.time(duration) else "--"
        if(!dragging)progress.progress=if(duration>0)((position.toDouble()/duration)*1000).toInt().coerceIn(0,1000) else 0
    }
    private fun seek(value:Int) {if(duration>0 && progress.isEnabled)send {seekTo(duration*value/1000)}}
    private fun toggle() {
        if(!play.isEnabled)return
        val playing=controller?.playbackState?.state in listOf(PlaybackState.STATE_PLAYING,PlaybackState.STATE_BUFFERING,PlaybackState.STATE_CONNECTING,PlaybackState.STATE_FAST_FORWARDING,PlaybackState.STATE_REWINDING)
        send {if(playing)pause() else play()}
    }
    private fun send(action:MediaController.TransportControls.()->Unit) {
        if(!running)return
        try {controller?.transportControls?.action()}catch(_:RuntimeException){Toast.makeText(context,"Playback app is no longer available",Toast.LENGTH_SHORT).show()}
    }
    private fun enableAccess() {
        try {context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))}
        catch(_:RuntimeException){Toast.makeText(context,"Enable Quiet TV media access from the Mac setup helper",Toast.LENGTH_LONG).show()}
    }
    fun diagnostics()="${tracker.diagnostics()} tick=$ticking package=${controller?.packageName} state=${controller?.playbackState?.state}"
}
