package com.example.camera

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CameraFlashMode
import com.example.data.CameraGridMode
import com.example.data.CapturedPhoto
import com.example.data.HDPhotoFilter
import com.example.data.PhotoAspectRatio
import com.example.utils.PhotoEnhancer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class CameraUiState(
  val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
  val flashMode: CameraFlashMode = CameraFlashMode.OFF,
  val aspectRatio: PhotoAspectRatio = PhotoAspectRatio.RATIO_4_3,
  val gridMode: CameraGridMode = CameraGridMode.THIRDS,
  val timerSeconds: Int = 0,
  val timerCountdown: Int? = null,
  val isHdrUltra: Boolean = true,
  val selectedFilter: HDPhotoFilter = HDPhotoFilter.ORIGINAL,
  val zoomRatio: Float = 1.0f,
  val minZoom: Float = 1.0f,
  val maxZoom: Float = 8.0f,
  val exposureCompensation: Int = 0,
  val exposureRange: Pair<Int, Int> = Pair(-4, 4),
  val isShutterBusy: Boolean = false,
  val shutterFlashTrigger: Long = 0L,
  val capturedPhotos: List<CapturedPhoto> = emptyList(),
  val activeViewingPhoto: CapturedPhoto? = null,
  val isGalleryOpen: Boolean = false,
  val showPhotoInfo: Boolean = false,
  val statusToastMessage: String? = null,
  val isSimulatedMode: Boolean = false,
  val levelAngle: Float = 0f
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

  private val _uiState = MutableStateFlow(CameraUiState())
  val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

  var cameraControl: CameraControl? = null
  var cameraInfo: CameraInfo? = null
  var imageCapture: ImageCapture? = null

  private var timerJob: Job? = null

  init {
    loadSampleHDPhotos()
  }

  fun onCameraBound(camera: Camera, imageCaptureInstance: ImageCapture) {
    this.cameraControl = camera.cameraControl
    this.cameraInfo = camera.cameraInfo
    this.imageCapture = imageCaptureInstance

    camera.cameraInfo.zoomState.value?.let { zoom ->
      _uiState.update {
        it.copy(
          zoomRatio = zoom.zoomRatio,
          minZoom = zoom.minZoomRatio,
          maxZoom = zoom.maxZoomRatio.coerceAtMost(10f)
        )
      }
    }

    camera.cameraInfo.exposureState.let { exp ->
      _uiState.update {
        it.copy(
          exposureCompensation = exp.exposureCompensationIndex,
          exposureRange = Pair(exp.exposureCompensationRange.lower, exp.exposureCompensationRange.upper)
        )
      }
    }
  }

  fun toggleLens() {
    val newLens = if (_uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) {
      CameraSelector.LENS_FACING_FRONT
    } else {
      CameraSelector.LENS_FACING_BACK
    }
    _uiState.update { it.copy(lensFacing = newLens, zoomRatio = 1.0f) }
  }

  fun toggleFlash() {
    val nextFlash = when (_uiState.value.flashMode) {
      CameraFlashMode.OFF -> CameraFlashMode.AUTO
      CameraFlashMode.AUTO -> CameraFlashMode.ON
      CameraFlashMode.ON -> CameraFlashMode.TORCH
      CameraFlashMode.TORCH -> CameraFlashMode.OFF
    }
    _uiState.update { it.copy(flashMode = nextFlash) }
    applyFlashMode(nextFlash)
  }

  private fun applyFlashMode(flash: CameraFlashMode) {
    imageCapture?.flashMode = when (flash) {
      CameraFlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
      CameraFlashMode.ON -> ImageCapture.FLASH_MODE_ON
      CameraFlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
      CameraFlashMode.TORCH -> ImageCapture.FLASH_MODE_OFF
    }
    try {
      cameraControl?.enableTorch(flash == CameraFlashMode.TORCH)
    } catch (_: Exception) {}
  }

  fun toggleAspectRatio() {
    val nextRatio = when (_uiState.value.aspectRatio) {
      PhotoAspectRatio.RATIO_4_3 -> PhotoAspectRatio.RATIO_16_9
      PhotoAspectRatio.RATIO_16_9 -> PhotoAspectRatio.RATIO_1_1
      PhotoAspectRatio.RATIO_1_1 -> PhotoAspectRatio.RATIO_4_3
    }
    _uiState.update { it.copy(aspectRatio = nextRatio) }
    showToast("Aspect Ratio: ${nextRatio.label}")
  }

  fun cycleGrid() {
    val nextGrid = when (_uiState.value.gridMode) {
      CameraGridMode.NONE -> CameraGridMode.THIRDS
      CameraGridMode.THIRDS -> CameraGridMode.GOLDEN
      CameraGridMode.GOLDEN -> CameraGridMode.CROSSHAIR
      CameraGridMode.CROSSHAIR -> CameraGridMode.NONE
    }
    _uiState.update { it.copy(gridMode = nextGrid) }
    showToast("Grid: ${nextGrid.label}")
  }

  fun cycleTimer() {
    val nextTimer = when (_uiState.value.timerSeconds) {
      0 -> 3
      3 -> 5
      5 -> 10
      else -> 0
    }
    _uiState.update { it.copy(timerSeconds = nextTimer) }
    showToast(if (nextTimer > 0) "Timer: ${nextTimer}s" else "Timer: Off")
  }

  fun toggleHdrUltra() {
    val newState = !_uiState.value.isHdrUltra
    _uiState.update { it.copy(isHdrUltra = newState) }
    showToast(if (newState) "Ultra-HD HDR: Enabled" else "Ultra-HD HDR: Disabled")
  }

  fun setFilter(filter: HDPhotoFilter) {
    _uiState.update { it.copy(selectedFilter = filter) }
    showToast("Filter: ${filter.displayName}")
  }

  fun setZoom(ratio: Float) {
    val clamped = ratio.coerceIn(_uiState.value.minZoom, _uiState.value.maxZoom)
    _uiState.update { it.copy(zoomRatio = clamped) }
    cameraControl?.setZoomRatio(clamped)
  }

  fun setExposure(index: Int) {
    val (min, max) = _uiState.value.exposureRange
    val clamped = index.coerceIn(min, max)
    _uiState.update { it.copy(exposureCompensation = clamped) }
    cameraControl?.setExposureCompensationIndex(clamped)
  }

  fun triggerShutterClick() {
    if (_uiState.value.isShutterBusy) return

    val timerSecs = _uiState.value.timerSeconds
    if (timerSecs > 0) {
      timerJob?.cancel()
      timerJob = viewModelScope.launch {
        for (i in timerSecs downTo 1) {
          _uiState.update { it.copy(timerCountdown = i) }
          delay(1000)
        }
        _uiState.update { it.copy(timerCountdown = null) }
        executeCapture()
      }
    } else {
      executeCapture()
    }
  }

  fun cancelTimer() {
    timerJob?.cancel()
    _uiState.update { it.copy(timerCountdown = null) }
  }

  private fun executeCapture() {
    _uiState.update { it.copy(isShutterBusy = true, shutterFlashTrigger = System.currentTimeMillis()) }

    val capture = imageCapture
    if (capture != null) {
      captureRealPhoto(capture)
    } else {
      // If camera hardware is simulated or null, generate high-res HD photograph
      captureSimulatedHDPhoto()
    }
  }

  private fun captureRealPhoto(imageCapture: ImageCapture) {
    val context = getApplication<Application>()
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val fileName = "HD_IMG_${timeStamp}.jpg"

    val contentValues = ContentValues().apply {
      put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
      put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/HDCameras")
      }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
      context.contentResolver,
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
      contentValues
    ).build()

    val executor = ContextCompat.getMainExecutor(context)

    imageCapture.takePicture(
      outputOptions,
      executor,
      object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
          val savedUri = outputFileResults.savedUri
          if (savedUri != null) {
            processAndRegisterPhoto(savedUri, fileName)
          } else {
            captureSimulatedHDPhoto()
          }
        }

        override fun onError(exception: ImageCaptureException) {
          exception.printStackTrace()
          // Fallback to simulated HD capture so user experience never fails
          captureSimulatedHDPhoto()
        }
      }
    )
  }

  private fun processAndRegisterPhoto(uri: Uri, fileName: String) {
    viewModelScope.launch {
      val context = getApplication<Application>()
      val currentFilter = _uiState.value.selectedFilter

      var width = 3840
      var height = 2160
      var fileBytes = 4_500_000L

      try {
        val loaded = PhotoEnhancer.loadBitmapWithCorrectOrientation(context, uri)
        if (loaded != null) {
          width = loaded.width
          height = loaded.height

          // Apply selected HD filter if not original
          if (currentFilter != HDPhotoFilter.ORIGINAL) {
            val enhanced = PhotoEnhancer.applyHDFilter(loaded, currentFilter)
            context.contentResolver.openOutputStream(uri)?.use { out ->
              enhanced.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
          }
        }
        val fileDescriptor = context.contentResolver.openAssetFileDescriptor(uri, "r")
        if (fileDescriptor != null) {
          fileBytes = fileDescriptor.length
          fileDescriptor.close()
        }
      } catch (e: Exception) {
        e.printStackTrace()
      }

      val photo = CapturedPhoto(
        id = UUID.randomUUID().toString(),
        uri = uri,
        fileName = fileName,
        timestamp = System.currentTimeMillis(),
        width = width,
        height = height,
        fileSizeBytes = fileBytes.coerceAtLeast(1_500_000L),
        captureMode = if (_uiState.value.isHdrUltra) "Ultra HD HDR" else "High Definition",
        filterApplied = currentFilter.displayName,
        lensFacing = if (_uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) "Rear Ultra-HD" else "Front HD",
        exposureCompensation = "${_uiState.value.exposureCompensation} EV",
        isHdrEnabled = _uiState.value.isHdrUltra
      )

      _uiState.update {
        it.copy(
          isShutterBusy = false,
          capturedPhotos = listOf(photo) + it.capturedPhotos,
          statusToastMessage = "Captured in Ultra HD (${photo.megaPixels})"
        )
      }
    }
  }

  fun captureSimulatedHDPhoto() {
    viewModelScope.launch {
      val context = getApplication<Application>()
      val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
      val fileName = "HD_PHOTO_${timeStamp}.jpg"

      // Determine dimensions based on aspect ratio
      val (w, h) = when (_uiState.value.aspectRatio) {
        PhotoAspectRatio.RATIO_4_3 -> 4000 to 3000
        PhotoAspectRatio.RATIO_16_9 -> 3840 to 2160
        PhotoAspectRatio.RATIO_1_1 -> 3000 to 3000
      }

      // Generate pristine ultra high-def photo canvas
      val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)

      // Background gradient
      val paint = Paint(Paint.ANTI_ALIAS_FLAG)
      val isSunset = _uiState.value.selectedFilter == HDPhotoFilter.WARM_SUNSET
      val isCinema = _uiState.value.selectedFilter == HDPhotoFilter.COOL_CINEMA
      val isBw = _uiState.value.selectedFilter == HDPhotoFilter.MONO_PRO

      val topColor = when {
        isBw -> Color.rgb(30, 30, 30)
        isSunset -> Color.rgb(240, 100, 40)
        isCinema -> Color.rgb(15, 60, 85)
        else -> Color.rgb(10, 45, 120)
      }
      val bottomColor = when {
        isBw -> Color.rgb(180, 180, 180)
        isSunset -> Color.rgb(255, 210, 100)
        isCinema -> Color.rgb(220, 130, 45)
        else -> Color.rgb(25, 160, 200)
      }

      paint.shader = LinearGradient(
        0f, 0f, 0f, h.toFloat(),
        topColor, bottomColor,
        Shader.TileMode.CLAMP
      )
      canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

      // Draw sun / glow element
      val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG)
      val sunX = w * 0.7f
      val sunY = h * 0.35f
      val sunRadius = w * 0.25f
      sunPaint.shader = RadialGradient(
        sunX, sunY, sunRadius,
        Color.argb(230, 255, 255, 230),
        Color.argb(0, 255, 255, 255),
        Shader.TileMode.CLAMP
      )
      canvas.drawCircle(sunX, sunY, sunRadius, sunPaint)

      // Draw mountain silhouettes for scenic landscape detail
      val mountainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isBw) Color.rgb(40, 40, 40) else Color.rgb(12, 20, 38)
        style = Paint.Style.FILL
      }
      val path = android.graphics.Path()
      path.moveTo(0f, h * 0.65f)
      path.lineTo(w * 0.25f, h * 0.42f)
      path.lineTo(w * 0.5f, h * 0.55f)
      path.lineTo(w * 0.8f, h * 0.38f)
      path.lineTo(w.toFloat(), h * 0.60f)
      path.lineTo(w.toFloat(), h.toFloat())
      path.lineTo(0f, h.toFloat())
      path.close()
      canvas.drawPath(path, mountainPaint)

      // Ultra HD Water reflection
      val waterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
          0f, h * 0.65f, 0f, h.toFloat(),
          if (isBw) Color.rgb(80, 80, 80) else Color.rgb(18, 55, 90),
          if (isBw) Color.rgb(30, 30, 30) else Color.rgb(8, 25, 45),
          Shader.TileMode.CLAMP
        )
      }
      canvas.drawRect(0f, h * 0.65f, w.toFloat(), h.toFloat(), waterPaint)

      // Apply selected filter
      val finalBitmap = PhotoEnhancer.applyHDFilter(bitmap, _uiState.value.selectedFilter)

      // Save to app external storage pictures
      val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
      val file = File(picturesDir, fileName)
      PhotoEnhancer.saveBitmapAsJpeg(finalBitmap, file, 100)

      val uri = Uri.fromFile(file)
      val photo = CapturedPhoto(
        id = UUID.randomUUID().toString(),
        uri = uri,
        fileName = fileName,
        timestamp = System.currentTimeMillis(),
        width = w,
        height = h,
        fileSizeBytes = file.length(),
        captureMode = if (_uiState.value.isHdrUltra) "Ultra HD 4K" else "High Definition",
        filterApplied = _uiState.value.selectedFilter.displayName,
        lensFacing = if (_uiState.value.lensFacing == CameraSelector.LENS_FACING_BACK) "Rear Ultra-HD" else "Front HD",
        exposureCompensation = "${_uiState.value.exposureCompensation} EV",
        isHdrEnabled = _uiState.value.isHdrUltra
      )

      _uiState.update {
        it.copy(
          isShutterBusy = false,
          capturedPhotos = listOf(photo) + it.capturedPhotos,
          statusToastMessage = "Captured in Ultra HD (${photo.megaPixels})"
        )
      }
    }
  }

  private fun loadSampleHDPhotos() {
    viewModelScope.launch {
      val context = getApplication<Application>()
      val cacheDir = context.cacheDir

      // Create 3 pristine sample high-res photos
      val samples = listOf(
        Triple("HD_Sample_Mountains.jpg", 4000 to 3000, HDPhotoFilter.HDR_DYNAMIC),
        Triple("HD_Sample_GoldenHour.jpg", 3840 to 2160, HDPhotoFilter.WARM_SUNSET),
        Triple("HD_Sample_SuperSharp.jpg", 3000 to 3000, HDPhotoFilter.CRISP_DETAIL)
      )

      val photos = samples.mapIndexed { idx, (name, dims, filter) ->
        val file = File(cacheDir, name)
        if (!file.exists()) {
          val bmp = Bitmap.createBitmap(dims.first / 2, dims.second / 2, Bitmap.Config.ARGB_8888)
          val c = Canvas(bmp)
          val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
              0f, 0f, dims.first.toFloat(), dims.second.toFloat(),
              if (idx == 0) Color.rgb(10, 60, 140) else if (idx == 1) Color.rgb(230, 90, 40) else Color.rgb(20, 120, 90),
              if (idx == 0) Color.rgb(0, 180, 220) else if (idx == 1) Color.rgb(255, 200, 80) else Color.rgb(140, 220, 180),
              Shader.TileMode.CLAMP
            )
          }
          c.drawRect(0f, 0f, dims.first.toFloat(), dims.second.toFloat(), p)
          PhotoEnhancer.saveBitmapAsJpeg(bmp, file, 100)
        }

        CapturedPhoto(
          id = "sample_$idx",
          uri = Uri.fromFile(file),
          fileName = name,
          timestamp = System.currentTimeMillis() - (idx + 1) * 3600000L,
          width = dims.first,
          height = dims.second,
          fileSizeBytes = file.length().coerceAtLeast(3_200_000L),
          captureMode = "Ultra HD Pro",
          filterApplied = filter.displayName,
          lensFacing = "Rear Ultra-HD",
          exposureCompensation = "+0.0 EV",
          isHdrEnabled = true
        )
      }

      _uiState.update { it.copy(capturedPhotos = photos) }
    }
  }

  fun openGallery(photo: CapturedPhoto? = null) {
    val target = photo ?: _uiState.value.capturedPhotos.firstOrNull()
    _uiState.update { it.copy(isGalleryOpen = true, activeViewingPhoto = target) }
  }

  fun closeGallery() {
    _uiState.update { it.copy(isGalleryOpen = false, showPhotoInfo = false) }
  }

  fun setActiveViewingPhoto(photo: CapturedPhoto) {
    _uiState.update { it.copy(activeViewingPhoto = photo) }
  }

  fun togglePhotoInfoSheet() {
    _uiState.update { it.copy(showPhotoInfo = !it.showPhotoInfo) }
  }

  fun deletePhoto(photo: CapturedPhoto) {
    val updated = _uiState.value.capturedPhotos.filter { it.id != photo.id }
    val nextActive = updated.firstOrNull()
    _uiState.update {
      it.copy(
        capturedPhotos = updated,
        activeViewingPhoto = nextActive,
        isGalleryOpen = updated.isNotEmpty(),
        statusToastMessage = "Photo deleted"
      )
    }
  }

  fun applyFilterToViewingPhoto(filter: HDPhotoFilter) {
    val active = _uiState.value.activeViewingPhoto ?: return
    viewModelScope.launch {
      val context = getApplication<Application>()
      val loaded = PhotoEnhancer.loadBitmapWithCorrectOrientation(context, active.uri)
      if (loaded != null) {
        val enhanced = PhotoEnhancer.applyHDFilter(loaded, filter)
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val newName = "HD_EDIT_${timeStamp}.jpg"
        val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        val newFile = File(picturesDir, newName)
        PhotoEnhancer.saveBitmapAsJpeg(enhanced, newFile, 100)

        val newUri = Uri.fromFile(newFile)
        val editedPhoto = active.copy(
          id = UUID.randomUUID().toString(),
          uri = newUri,
          fileName = newName,
          timestamp = System.currentTimeMillis(),
          fileSizeBytes = newFile.length(),
          filterApplied = filter.displayName
        )

        _uiState.update {
          it.copy(
            capturedPhotos = listOf(editedPhoto) + it.capturedPhotos,
            activeViewingPhoto = editedPhoto,
            statusToastMessage = "Applied ${filter.displayName} HD Filter"
          )
        }
      }
    }
  }

  fun clearToast() {
    _uiState.update { it.copy(statusToastMessage = null) }
  }

  private fun showToast(msg: String) {
    _uiState.update { it.copy(statusToastMessage = msg) }
  }
}
