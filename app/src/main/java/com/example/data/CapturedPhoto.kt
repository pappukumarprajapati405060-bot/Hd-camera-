package com.example.data

import android.net.Uri

data class CapturedPhoto(
  val id: String,
  val uri: Uri,
  val fileName: String,
  val timestamp: Long,
  val width: Int,
  val height: Int,
  val fileSizeBytes: Long,
  val captureMode: String = "Ultra HD",
  val filterApplied: String = "Normal HD",
  val lensFacing: String = "Rear Ultra-HD",
  val exposureCompensation: String = "0.0 EV",
  val isHdrEnabled: Boolean = true
) {
  val megaPixels: String
    get() {
      val mp = (width.toLong() * height.toLong()) / 1_000_000.0
      return if (mp >= 1.0) String.format("%.1f MP", mp) else String.format("%.2f MP", mp)
    }

  val formattedResolution: String
    get() = "${width} × ${height}"

  val formattedFileSize: String
    get() {
      val kb = fileSizeBytes / 1024.0
      return if (kb >= 1024.0) {
        String.format("%.2f MB", kb / 1024.0)
      } else {
        String.format("%.1f KB", kb)
      }
    }
}

enum class CameraFlashMode(val label: String) {
  AUTO("Auto"),
  ON("On"),
  OFF("Off"),
  TORCH("Torch")
}

enum class PhotoAspectRatio(val label: String, val ratioFloat: Float) {
  RATIO_4_3("4:3", 4f / 3f),
  RATIO_16_9("16:9", 16f / 9f),
  RATIO_1_1("1:1", 1f / 1f)
}

enum class CameraGridMode(val label: String) {
  NONE("Off"),
  THIRDS("3×3"),
  GOLDEN("Golden"),
  CROSSHAIR("Target")
}

enum class HDPhotoFilter(val displayName: String, val description: String) {
  ORIGINAL("Raw HD", "Pure sensor output with zero post-processing"),
  ULTRA_VIBRANT("Ultra HD Color", "Enhanced saturation, dynamic contrast and vivid skies"),
  HDR_DYNAMIC("Pro HDR", "High dynamic range tone mapping with shadow lift"),
  CRISP_DETAIL("Super Sharp", "Micro-contrast and edge clarity enhancement"),
  WARM_SUNSET("Golden Hour", "Rich warm amber highlights and soft shadows"),
  MONO_PRO("Noir HD", "Deep black & white with high dynamic tonal curve"),
  COOL_CINEMA("Teal & Orange", "Cinematic color grading for portraits & cityscapes")
}
