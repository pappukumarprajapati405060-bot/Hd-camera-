package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.camera.CameraUiState
import com.example.camera.CameraViewModel
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentRed
import com.example.ui.theme.ShutterInner
import com.example.ui.theme.ShutterOuterRing

@Composable
fun CameraBottomControls(
  viewModel: CameraViewModel,
  uiState: CameraUiState,
  modifier: Modifier = Modifier
) {
  val latestPhoto = uiState.capturedPhotos.firstOrNull()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(Color.Black)
      .padding(horizontal = 24.dp, vertical = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // If timer is counting down, show prominent countdown
    if (uiState.timerCountdown != null) {
      Row(
        modifier = Modifier
          .padding(bottom = 12.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(AccentRed.copy(alpha = 0.25f))
          .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Shooting in ${uiState.timerCountdown}s...",
          color = AccentGold,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
        IconButton(
          onClick = { viewModel.cancelTimer() },
          modifier = Modifier.size(24.dp).padding(start = 6.dp)
        ) {
          Icon(Icons.Filled.Close, contentDescription = "Cancel Timer", tint = Color.White)
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Gallery Thumbnail / Shortcut
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(CircleShape)
          .background(Color.DarkGray)
          .border(2.dp, AccentCyan.copy(alpha = 0.8f), CircleShape)
          .clickable { viewModel.openGallery() }
          .testTag("gallery_thumbnail_button"),
        contentAlignment = Alignment.Center
      ) {
        if (latestPhoto != null) {
          Image(
            painter = rememberAsyncImagePainter(latestPhoto.uri),
            contentDescription = "Latest HD Photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(54.dp)
          )
        } else {
          Icon(
            Icons.Filled.PhotoLibrary,
            contentDescription = "Gallery",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
          )
        }
      }

      // Shutter Button
      ShutterButton(
        isBusy = uiState.isShutterBusy,
        timerCountdown = uiState.timerCountdown,
        onClick = { viewModel.triggerShutterClick() }
      )

      // Lens Switch Button
      Box(
        modifier = Modifier
          .size(54.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.15f))
          .clickable { viewModel.toggleLens() }
          .testTag("switch_lens_button"),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Filled.Cameraswitch,
          contentDescription = "Switch Camera Lens",
          tint = Color.White,
          modifier = Modifier.size(28.dp)
        )
      }
    }
  }
}

@Composable
fun ShutterButton(
  isBusy: Boolean,
  timerCountdown: Int?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition()
  val pulsingScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    )
  )

  Box(
    modifier = modifier
      .size(80.dp)
      .border(4.dp, ShutterOuterRing, CircleShape)
      .padding(6.dp)
      .clip(CircleShape)
      .background(if (timerCountdown != null) AccentRed else ShutterInner)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = !isBusy
      ) { onClick() }
      .testTag("shutter_button"),
    contentAlignment = Alignment.Center
  ) {
    if (isBusy) {
      CircularProgressIndicator(
        modifier = Modifier.size(36.dp),
        color = Color.Black,
        strokeWidth = 3.dp
      )
    } else if (timerCountdown != null) {
      Text(
        text = timerCountdown.toString(),
        color = Color.White,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.scale(pulsingScale)
      )
    } else {
      // Inner circle detail
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(Color.White)
      )
    }
  }
}
