package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.data.HDPhotoFilter
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold

@Composable
fun ProControlsBar(
  viewModel: CameraViewModel,
  uiState: CameraUiState,
  modifier: Modifier = Modifier
) {
  var showExposureSlider by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Exposure Slider popup if toggled
    AnimatedVisibility(visible = showExposureSlider) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 6.dp)
          .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(20.dp))
          .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "${if (uiState.exposureCompensation > 0) "+" else ""}${uiState.exposureCompensation} EV",
          color = AccentGold,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.width(48.dp)
        )
        Slider(
          value = uiState.exposureCompensation.toFloat(),
          onValueChange = { viewModel.setExposure(it.toInt()) },
          valueRange = uiState.exposureRange.first.toFloat()..uiState.exposureRange.second.toFloat(),
          steps = (uiState.exposureRange.second - uiState.exposureRange.first - 1).coerceAtLeast(0),
          colors = SliderDefaults.colors(
            thumbColor = AccentGold,
            activeTrackColor = AccentGold,
            inactiveTrackColor = Color.DarkGray
          ),
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp)
            .testTag("exposure_slider")
        )
      }
    }

    // Zoom and EV row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 2.dp),
      horizontalArrangement = Arrangement.Center,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // EV Button
      Box(
        modifier = Modifier
          .padding(end = 8.dp)
          .size(34.dp)
          .clip(CircleShape)
          .background(if (showExposureSlider) AccentGold else Color.Black.copy(alpha = 0.6f))
          .clickable { showExposureSlider = !showExposureSlider }
          .testTag("ev_button"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Filled.Tune,
          contentDescription = "Exposure Compensation",
          tint = if (showExposureSlider) Color.Black else Color.White,
          modifier = Modifier.size(18.dp)
        )
      }

      // Zoom presets: 1x, 2x, 5x
      val zoomOptions = listOf(1.0f, 2.0f, 5.0f)
      zoomOptions.forEach { zoom ->
        val isSelected = kotlin.math.abs(uiState.zoomRatio - zoom) < 0.2f
        Box(
          modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(if (isSelected) AccentCyan else Color.Black.copy(alpha = 0.55f))
            .border(
              width = 1.dp,
              color = if (isSelected) AccentCyan else Color.White.copy(alpha = 0.25f),
              shape = CircleShape
            )
            .clickable { viewModel.setZoom(zoom) }
            .testTag("zoom_button_${zoom.toInt()}x"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "${zoom.toInt()}×",
            color = if (isSelected) Color.Black else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // HD Filter Selector Carousel
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState())
        .padding(horizontal = 12.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      HDPhotoFilter.values().forEach { filter ->
        val isSelected = uiState.selectedFilter == filter
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
              if (isSelected) AccentCyan.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.5f)
            )
            .border(
              width = 1.dp,
              color = if (isSelected) AccentCyan else Color.White.copy(alpha = 0.2f),
              shape = RoundedCornerShape(16.dp)
            )
            .clickable { viewModel.setFilter(filter) }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("filter_${filter.name}"),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = filter.displayName,
            color = if (isSelected) AccentCyan else Color.White,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
          )
        }
      }
    }
  }
}
