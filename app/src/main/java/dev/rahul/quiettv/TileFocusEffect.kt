package dev.rahul.quiettv

import android.animation.ValueAnimator
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.animation.PathInterpolator

/** Short transform/opacity feedback; reversing focus continues from the current value. */
class TileFocusEffect(private val tile:View,private val image:View,private val glow:View,private val caption:View,onFocus:()->Unit) {
    private var hovered=false
    private val curve=PathInterpolator(.23f,1f,.32f,1f)
    init {
        tile.setOnFocusChangeListener { _,focused ->if(focused)onFocus();update() }
        tile.setOnHoverListener { _,event ->
            val accessibility=tile.context.getSystemService(AccessibilityManager::class.java)
            if(event.isFromSource(InputDevice.SOURCE_MOUSE) && !accessibility.isTouchExplorationEnabled) {
                val wasHovered=hovered
                if(event.actionMasked==MotionEvent.ACTION_HOVER_ENTER)hovered=true
                if(event.actionMasked==MotionEvent.ACTION_HOVER_EXIT)hovered=false
                if(wasHovered!=hovered)update()
            }
            false
        }
    }
    private fun update() {
        val active=tile.hasFocus() || hovered
        val motion=ValueAnimator.areAnimatorsEnabled()
        tile.animate().cancel();image.animate().cancel();glow.animate().cancel()
        val size=if(active && motion)1.035f else 1f
        val opacity=if(active)1f else .86f;val fill=if(active).10f else 0f
        caption.visibility=if(active)View.VISIBLE else View.INVISIBLE
        if(motion) {
            tile.animate().scaleX(size).scaleY(size).setDuration(160).setInterpolator(curve).start()
            image.animate().alpha(opacity).setDuration(160).setInterpolator(curve).start()
            glow.animate().alpha(fill).setDuration(160).setInterpolator(curve).start()
        } else {
            tile.scaleX=1f;tile.scaleY=1f;image.alpha=opacity;glow.alpha=fill
        }
    }
    fun stop() {
        tile.animate().cancel();image.animate().cancel();glow.animate().cancel()
        tile.scaleX=1f;tile.scaleY=1f;hovered=false
    }
}
