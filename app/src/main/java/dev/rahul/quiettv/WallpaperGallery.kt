package dev.rahul.quiettv

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.*

/** Remote-friendly thumbnails with a persistent selection badge and separate focus fill. */
class WallpaperGallery(context:Context,private val prefs:SharedPreferences,private val leftId:Int,private val closeId:Int,private val changed:()->Unit):ScrollView(context) {
    private val worker=ForegroundTasks {post {it()}}
    private val thumbnails=mutableListOf<Pair<ImageView,String>>()
    private val entries=listOf(WallpaperCatalog.Picture("shuffle","Shuffle all · 10s"))+WallpaperCatalog.pictures
    private val cards=mutableListOf<View>()
    private val badges=mutableListOf<ImageView>()
    val firstOption:View get()=cards.getOrElse(selectedIndex()){cards.first()}
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun shape(color:Int,radius:Int=8)=GradientDrawable().apply {setColor(color);cornerRadius=dp(radius).toFloat()}
    private fun selectedIndex()=if(prefs.getBoolean("photo",false))-1 else if(prefs.getBoolean("wallpaperShuffle",false))0 else entries.indexOfFirst {it.id==prefs.getString("wallpaperId",WallpaperCatalog.defaultId)}
    init {
        isVerticalScrollBarEnabled=false;clipToPadding=false
        val grid=LinearLayout(context).apply {orientation=LinearLayout.VERTICAL;setPadding(dp(2),dp(2),dp(2),dp(8))}
        addView(grid)
        entries.chunked(3).forEachIndexed {rowIndex,rowEntries->
            val row=LinearLayout(context).apply {orientation=LinearLayout.HORIZONTAL}
            grid.addView(row,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(8)})
            rowEntries.forEachIndexed {col,item->
                val index=rowIndex*3+col
                val card=LinearLayout(context).apply {
                    orientation=LinearLayout.VERTICAL;isFocusable=true;isClickable=true;id=View.generateViewId();setPadding(dp(4),dp(4),dp(4),dp(10));background=shape(0x08FFFFFF)
                }
                val picture=FrameLayout(context).apply {background=shape(0xFF17222C.toInt(),6);clipToOutline=true;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS}
                fun thumb(id:String)=ImageView(context).apply {scaleType=ImageView.ScaleType.CENTER_CROP;thumbnails.add(this to id)}
                if(item.id=="shuffle") {
                    val collage=LinearLayout(context).apply {orientation=LinearLayout.VERTICAL}
                    repeat(2) {r->val strip=LinearLayout(context);repeat(2){c->strip.addView(thumb(WallpaperCatalog.pictures[r*2+c].id),LinearLayout.LayoutParams(0,-1,1f))};collage.addView(strip,LinearLayout.LayoutParams(-1,0,1f))}
                    picture.addView(collage,FrameLayout.LayoutParams(-1,-1))
                    picture.addView(View(context).apply {setBackgroundColor(0x40000000)},FrameLayout.LayoutParams(-1,-1))
                    picture.addView(ImageView(context).apply {setImageResource(R.drawable.huge_shuffle);setColorFilter(Color.WHITE)},FrameLayout.LayoutParams(dp(34),dp(34),Gravity.CENTER))
                } else picture.addView(thumb(item.id),FrameLayout.LayoutParams(-1,-1))
                val badge=ImageView(context).apply {setImageResource(R.drawable.selection_check);scaleType=ImageView.ScaleType.FIT_CENTER;importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO}
                picture.addView(badge,FrameLayout.LayoutParams(dp(22),dp(22),Gravity.TOP or Gravity.END).apply {setMargins(dp(4),dp(4),dp(4),dp(4))})
                badges.add(badge);cards.add(card)
                card.addView(picture,LinearLayout.LayoutParams(-1,dp(76)))
                card.addView(TextView(context).apply {text=item.title;textSize=12f;setTextColor(Color.rgb(238,243,247));gravity=Gravity.CENTER;maxLines=1;includeFontPadding=false;setPadding(0,dp(2),0,0);importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO},LinearLayout.LayoutParams(-1,dp(22)))
                card.setOnFocusChangeListener {v,focused->v.background=shape(if(focused)0x30FFFFFF else 0x08FFFFFF);picture.alpha=if(focused)1f else .9f}
                card.setOnClickListener {
                    prefs.edit().putBoolean("photo",false).putBoolean("wallpaperShuffle",item.id=="shuffle").apply {if(item.id!="shuffle")putString("wallpaperId",item.id)}.apply()
                    refreshSelection();changed()
                }
                card.setOnKeyListener {_,key,event->if(key==KeyEvent.KEYCODE_DPAD_LEFT && col==0){if(event.action==KeyEvent.ACTION_DOWN)rootView.findViewById<View>(leftId)?.requestFocus();true}else false}
                row.addView(card,LinearLayout.LayoutParams(0,-2,1f).apply {if(col<2)marginEnd=dp(8)})
            }
            repeat(3-rowEntries.size){row.addView(Space(context),LinearLayout.LayoutParams(0,1,1f).apply {marginStart=dp(8)})}
        }
        cards.forEachIndexed {i,v->
            v.nextFocusLeftId=if(i%3==0)leftId else cards[i-1].id
            v.nextFocusRightId=if(i%3==2 || i==cards.lastIndex)v.id else cards[i+1].id
            v.nextFocusUpId=if(i<3)v.id else cards[i-3].id
            v.nextFocusDownId=cards.getOrNull(i+3)?.id ?: if(i/3 < (cards.size-1)/3)cards.last().id else closeId
        }
        refreshSelection()
    }
    private fun refreshSelection() {cards.forEachIndexed {i,v->val selected=i==selectedIndex();v.isSelected=selected;v.contentDescription=entries[i].title+if(selected)", selected" else "";badges[i].visibility=if(selected)View.VISIBLE else View.GONE}}
    override fun onAttachedToWindow() {
        super.onAttachedToWindow();worker.start()
        thumbnails.forEach {(image,id)->worker.submit({try{context.assets.open("wallpapers/thumbs/$id.jpg").use {BitmapFactory.decodeStream(it)}}catch(_:Exception){null}}){image.setImageBitmap(it)}}
    }
    override fun onDetachedFromWindow() {worker.stop();thumbnails.forEach {it.first.setImageDrawable(null)};super.onDetachedFromWindow()}
}
