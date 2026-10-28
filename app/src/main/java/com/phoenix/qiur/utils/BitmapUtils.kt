package com.phoenix.qiur.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface

fun Uri.toBitmap(context: Context): Bitmap =
    context.contentResolver.openInputStream(this).use { inputStream ->
        BitmapFactory.decodeStream(inputStream)
    }

fun Bitmap.rotate(degrees: Int): Bitmap {
    if (degrees == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.applyExif(orientation: Int): Bitmap {
    val m = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { m.postRotate(90f); m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { m.postRotate(-90f); m.postScale(-1f, 1f) }
        else -> return this
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
}

fun Bitmap.downscale(): Bitmap {
    val base = this.width / 720f
    val downHeight = this.height / base
    val downWidth = this.width / base
    return this.scale(downWidth.toInt(), downHeight.toInt())
}

fun Bitmap.withLabel(text: String, textSizePx: Float = 36f): Bitmap {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = textSizePx
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
    }
    val srcW = width
    val srcH = height
    val extra = (textSizePx * 1.6f).toInt()
    val out = createBitmap(srcW, srcH + extra)
    Canvas(out).apply {
        drawColor(Color.WHITE)
        drawBitmap(this@withLabel, 0f, 0f, null)
        drawText(text, srcW / 2f, srcH + textSizePx * 1.1f, paint)
    }
    return out
}

fun render1D(
    bars: BooleanArray,
    scale: Int = 4,
    barHeight: Int = 160,
    quietModules: Int = 10,
): Bitmap {
    val w = (bars.size + quietModules * 2) * scale
    val px = IntArray(w * barHeight) { Color.WHITE }
    for (i in bars.indices) {
        if (!bars[i]) continue
        for (dx in 0 until scale) {
            val x = (i + quietModules) * scale + dx
            for (y in 0 until barHeight) px[y * w + x] = Color.BLACK
        }
    }
    return Bitmap.createBitmap(px, w, barHeight, Bitmap.Config.ARGB_8888)
}

fun renderMatrix(
    modules: Int,
    isDark: (x: Int, y: Int) -> Boolean,
    scale: Int = 10,
    quietModules: Int = 4,
): Bitmap {
    val size = (modules + quietModules * 2) * scale
    val px = IntArray(size * size) { Color.WHITE }
    for (y in 0 until modules) for (x in 0 until modules) {
        if (!isDark(x, y)) continue
        val ox = (x + quietModules) * scale
        val oy = (y + quietModules) * scale
        for (dy in 0 until scale) for (dx in 0 until scale)
            px[(oy + dy) * size + ox + dx] = Color.BLACK
    }
    return Bitmap.createBitmap(px, size, size, Bitmap.Config.ARGB_8888)
}