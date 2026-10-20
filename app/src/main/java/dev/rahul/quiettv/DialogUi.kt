package dev.rahul.quiettv

import android.app.AlertDialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ReplacementSpan
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** Shared dark card menus and compact centered icon actions for Home and MK-IV. */
object DialogUi {
    private val ink=0xFFE9EEF2.toInt()
    private fun dp(c:Context,n:Int)=(n*c.resources.displayMetrics.density).toInt()
    private fun rounded(c:Context,color:Int,radius:Int=9)=GradientDrawable().apply {setColor(color);cornerRadius=dp(c,radius).toFloat()}
    private fun accent(c:Context)=try {Color.parseColor(c.getSharedPreferences("home",Context.MODE_PRIVATE).getString("accent","#AADBCB"))}catch(_:Exception){0xFFAADBCB.toInt()}
    private fun states(c:Context)=StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused),rounded(c,0x30FFFFFF))
        addState(intArrayOf(android.R.attr.state_hovered),rounded(c,0x24FFFFFF))
        addState(intArrayOf(android.R.attr.state_pressed),rounded(c,0x30FFFFFF))
        addState(intArrayOf(),rounded(c,0x0CFFFFFF))
    }
    fun compactTitle(c:Context,title:String)=TextView(c).apply {
        text=title;textSize=22f;setTextColor(ink);typeface=c.resources.getFont(R.font.google_sans)
        includeFontPadding=false;setPadding(dp(c,24),dp(c,20),dp(c,24),dp(c,4))
        if(android.os.Build.VERSION.SDK_INT>=28)isAccessibilityHeading=true
    }
    fun style(dialog:AlertDialog,width:Int=620) {
        val c=dialog.context
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.apply {
            setBackgroundDrawable(rounded(c,0xFF0E151E.toInt(),12));setDimAmount(.28f)
            setLayout(dp(c,width).coerceAtMost(c.resources.displayMetrics.widthPixels-dp(c,48)),ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        listOf(AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL).forEach {which->
            val button=dialog.getButton(which) ?: return@forEach
            if(button.visibility!=View.VISIBLE)return@forEach
            val title=button.text.toString()
            val iconRes=if(title.startsWith("Save"))R.drawable.huge_save else if(title in listOf("Close","Cancel","Back","Keep editing on TV"))R.drawable.huge_cancel else R.drawable.huge_reset
            val icon=c.getDrawable(iconRes)!!.mutate().apply {setTint(accent(c));setBounds(0,0,dp(c,16),dp(c,16))}
            button.isAllCaps=false;button.textSize=13f;button.setTextColor(ink);button.typeface=c.resources.getFont(R.font.google_sans)
            button.setCompoundDrawables(null,null,null,null)
            button.text=SpannableString("  $title").apply {setSpan(CenteredIcon(icon),0,1,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)}
            button.contentDescription=title;button.includeFontPadding=false;button.gravity=Gravity.CENTER
            button.setPadding(dp(c,15),0,dp(c,15),0);button.minHeight=0;button.minimumHeight=0;button.minWidth=dp(c,100)
            button.backgroundTintList=null;button.background=states(c);button.stateListAnimator=null
            button.layoutParams=button.layoutParams.apply {
                height=dp(c,36);this.width=ViewGroup.LayoutParams.WRAP_CONTENT
                if(this is ViewGroup.MarginLayoutParams){marginStart=dp(c,6);marginEnd=dp(c,6);topMargin=0;bottomMargin=0}
            }
            (button.parent as? LinearLayout)?.apply {
                // Native baseline alignment can lift compact buttons outside the bar's clip bounds.
                isBaselineAligned=false
                gravity=(gravity and Gravity.VERTICAL_GRAVITY_MASK.inv()) or Gravity.CENTER_VERTICAL
                setPadding(paddingLeft,dp(c,8),paddingRight,dp(c,12))
                minimumHeight=dp(c,56)
            }
        }
    }
    fun choose(c:Context,title:String,items:List<String>,selected:Int=-1,icon:Int=R.drawable.huge_settings,action:(Int)->Unit):AlertDialog {
        val adapter=object:BaseAdapter() {
            override fun getCount()=items.size
            override fun getItem(position:Int)=items[position]
            override fun getItemId(position:Int)=position.toLong()
            override fun getView(position:Int,old:View?,parent:ViewGroup):View {
                val row=LinearLayout(c).apply {
                    gravity=Gravity.CENTER_VERTICAL;orientation=LinearLayout.HORIZONTAL
                    setPadding(dp(c,16),dp(c,11),dp(c,16),dp(c,11));minimumHeight=dp(c,46)
                    background=rounded(c,if(position==selected)0x1CFFFFFF else 0x0CFFFFFF)
                }
                row.addView(ImageView(c).apply {setImageResource(icon);setColorFilter(accent(c));importantForAccessibility=View.IMPORTANT_FOR_ACCESSIBILITY_NO},LinearLayout.LayoutParams(dp(c,19),dp(c,19)).apply {marginEnd=dp(c,12)})
                row.addView(TextView(c).apply {
                    text=items[position];textSize=14f;setTextColor(ink);includeFontPadding=false;typeface=c.resources.getFont(R.font.google_sans)
                },LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1f))
                if(position==selected)row.addView(ImageView(c).apply {setImageResource(R.drawable.selection_check);contentDescription="Selected"},LinearLayout.LayoutParams(dp(c,19),dp(c,19)).apply {marginStart=dp(c,12)})
                return row
            }
        }
        val dialog=AlertDialog.Builder(c).setTitle(title).setAdapter(adapter) {d,i->d.dismiss();action(i)}.setNegativeButton("Close",null).create()
        dialog.show();style(dialog)
        if(items.size>6)dialog.window?.setLayout(dialog.window!!.attributes.width,c.resources.displayMetrics.heightPixels-dp(c,48))
        dialog.listView.apply {
            scrollIndicators=0
            setPadding(dp(c,20),dp(c,4),dp(c,20),dp(c,8));clipToPadding=false
            divider=android.graphics.drawable.ColorDrawable(Color.TRANSPARENT);dividerHeight=dp(c,8)
            selector=rounded(c,0x20FFFFFF);setDrawSelectorOnTop(true)
            if(selected>=0)setSelection(selected)
        }
        return dialog
    }
    private class CenteredIcon(private val drawable:Drawable):ReplacementSpan() {
        override fun getSize(paint:Paint,text:CharSequence,start:Int,end:Int,fm:Paint.FontMetricsInt?)=drawable.bounds.width()
        override fun draw(canvas:Canvas,text:CharSequence,start:Int,end:Int,x:Float,top:Int,y:Int,bottom:Int,paint:Paint) {
            val metrics=paint.fontMetrics
            canvas.save();canvas.translate(x,y+(metrics.ascent+metrics.descent)/2-drawable.bounds.height()/2f);drawable.draw(canvas);canvas.restore()
        }
    }
}
