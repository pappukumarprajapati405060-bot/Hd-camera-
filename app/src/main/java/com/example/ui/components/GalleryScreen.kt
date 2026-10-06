package com.example.ui.components

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.camera.CameraUiState
import com.example.camera.CameraViewModel
import com.example.data.HDPhotoFilter
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.AccentRed
import com.example.ui.theme.CameraBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextWhite

@Composable
fun GalleryScreen(
  viewModel: CameraViewModel,
  uiState: CameraUiState,
  modifier: Modifier = Modifier
) {
  BackHandler {
    viewModel.closeGallery()
  }

  val context = LocalContext.current
  val activePhoto = uiState.activeViewingPhoto ?: uiState.capturedPhotos.firstOrNull()

  var scale by remember(activePhoto?.id) { mutableFloatStateOf(1f) }
  var offset by remember(activePhoto?.id) { mutableStateOf(Offset.Zero) }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  if (uiState.showPhotoInfo && activePhoto != null) {
    PhotoDetailDialog(
      photo = activePhoto,
      onDismiss = { viewModel.togglePhotoInfoSheet() }
    )
  }

  if (showDeleteConfirm && activePhoto != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Delete Photo?", color = TextWhite) },
      text = { Text("Are you sure you want to delete this Ultra-HD photo?", color = Color.LightGray) },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deletePhoto(activePhoto)
            showDeleteConfirm = false
          }
        ) {
          Text("Delete", color = AccentRed)
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) {
          Text("Cancel", color = Color.White)
        }
      },
      containerColor = SurfaceCard
    )
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(CameraBlack)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    // Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
        .background(Color.Black.copy(alpha = 0.7f))
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = { viewModel.closeGallery() },
        modifier = Modifier.size(48.dp).testTag("gallery_back_button")
      ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Camera", tint = Color.White)
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = activePhoto?.fileName ?: "HD Photo",
          color = Color.White,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )
        if (activePhoto != null) {
          Text(
            text = "${activePhoto.megaPixels} • ${activePhoto.formattedResolution}",
            color = AccentCyan,
            fontSize = 11.sp
          )
        }
      }

      Row {
        // Info Button
        IconButton(
          onClick = { viewModel.togglePhotoInfoSheet() },
          modifier = Modifier.size(48.dp).testTag("photo_info_button")
        ) {
          Icon(Icons.Filled.Info, contentDescription = "HD Photo Specs", tint = AccentGold)
        }

        // Share Button
        IconButton(
          onClick = {
            activePhoto?.let { photo ->
              val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, photo.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              }
              context.startActivity(Intent.createChooser(shareIntent, "Share Ultra-HD Photo"))
            }
          },
          modifier = Modifier.size(48.dp).testTag("photo_share_button")
        ) {
          Icon(Icons.Filled.Share, contentDescription = "Share Photo", tint = Color.White)
        }

        // Delete Button
        IconButton(
          onClick = { showDeleteConfirm = true },
          modifier = Modifier.size(48.dp).testTag("photo_delete_button")
        ) {
          Icon(Icons.Filled.Delete, contentDescription = "Delete Photo", tint = AccentRed)
        }
      }
    }

    // Zoomable Image Canvas
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = 64.dp, bottom = 170.dp)
        .pointerInput(activePhoto?.id) {
          detectTransformGestures { _, pan, zoom, _ ->
            scale = (scale * zoom).coerceIn(1f, 5f)
            if (scale > 1f) {
              val maxOffset = 500f * (scale - 1f)
              offset = Offset(
                (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
              )
            } else {
              offset = Offset.Zero
            }
          }
        },
      contentAlignment = Alignment.Center
    ) {
      if (activePhoto != null) {
        Image(
          painter = rememberAsyncImagePainter(activePhoto.uri),
          contentDescription = "HD Photo Viewer",
          contentScale = ContentScale.Fit,
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
              scaleX = scale,
              scaleY = scale,
              translationX = offset.x,
              translationY = offset.y
            )
            .testTag("full_hd_photo_image")
        )

        // HD Quality Watermark Badge
        Box(
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(16.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text(
            text = "${activePhoto.megaPixels} • ${activePhoto.filterApplied}",
            color = AccentCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      } else {
        Text("No photos captured yet", color = Color.Gray, fontSize = 16.sp)
      }
    }

    // Bottom Controls & Filmstrip
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .background(Color.Black.copy(alpha = 0.85f))
        .padding(vertical = 8.dp)
    ) {
      // HD Enhance Quick Filters Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          Icons.Filled.AutoFixHigh,
          contentDescription = null,
          tint = AccentGold,
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = "HD Studio Enhancement:",
          color = AccentGold,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(start = 6.dp)
        )
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState())
          .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        HDPhotoFilter.values().forEach { filter ->
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(Color.White.copy(alpha = 0.1f))
              .clickable { viewModel.applyFilterToViewingPhoto(filter) }
              .padding(horizontal = 10.dp, vertical = 4.dp)
              .testTag("gallery_filter_${filter.name}")
          ) {
            Text(
              text = filter.displayName,
              color = Color.White,
              fontSize = 11.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Filmstrip thumbnail carousel
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        items(uiState.capturedPhotos, key = { it.id }) { photo ->
          val isSelected = photo.id == activePhoto?.id
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(RoundedCornerShape(8.dp))
              .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) AccentCyan else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
              )
              .clickable {
                viewModel.setActiveViewingPhoto(photo)
                scale = 1f
                offset = Offset.Zero
              }
              .testTag("filmstrip_item_${photo.id}")
          ) {
            Image(
              painter = rememberAsyncImagePainter(photo.uri),
              contentDescription = photo.fileName,
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }
        }
      }
    }
  }
}
