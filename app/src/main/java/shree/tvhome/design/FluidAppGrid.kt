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

    init {
        clipChildren = false
        clipToPadding = false
    }

    fun updateGridState(newCols: Int, newTileW: Int, newTileH: Int, newGap: Int, animate: Boolean = true) {
        columns = newCols.coerceIn(TileThemePalette.GRID_COL_RANGE)
        gap = newGap

        val sizeChanged = (newTileW != tileW || newTileH != tileH) && childCount > 0
        val oldTileW = tileW
        val oldTileH = tileH

        tileW = newTileW
        tileH = newTileH

        val topPad = LauncherUtils.dpF(context, 8f)

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val row = i / columns
            val col = i % columns

            val targetX = col * (tileW + gap).toFloat()
            val targetY = topPad + row * (tileH + gap).toFloat()

            if (animate) {
                child.animate()
                    .x(targetX).y(targetY)
                    .setDuration(420L)
                    .setInterpolator(LauncherUtils.FLUID_SPRING)
                    .start()
            } else {
                child.x = targetX
                child.y = targetY
                if (sizeChanged) {
                    child.layoutParams.width = tileW
                    child.layoutParams.height = tileH
                }
            }
        }

        if (!animate) requestLayout()

        if (animate && sizeChanged) {
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 420L
                interpolator = LauncherUtils.FLUID_SPRING
                addUpdateListener { anim ->
                    val f = anim.animatedValue as Float
                    val curW = (oldTileW + (tileW - oldTileW) * f).toInt()
                    val curH = (oldTileH + (tileH - oldTileH) * f).toInt()
                    for (i in 0 until childCount) {
                        val c = getChildAt(i)
                        c.layoutParams.width = curW
                        c.layoutParams.height = curH
                    }
                    requestLayout()
                }
                start()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        if (childCount == 0 || tileW <= 0 || tileH <= 0) {
            setMeasuredDimension(w, 0)
            return
        }
        val childWSpec = MeasureSpec.makeMeasureSpec(tileW, MeasureSpec.EXACTLY)
        val childHSpec = MeasureSpec.makeMeasureSpec(tileH, MeasureSpec.EXACTLY)
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
        val topPad = LauncherUtils.dp(context, 8)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val row = i / columns
            val col = i % columns
            val l = col * (tileW + gap)
            val t = topPad + row * (tileH + gap)
            child.layout(l, t, l + tileW, t + tileH)
        }
    }

    override fun focusSearch(focused: View, direction: Int): View? {
        val index = indexOfChild(focused)
        if (index < 0) return super.focusSearch(focused, direction)

        val row = index / columns
        val col = index % columns
        val totalRows = ceil(childCount.toDouble() / columns).toInt()

        return when (direction) {
            View.FOCUS_RIGHT -> {
                if (col < columns - 1 && index + 1 < childCount) getChildAt(index + 1) else focused
            }
            View.FOCUS_LEFT -> {
                if (col > 0) getChildAt(index - 1) else focused
            }
            View.FOCUS_DOWN -> {
                if (row < totalRows - 1) {
                    val nextIndex = minOf(index + columns, childCount - 1)
                    getChildAt(nextIndex)
                } else focused
            }
            View.FOCUS_UP -> {
                if (row > 0) getChildAt(index - columns) else super.focusSearch(focused, direction)
            }
            else -> focused
        }
    }
}
