package shree.tvhome.panels

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import shree.tvhome.source.LauncherPreferences
import shree.tvhome.source.LauncherUtils
import shree.tvhome.source.MotionEngine

object ProfileCropDialog {

    private data class CropBtnSpec(val label: String, val weight: Float, val onClick: () -> Unit)

    fun show(
        ctx: Context,
        lp: LauncherPreferences,
        onPickPhoto: () -> Unit,
        onSaved: () -> Unit
    ) {
        val dialog = Dialog(ctx)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        var zoom = lp.profilePicZoom
        var panX = lp.profilePicPanX
        var panY = lp.profilePicPanY
        var rawBitmap: Bitmap? = loadUserBitmap(ctx, lp.profilePicUri)


        val rootGlass = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(ctx, 24), dp(ctx, 20), dp(ctx, 24), dp(ctx, 20))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.argb(242, 24, 36, 78), Color.argb(248, 12, 18, 44))
            ).apply {
                cornerRadius = LauncherUtils.dpF(ctx, 24f)
                setStroke(dp(ctx, 1), Color.argb(140, 255, 255, 255))
            }
        }

        rootGlass.addView(TextView(ctx).apply {
            text = "✦  Profile & Avatar Studio"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, dp(ctx, 14))
        })

        val contentRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val previewSize = dp(ctx, 150)
        val cropPreview = object : View(ctx) {
            private val circlePath = Path()
            private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
            private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = Color.argb(70, 255, 255, 255)
                strokeWidth = 1.5f
            }
            private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
            }
            private val drawMatrix = Matrix()

            override fun onDraw(canvas: Canvas) {
                val cx = width / 2f
                val cy = height / 2f
                val r = minOf(cx, cy) - dp(ctx, 4)

                circlePath.reset()
                circlePath.addCircle(cx, cy, r, Path.Direction.CW)

                canvas.save()
                canvas.clipPath(circlePath)

                val bmp = rawBitmap
                if (bmp != null) {
                    val baseScale = maxOf((r * 2f) / bmp.width, (r * 2f) / bmp.height)
                    val finalScale = baseScale * zoom
                    drawMatrix.reset()
                    drawMatrix.postTranslate(-bmp.width / 2f, -bmp.height / 2f)
                    drawMatrix.postScale(finalScale, finalScale)
                    drawMatrix.postTranslate(cx + (panX * r), cy + (panY * r))
                    canvas.drawBitmap(bmp, drawMatrix, fillPaint)

                    canvas.drawLine(cx - r, cy, cx + r, cy, gridPaint)
                    canvas.drawLine(cx, cy - r, cx, cy + r, gridPaint)
                } else {
                    fillPaint.color = lp.accentColor
                    canvas.drawCircle(cx, cy, r, fillPaint)
                    textPaint.textSize = r * 0.65f
                    val initial = lp.userName.ifBlank { "S" }.trim().first().uppercaseChar().toString()
                    val ty = cy - ((textPaint.descent() + textPaint.ascent()) / 2f)
                    canvas.drawText(initial, cx, ty, textPaint)
                }
                canvas.restore()

                rimPaint.strokeWidth = LauncherUtils.dpF(ctx, 2.5f)
                rimPaint.color = lp.accentColor
                canvas.drawCircle(cx, cy, r, rimPaint)
            }
        }
        contentRow.addView(cropPreview, LinearLayout.LayoutParams(previewSize, previewSize).apply {
            rightMargin = dp(ctx, 20)
        })

        val rightCol = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

        val nameInput = EditText(ctx).apply {
            isSingleLine = true
            hint = "Enter Profile Name"
            setText(lp.userName)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.argb(150, 200, 220, 245))
            setPadding(dp(ctx, 12), dp(ctx, 9), dp(ctx, 12), dp(ctx, 9))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            background = GradientDrawable().apply {
                setColor(Color.argb(55, 255, 255, 255))
                cornerRadius = LauncherUtils.dpF(ctx, 12f)
                setStroke(dp(ctx, 1), lp.accentColor)
            }
        }
        rightCol.addView(nameInput, LinearLayout.LayoutParams(dp(ctx, 260), -2))

        val controlRows = listOf(
            listOf(
                CropBtnSpec("🖼  Choose Photo", 1.4f) { dialog.dismiss(); onPickPhoto() },
                CropBtnSpec("✕ Remove", 1f) {
                    rawBitmap = null
                    zoom = 1.0f; panX = 0f; panY = 0f
                    lp.prefs.edit()
                        .remove(LauncherPreferences.PREF_PROFILE_PIC_URI)
                        .putFloat(LauncherPreferences.PREF_PROFILE_PIC_ZOOM, 1f)
                        .putFloat(LauncherPreferences.PREF_PROFILE_PIC_PAN_X, 0f)
                        .putFloat(LauncherPreferences.PREF_PROFILE_PIC_PAN_Y, 0f)
                        .apply()
                    cropPreview.invalidate()
                    onSaved()
                }
            ),
            listOf(
                CropBtnSpec("🔍 +", 1f) { zoom = (zoom + 0.15f).coerceAtMost(2.8f); cropPreview.invalidate() },
                CropBtnSpec("🔍 −", 1f) { zoom = (zoom - 0.15f).coerceAtLeast(1.0f); cropPreview.invalidate() },
                CropBtnSpec("↺ Reset", 1f) { zoom = 1.0f; panX = 0f; panY = 0f; cropPreview.invalidate() }
            ),
            listOf(
                CropBtnSpec("◀", 1f) { panX = (panX - 0.10f).coerceIn(-0.85f, 0.85f); cropPreview.invalidate() },
                CropBtnSpec("▶", 1f) { panX = (panX + 0.10f).coerceIn(-0.85f, 0.85f); cropPreview.invalidate() },
                CropBtnSpec("▲", 1f) { panY = (panY - 0.10f).coerceIn(-0.85f, 0.85f); cropPreview.invalidate() },
                CropBtnSpec("▼", 1f) { panY = (panY + 0.10f).coerceIn(-0.85f, 0.85f); cropPreview.invalidate() }
            )
        )

        controlRows.forEachIndexed { rIdx, specs ->
            val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            specs.forEachIndexed { cIdx, spec ->
                row.addView(
                    makeGlossBtn(ctx, lp, spec.label, rIdx == 0 && cIdx == 0, spec.onClick),
                    LinearLayout.LayoutParams(0, -2, spec.weight).apply {
                        if (cIdx < specs.lastIndex) rightMargin = dp(ctx, if (rIdx == 2) 4 else 6)
                    }
                )
            }
            rightCol.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, if (rIdx == 0) 10 else 8) })
        }

        contentRow.addView(rightCol)
        rootGlass.addView(contentRow)

        val actionRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        actionRow.addView(makeGlossBtn(ctx, lp, "Cancel", false) {
            dialog.dismiss()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = dp(ctx, 8) })

        actionRow.addView(makeGlossBtn(ctx, lp, "✓  Save Profile", true) {
            lp.userName = nameInput.text.toString().trim()
            lp.profilePicZoom = zoom
            lp.profilePicPanX = panX
            lp.profilePicPanY = panY
            onSaved()
            Toast.makeText(ctx, "Profile saved!", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }, LinearLayout.LayoutParams(0, -2, 1.3f))
        rootGlass.addView(actionRow, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 16) })

        dialog.setContentView(rootGlass)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun loadUserBitmap(ctx: Context, uriStr: String?): Bitmap? {
        if (uriStr.isNullOrEmpty()) return null
        return try {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            ctx.contentResolver.openInputStream(Uri.parse(uriStr))?.use {
                BitmapFactory.decodeStream(it, null, opts)
            }
        } catch (_: Exception) { null }
    }

    private fun makeGlossBtn(ctx: Context, lp: LauncherPreferences, label: String, highlight: Boolean, onClick: () -> Unit) =
        TextView(ctx).apply {
            text = label
            textSize = 10.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            setPadding(dp(ctx, 8), dp(ctx, 8), dp(ctx, 8), dp(ctx, 8))
            background = btnBg(ctx, lp.accentColor, highlight)
            setOnFocusChangeListener { v, f ->
                MotionEngine.animateFocus(v, f, lp.motionStyle, 1.04f, 4f)
                v.background = btnBg(ctx, lp.accentColor, f || highlight)
            }
            setOnClickListener { onClick() }
        }

    private fun btnBg(ctx: Context, accent: Int, active: Boolean) = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        if (active) intArrayOf(LauncherUtils.withAlpha(accent, 185), LauncherUtils.withAlpha(accent, 110))
        else intArrayOf(Color.argb(65, 255, 255, 255), Color.argb(30, 255, 255, 255))
    ).apply {
        cornerRadius = LauncherUtils.dpF(ctx, 12f)
        setStroke(dp(ctx, 1), if (active) Color.WHITE else Color.argb(80, 255, 255, 255))
    }

    private fun dp(ctx: Context, v: Int) = LauncherUtils.dp(ctx, v)
}
