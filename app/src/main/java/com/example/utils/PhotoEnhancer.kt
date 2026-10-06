package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import com.example.data.HDPhotoFilter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object PhotoEnhancer {

  fun loadBitmapWithCorrectOrientation(context: Context, uri: Uri): Bitmap? {
    var inputStream: InputStream? = null
    return try {
      inputStream = context.contentResolver.openInputStream(uri) ?: return null
      val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeStream(inputStream, null, bounds)
      inputStream.close()

      // Calculate sample size if extremely huge (e.g. > 8000px) to prevent OOM
      val maxDimension = max(bounds.outWidth, bounds.outHeight)
      var sampleSize = 1
      while (maxDimension / sampleSize > 4096) {
        sampleSize *= 2
      }

      val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }

      inputStream = context.contentResolver.openInputStream(uri) ?: return null
      val decoded = BitmapFactory.decodeStream(inputStream, null, decodeOptions) ?: return null
      inputStream.close()

      // Check orientation
      val exifStream = context.contentResolver.openInputStream(uri)
      val orientation = if (exifStream != null) {
        val exif = ExifInterface(exifStream)
        val orient = exif.getAttributeInt(
          ExifInterface.TAG_ORIENTATION,
          ExifInterface.ORIENTATION_NORMAL
        )
        exifStream.close()
        orient
      } else {
        ExifInterface.ORIENTATION_NORMAL
      }

      val matrix = Matrix()
      when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
      }

      if (!matrix.isIdentity) {
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
      } else {
        decoded
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    } finally {
      try {
        inputStream?.close()
      } catch (_: Exception) {}
    }
  }

  fun applyHDFilter(source: Bitmap, filter: HDPhotoFilter): Bitmap {
    if (filter == HDPhotoFilter.ORIGINAL) {
      return source
    }

    val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val cm = ColorMatrix()

    when (filter) {
      HDPhotoFilter.ORIGINAL -> {
        // No-op
      }
      HDPhotoFilter.ULTRA_VIBRANT -> {
        // Boost saturation by 35% and slight contrast boost
        cm.setSaturation(1.35f)
        val contrastMatrix = getContrastMatrix(1.15f)
        cm.postConcat(contrastMatrix)
      }
      HDPhotoFilter.HDR_DYNAMIC -> {
        // HDR look: Lift shadows, punch midtones, moderate saturation
        cm.setSaturation(1.2f)
        val hdrMatrix = ColorMatrix(
          floatArrayOf(
            1.12f, 0f, 0f, 0f, 12f,
            0f, 1.12f, 0f, 0f, 12f,
            0f, 0f, 1.12f, 0f, 12f,
            0f, 0f, 0f, 1f, 0f
          )
        )
        cm.postConcat(hdrMatrix)
      }
      HDPhotoFilter.CRISP_DETAIL -> {
        // Micro contrast & sharp tone mapping
        cm.setSaturation(1.1f)
        val sharpMatrix = getContrastMatrix(1.22f)
        cm.postConcat(sharpMatrix)
      }
      HDPhotoFilter.WARM_SUNSET -> {
        // Golden hour warm tint
        val warmMatrix = ColorMatrix(
          floatArrayOf(
            1.25f, 0f, 0f, 0f, 20f,
            0f, 1.05f, 0f, 0f, 10f,
            0f, 0f, 0.85f, 0f, -10f,
            0f, 0f, 0f, 1f, 0f
          )
        )
        cm.setSaturation(1.15f)
        cm.postConcat(warmMatrix)
      }
      HDPhotoFilter.MONO_PRO -> {
        // Deep monochrome with punchy contrast
        cm.setSaturation(0f)
        val bwContrast = getContrastMatrix(1.3f)
        cm.postConcat(bwContrast)
      }
      HDPhotoFilter.COOL_CINEMA -> {
        // Teal shadows & orange highlights
        val cinemaMatrix = ColorMatrix(
          floatArrayOf(
            1.18f, 0f, 0f, 0f, 15f,
            0f, 1.02f, 0f, 0f, 5f,
            0f, 0f, 1.22f, 0f, 20f,
            0f, 0f, 0f, 1f, 0f
          )
        )
        cm.setSaturation(1.1f)
        cm.postConcat(cinemaMatrix)
      }
    }

    paint.colorFilter = ColorMatrixColorFilter(cm)
    canvas.drawBitmap(source, 0f, 0f, paint)
    return result
  }

  private fun getContrastMatrix(contrast: Float): ColorMatrix {
    val scale = contrast
    val translate = (-0.5f * scale + 0.5f) * 255f
    return ColorMatrix(
      floatArrayOf(
        scale, 0f, 0f, 0f, translate,
        0f, scale, 0f, 0f, translate,
        0f, 0f, scale, 0f, translate,
        0f, 0f, 0f, 1f, 0f
      )
    )
  }

  fun saveBitmapAsJpeg(bitmap: Bitmap, targetFile: File, quality: Int = 100): Boolean {
    return try {
      FileOutputStream(targetFile).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
      }
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }
}
