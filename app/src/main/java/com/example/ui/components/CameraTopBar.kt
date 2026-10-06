package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.filled.Timer10
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.camera.CameraUiState
import com.example.camera.CameraViewModel
import com.example.data.CameraFlashMode
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold

@Composable
fun CameraTopBar(
  viewModel: CameraViewModel,
  uiState: CameraUiState,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(Color.Black.copy(alpha = 0.65f))
      .padding(horizontal = 8.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Flash Toggle
    IconButton(
      onClick = { viewModel.toggleFlash() },
      modifier = Modifier
        .size(48.dp)
        .testTag("flash_toggle_button")
    ) {
      val icon = when (uiState.flashMode) {
        CameraFlashMode.AUTO -> Icons.Filled.FlashAuto
        CameraFlashMode.ON -> Icons.Filled.FlashOn
        CameraFlashMode.OFF -> Icons.Filled.FlashOff
        CameraFlashMode.TORCH -> Icons.Filled.Highlight
      }
      val tint = if (uiState.flashMode != CameraFlashMode.OFF) AccentGold else Color.White
      Icon(icon, contentDescription = "Flash ${uiState.flashMode.label}", tint = tint)
    }

    // HDR Ultra Toggle
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(16.dp))
        .background(
          if (uiState.isHdrUltra) AccentCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f)
        )
        .clickable { viewModel.toggleHdrUltra() }
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .testTag("hdr_toggle_button"),
      contentAlignment = Alignment.Center
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Filled.HdrOn,
          contentDescription = "HDR Ultra",
          tint = if (uiState.isHdrUltra) AccentCyan else Color.LightGray,
          modifier = Modifier.size(18.dp)
        )
        Text(
          text = if (uiState.isHdrUltra) "HDR ULTRA" else "HDR OFF",
          color = if (uiState.isHdrUltra) AccentCyan else Color.LightGray,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(start = 4.dp)
        )
      }
    }

    // Timer Toggle
    IconButton(
      onClick = { viewModel.cycleTimer() },
      modifier = Modifier
        .size(48.dp)
        .testTag("timer_toggle_button")
    ) {
      val (icon, color) = when (uiState.timerSeconds) {
        3 -> Icons.Filled.Timer3 to AccentGold
        5 -> Icons.Filled.Timer to AccentGold
        10 -> Icons.Filled.Timer10 to AccentGold
        else -> Icons.Filled.Timer to Color.White
      }
      Icon(icon, contentDescription = "Timer ${uiState.timerSeconds}s", tint = color)
    }

    // Aspect Ratio Button
    Box(
      modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .background(Color.White.copy(alpha = 0.12f))
        .clickable { viewModel.toggleAspectRatio() }
        .padding(horizontal = 10.dp, vertical = 6.dp)
        .testTag("aspect_ratio_button"),
      contentAlignment = Alignment.Center
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Filled.AspectRatio,
          contentDescription = "Aspect Ratio",
          tint = Color.White,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = uiState.aspectRatio.label,
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(start = 4.dp)
        )
      }
    }

    // Grid toggle
    IconButton(
      onClick = { viewModel.cycleGrid() },
      modifier = Modifier
        .size(48.dp)
        .testTag("grid_toggle_button")
    ) {
      Icon(
        Icons.Filled.GridOn,
        contentDescription = "Grid: ${uiState.gridMode.label}",
        tint = if (uiState.gridMode != com.example.data.CameraGridMode.NONE) AccentCyan else Color.White.copy(alpha = 0.6f)
      )
    }
  }
}
