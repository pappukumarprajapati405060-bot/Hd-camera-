package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.CapturedPhoto
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PhotoDetailDialog(
  photo: CapturedPhoto,
  onDismiss: () -> Unit
) {
  val dateFormatted = SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.US).format(Date(photo.timestamp))

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = SurfaceCard,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("photo_detail_dialog")
    ) {
      Column(
        modifier = Modifier.padding(20.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.HighQuality, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(24.dp))
            Text(
              text = "Ultra-HD Photo Specs",
              color = TextWhite,
              fontWeight = FontWeight.Bold,
              fontSize = 17.sp,
              modifier = Modifier.padding(start = 8.dp)
            )
          }
          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextGray)
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        Spacer(modifier = Modifier.height(14.dp))

        // High resolution badge row
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AccentCyan.copy(alpha = 0.15f))
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = "Clarity & Resolution", color = TextGray, fontSize = 12.sp)
              Text(
                text = "${photo.megaPixels} (${photo.formattedResolution})",
                color = AccentCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AccentGold)
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(text = "TRUE HD", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        DetailRow(icon = Icons.Filled.Storage, label = "File Size", value = photo.formattedFileSize)
        DetailRow(icon = Icons.Filled.Camera, label = "Camera Lens", value = photo.lensFacing)
        DetailRow(icon = Icons.Filled.Image, label = "Capture Mode", value = photo.captureMode)
        DetailRow(icon = Icons.Filled.HighQuality, label = "HD Color Profile", value = photo.filterApplied)
        DetailRow(icon = Icons.Filled.Info, label = "Exposure", value = photo.exposureCompensation)
        DetailRow(icon = Icons.Filled.Info, label = "Date & Time", value = dateFormatted)
      }
    }
  }
}

@Composable
private fun DetailRow(
  icon: ImageVector,
  label: String,
  value: String
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(16.dp))
      Text(
        text = label,
        color = TextGray,
        fontSize = 13.sp,
        modifier = Modifier.padding(start = 8.dp)
      )
    }
    Text(
      text = value,
      color = TextWhite,
      fontWeight = FontWeight.SemiBold,
      fontSize = 13.sp
    )
  }
}
