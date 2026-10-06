package com.example.ui.components

import android.view.ViewGroup
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.CameraUiState
import com.example.camera.CameraViewModel
import com.example.data.CameraGridMode
import com.example.data.PhotoAspectRatio
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGreen
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun CameraViewfinder(
  viewModel: CameraViewModel,
  uiState: CameraUiState,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  var focusPoint by remember { mutableStateOf<Offset?>(null) }
  val focusRingAlpha = remember { Animatable(0f) }

  // Flash white animation on shutter click
  var showWhiteFlash by remember { mutableStateOf(false) }
  LaunchedEffect(uiState.shutterFlashTrigger) {
    if (uiState.shutterFlashTrigger > 0) {
      showWhiteFlash = true
      delay(80)
      showWhiteFlash = false
    }
  }

  val ratioModifier = when (uiState.aspectRatio) {
    PhotoAspectRatio.RATIO_4_3 -> Modifier.aspectRatio(3f / 4f)
    PhotoAspectRatio.RATIO_16_9 -> Modifier.aspectRatio(9f / 16f)
    PhotoAspectRatio.RATIO_1_1 -> Modifier.aspectRatio(1f)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black),
    contentAlignment = Alignment.Center
  ) {
    Box(
      modifier = Modifier
        .then(ratioModifier)
        .pointerInput(Unit) {
          detectTransformGestures { _, _, zoom, _ ->
            val newZoom = (uiState.zoomRatio * zoom).coerceIn(uiState.minZoom, uiState.maxZoom)
            viewModel.setZoom(newZoom)
          }
        }
        .pointerInput(Unit) {
          detectTapGestures { tapOffset ->
            focusPoint = tapOffset
            val cameraControl = viewModel.cameraControl
            if (cameraControl != null) {
              val factory = androidx.camera.core.SurfaceOrientedMeteringPointFactory(
                size.width.toFloat(),
                size.height.toFloat()
              )
              val point = factory.createPoint(tapOffset.x, tapOffset.y)
              val action = FocusMeteringAction.Builder(point).build()
              cameraControl.startFocusAndMetering(action)
            }
          }
        }
    ) {
      // CameraX AndroidView
      AndroidView(
        factory = { ctx ->
          val previewView = PreviewView(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
          }

          val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
          cameraProviderFuture.addListener({
            try {
              val cameraProvider = cameraProviderFuture.get()

              val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(uiState.lensFacing)
                .build()

              val preview = Preview.Builder().build()
              preview.surfaceProvider = previewView.surfaceProvider

              val targetRatio = when (uiState.aspectRatio) {
                PhotoAspectRatio.RATIO_16_9 -> AspectRatio.RATIO_16_9
                else -> AspectRatio.RATIO_4_3
              }

              val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setTargetAspectRatio(targetRatio)
                .build()

              cameraProvider.unbindAll()
              val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
              )

              viewModel.onCameraBound(camera, imageCapture)
            } catch (e: Exception) {
              e.printStackTrace()
            }
          }, ContextCompat.getMainExecutor(ctx))

          previewView
        },
        modifier = Modifier
          .fillMaxSize()
          .testTag("camera_preview_view")
      )

      // Grid overlay
      if (uiState.gridMode != CameraGridMode.NONE) {
        GridOverlay(
          gridMode = uiState.gridMode,
          modifier = Modifier.fillMaxSize()
        )
      }

      // Animated Tap to Focus ring
      focusPoint?.let { pt ->
        LaunchedEffect(pt) {
          focusRingAlpha.snapTo(1f)
          delay(1200)
          focusRingAlpha.animateTo(0f, tween(400))
          focusPoint = null
        }

        val density = LocalDensity.current
        val ringSize = 64.dp
        val ringPx = with(density) { ringSize.toPx() }

        Box(
          modifier = Modifier
            .offset {
              IntOffset(
                (pt.x - ringPx / 2).roundToInt(),
                (pt.y - ringPx / 2).roundToInt()
              )
            }
            .size(ringSize)
        ) {
          Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
              color = AccentCyan.copy(alpha = focusRingAlpha.value),
              style = Stroke(width = 2.dp.toPx())
            )
            // Center pip
            drawCircle(
              color = AccentCyan.copy(alpha = focusRingAlpha.value),
              radius = 2.dp.toPx()
            )
          }
        }
      }

      // HD Live watermark indicator
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .offset(x = (-12).dp, y = 12.dp)
          .background(Color.Black.copy(alpha = 0.55f), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp)
      ) {
        Text(
          text = if (uiState.isHdrUltra) "4K ULTRA HD" else "FULL HD",
          color = if (uiState.isHdrUltra) AccentCyan else Color.White,
          fontSize = 11.sp,
          fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
      }

      // Shutter White Flash animation
      AnimatedVisibility(
        visible = showWhiteFlash,
        enter = fadeIn(tween(30)),
        exit = fadeOut(tween(80)),
        modifier = Modifier.fillMaxSize()
      ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White))
      }
    }
  }
}

@Composable
fun GridOverlay(
  gridMode: CameraGridMode,
  modifier: Modifier = Modifier
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val strokeColor = Color.White.copy(alpha = 0.35f)
    val strokeWidth = 1.dp.toPx()

    when (gridMode) {
      CameraGridMode.NONE -> {}
      CameraGridMode.THIRDS -> {
        // Vertical 3rds
        drawLine(strokeColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth)
        drawLine(strokeColor, Offset(2 * w / 3f, 0f), Offset(2 * w / 3f, h), strokeWidth)
        // Horizontal 3rds
        drawLine(strokeColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth)
        drawLine(strokeColor, Offset(0f, 2 * h / 3f), Offset(w, 2 * h / 3f), strokeWidth)
      }
      CameraGridMode.CROSSHAIR -> {
        val cx = w / 2f
        val cy = h / 2f
        val armLen = 24.dp.toPx()
        // Center crosshair
        drawLine(AccentGreen, Offset(cx - armLen, cy), Offset(cx + armLen, cy), 1.5.dp.toPx())
        drawLine(AccentGreen, Offset(cx, cy - armLen), Offset(cx, cy + armLen), 1.5.dp.toPx())
        drawCircle(AccentGreen, radius = 6.dp.toPx(), style = Stroke(1.5.dp.toPx()))
      }
      CameraGridMode.GOLDEN -> {
        // Golden ratio ~0.618
        val phi = 0.618f
        drawLine(strokeColor, Offset(w * (1 - phi), 0f), Offset(w * (1 - phi), h), strokeWidth)
        drawLine(strokeColor, Offset(w * phi, 0f), Offset(w * phi, h), strokeWidth)
        drawLine(strokeColor, Offset(0f, h * (1 - phi)), Offset(w, h * (1 - phi)), strokeWidth)
        drawLine(strokeColor, Offset(0f, h * phi), Offset(w, h * phi), strokeWidth)
      }
    }
  }
}
