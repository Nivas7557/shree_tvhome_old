package shree.tvhome.design

import android.animation.ValueAnimator
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import shree.tvhome.source.LauncherUtils
import shree.tvhome.source.TileThemePalette
import kotlin.math.ceil

class FluidAppGrid(context: Context) : FrameLayout(context) {
    var columns = 6
    var gap = 0
    var tileW = 0
    var tileH = 0
    private var curCellW = 0
    private var curCellH = 0

    init {
        clipChildren = false
        clipToPadding = false
    }

    fun updateGridState(newCols: Int, newTileW: Int, newTileH: Int, newGap: Int, animate: Boolean = true) {
        columns = newCols.coerceIn(TileThemePalette.GRID_COL_RANGE)
        gap = newGap

        val sizeChanged = (newTileW != tileW || newTileH != tileH) && childCount > 0
        val oldTileW = if (tileW > 0) tileW else newTileW
        val oldTileH = if (tileH > 0) tileH else newTileH

        tileW = newTileW
        tileH = newTileH
        if (!animate || !sizeChanged) {
            curCellW = tileW
            curCellH = tileH
        }

        val topPad = LauncherUtils.dpF(context, 8f)

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.animate().cancel()
            val row = i / columns
            val col = i % columns

            val targetX = col * (tileW + gap).toFloat()
            val targetY = topPad + row * (tileH + gap).toFloat()

            if (animate) {
                child.animate()
                    .translationX(targetX)
                    .translationY(targetY)
                    .setDuration(360L)
                    .setInterpolator(LauncherUtils.FLUID_SPRING)
                    .start()
            } else {
                child.translationX = targetX
                child.translationY = targetY
            }
        }

        if (animate && sizeChanged) {
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 360L
                interpolator = LauncherUtils.FLUID_SPRING
                addUpdateListener { anim ->
                    val f = anim.animatedValue as Float
                    curCellW = (oldTileW + (tileW - oldTileW) * f).toInt()
                    curCellH = (oldTileH + (tileH - oldTileH) * f).toInt()
                    requestLayout()
                }
                start()
            }
        } else {
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        if (childCount == 0 || tileW <= 0 || tileH <= 0) {
            setMeasuredDimension(w, 0)
            return
        }
        val mw = if (curCellW > 0) curCellW else tileW
        val mh = if (curCellH > 0) curCellH else tileH
        val childWSpec = MeasureSpec.makeMeasureSpec(mw, MeasureSpec.EXACTLY)
        val childHSpec = MeasureSpec.makeMeasureSpec(mh, MeasureSpec.EXACTLY)
        for (i in 0 until childCount) {
            getChildAt(i).measure(childWSpec, childHSpec)
        }
        val rows = ceil(childCount.toDouble() / columns).toInt()
        val topPad = LauncherUtils.dp(context, 8)
        val bottomPad = LauncherUtils.dp(context, 24)
        val totalH = topPad + rows * tileH + maxOf(0, rows - 1) * gap + bottomPad
        setMeasuredDimension(w, totalH)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (childCount == 0 || tileW <= 0 || tileH <= 0) return
        val mw = if (curCellW > 0) curCellW else tileW
        val mh = if (curCellH > 0) curCellH else tileH
        for (i in 0 until childCount) {
            getChildAt(i).layout(0, 0, mw, mh)
        }
    }

    override fun focusSearch(focused: View, direction: Int): View? {
        val index = indexOfChild(focused)
        if (index < 0) return super.focusSearch(focused, direction)

        val row = index / columns
        val col = index % columns
        val totalRows = ceil(childCount.toDouble() / columns).toInt()

        return when (direction) {
            View.FOCUS_RIGHT -> if (col < columns - 1 && index + 1 < childCount) getChildAt(index + 1) else focused
            View.FOCUS_LEFT -> if (col > 0) getChildAt(index - 1) else focused
            View.FOCUS_DOWN -> if (row < totalRows - 1) getChildAt(minOf(index + columns, childCount - 1)) else focused
            View.FOCUS_UP -> if (row > 0) getChildAt(index - columns) else super.focusSearch(focused, direction)
            else -> focused
        }
    }
}
