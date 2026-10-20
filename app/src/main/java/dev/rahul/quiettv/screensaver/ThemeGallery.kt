package dev.rahul.quiettv.screensaver

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import dev.rahul.quiettv.R

/** Static samples from the real renderer. No frame loop, bitmap cache or network work. */
@android.annotation.SuppressLint("ViewConstructor") // Built programmatically with selection and navigation callbacks.
class ThemeGallery(context:Context,selected:Int,closeId:Int,choose:(Int)->Unit):ScrollView(context) {
    private val cards=mutableListOf<View>()
    val selectedCard:View get()=cards[selectedIndex]
    private val selectedIndex=selected.coerceIn(ConsoleCatalog.palettes.indices)
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun shape(color:Int,radius:Int=8)=GradientDrawable().apply {setColor(color);cornerRadius=dp(radius).toFloat()}
    init {
        isVerticalScrollBarEnabled=false
        val grid=LinearLayout(context).apply {orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(4),dp(20),dp(8))}
        addView(grid)
        ConsoleCatalog.palettes.chunked(3).forEachIndexed {rowIndex,palettes->
            val row=LinearLayout(context)
            grid.addView(row,LinearLayout.LayoutParams(-1,-2).apply {bottomMargin=dp(10)})
            palettes.forEachIndexed {col,palette->
                val index=rowIndex*3+col
                val card=LinearLayout(context).apply {
                    id=generateViewId();orientation=LinearLayout.VERTICAL;isFocusable=true;isClickable=true
                    setPadding(dp(4),dp(4),dp(4),dp(10));background=shape(0x08FFFFFF)
                    isSelected=index==selectedIndex
                    contentDescription=palette.name+if(isSelected)", selected" else ""
                }
                val picture=FrameLayout(context).apply {
                    background=shape(Color.parseColor(palette.bg),6);clipToOutline=true
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                }
                // Matching layout/time makes palette differences easy to compare. Never call start().
                picture.addView(ConsoleView(context,ConsoleConfig(theme=index,automatic=false,mixedPalettes=false,
                    density=0,brightness=100,scanlines=false,seed=42)),FrameLayout.LayoutParams(-1,-1))
                if(index==selectedIndex)picture.addView(ImageView(context).apply {setImageResource(R.drawable.selection_check)},
                    FrameLayout.LayoutParams(dp(22),dp(22),Gravity.TOP or Gravity.END).apply {setMargins(dp(5),dp(5),dp(5),dp(5))})
                card.addView(picture,LinearLayout.LayoutParams(-1,dp(110)))
                card.addView(TextView(context).apply {
                    text=palette.name;textSize=12f;typeface=resources.getFont(R.font.google_sans);setTextColor(0xFFE9EEF2.toInt())
                    gravity=Gravity.CENTER;includeFontPadding=false;isSingleLine=true;ellipsize=android.text.TextUtils.TruncateAt.END
                    importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO
                },LinearLayout.LayoutParams(-1,dp(26)))
                fun fill(){card.background=shape(if(card.hasFocus() || card.isHovered)0x30FFFFFF else 0x08FFFFFF)}
                card.setOnFocusChangeListener {_,_->fill()}
                card.setOnHoverListener {_,event->
                    when(event.actionMasked){android.view.MotionEvent.ACTION_HOVER_ENTER->card.isHovered=true;android.view.MotionEvent.ACTION_HOVER_EXIT->card.isHovered=false}
                    fill();false
                }
                card.setOnClickListener {choose(index)}
                cards.add(card)
                row.addView(card,LinearLayout.LayoutParams(0,-2,1f).apply {if(col<2)marginEnd=dp(10)})
            }
            repeat(3-palettes.size){row.addView(Space(context),LinearLayout.LayoutParams(0,1,1f))}
        }
        cards.forEachIndexed {i,v->
            v.nextFocusLeftId=if(i%3==0)v.id else cards[i-1].id
            v.nextFocusRightId=if(i%3==2 || i==cards.lastIndex)v.id else cards[i+1].id
            v.nextFocusUpId=cards.getOrNull(i-3)?.id ?: v.id
            v.nextFocusDownId=cards.getOrNull(i+3)?.id ?: if(i/3<(cards.size-1)/3)cards.last().id else closeId
            v.setOnKeyListener {_,key,event->
                val target=when(key) {
                    android.view.KeyEvent.KEYCODE_DPAD_LEFT->v.nextFocusLeftId
                    android.view.KeyEvent.KEYCODE_DPAD_RIGHT->v.nextFocusRightId
                    android.view.KeyEvent.KEYCODE_DPAD_UP->v.nextFocusUpId
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN->v.nextFocusDownId
                    else->null
                }
                if(target==null)false else {
                    if(event.action==android.view.KeyEvent.ACTION_DOWN)rootView.findViewById<View>(target)?.requestFocus()
                    true
                }
            }
        }
    }
}
