package dev.rahul.quiettv

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** One expanded section, real focusable controls, no hidden focus targets. */
class SettingsAccordion(context: Context) : LinearLayout(context) {
    private data class Section(val title: String, val icon: Int, val header: TextView, val content: LinearLayout)
    private val sections=mutableListOf<Section>()
    var expandedIndex=0
        private set
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    init { orientation=VERTICAL }
    private fun fill(focused:Boolean)=GradientDrawable().apply {
        setColor(if(focused)0x30FFFFFF else 0x12FFFFFF);cornerRadius=dp(10).toFloat()
    }
    fun section(title:String, icon:Int):LinearLayout {
        val index=sections.size
        val header=TextView(context).apply {
            id=View.generateViewId();text=title;textSize=16f;setTextColor(Color.rgb(233,238,242))
            typeface=resources.getFont(R.font.google_sans);setFontVariationSettings("'wght' 500")
            gravity=Gravity.CENTER_VERTICAL;includeFontPadding=false
            setPadding(dp(14),dp(15),dp(14),dp(15));compoundDrawablePadding=dp(12)
            isFocusable=true;isClickable=true;background=fill(false)
            setOnFocusChangeListener { v,focused ->v.background=fill(focused) }
            setOnClickListener { expand(if(expandedIndex==index)-1 else index) }
        }
        val content=LinearLayout(context).apply { orientation=VERTICAL;setPadding(dp(8),dp(8),dp(8),dp(4)) }
        sections.add(Section(title,icon,header,content))
        addView(header,LayoutParams(-1,-2).apply { topMargin=if(index==0)0 else dp(8) })
        addView(content,LayoutParams(-1,-2));expand(expandedIndex)
        return content
    }
    fun expand(index:Int) {
        sections.forEachIndexed { i,s ->
            if(i!=index && s.content.hasFocus())s.header.requestFocus()
            s.content.visibility=if(i==index)View.VISIBLE else View.GONE
            fun drawable(res:Int)=context.getDrawable(res)!!.mutate().apply {setTint(0xFFAADBCB.toInt());setBounds(0,0,dp(21),dp(21))}
            s.header.setCompoundDrawablesRelative(drawable(s.icon),null,drawable(if(i==index)R.drawable.chevron_up else R.drawable.chevron_down),null)
            s.header.contentDescription=s.title+if(i==index)", expanded" else ", collapsed"
        }
        expandedIndex=index
        updateFocusRoutes()
    }
    fun updateFocusRoutes() {
        val visible=mutableListOf<View>()
        sections.forEachIndexed { i,s ->
            visible.add(s.header)
            if(i==expandedIndex) for(n in 0 until s.content.childCount) {
                val v=s.content.getChildAt(n);if(v.isFocusable && v.visibility==View.VISIBLE)visible.add(v)
            }
        }
        visible.forEachIndexed { i,v ->
            v.nextFocusUpId=visible.getOrNull(i-1)?.id ?: v.id
            v.nextFocusDownId=visible.getOrNull(i+1)?.id ?: v.id
        }
    }
    fun focusFirst() {sections.firstOrNull()?.header?.requestFocus()}
}
