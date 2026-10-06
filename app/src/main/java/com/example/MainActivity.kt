package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.camera.CameraViewModel
import com.example.ui.components.CameraBottomControls
import com.example.ui.components.CameraTopBar
import com.example.ui.components.CameraViewfinder
import com.example.ui.components.GalleryScreen
import com.example.ui.components.ProControlsBar
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentGold
import com.example.ui.theme.CameraBlack
import com.example.ui.theme.HDCameraTheme
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextGray
import com.example.ui.theme.TextWhite

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      HDCameraTheme {
        CameraAppRoot()
      }
    }
  }
}

@Composable
fun CameraAppRoot(
  cameraViewModel: CameraViewModel = viewModel()
) {
  val context = LocalContext.current
  val uiState by cameraViewModel.uiState.collectAsState()

  var hasCameraPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
      ) == PackageManager.PERMISSION_GRANTED
    )
  }

  var permissionRequestedOnce by remember { mutableStateOf(false) }

  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasCameraPermission = isGranted
    permissionRequestedOnce = true
  }

  // Toast notifier
  LaunchedEffect(uiState.statusToastMessage) {
    uiState.statusToastMessage?.let { msg ->
      Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
      cameraViewModel.clearToast()
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(CameraBlack)
  ) {
    if (hasCameraPermission) {
      // Main Camera Interface
      Box(modifier = Modifier.fillMaxSize()) {
        // Viewfinder
        CameraViewfinder(
          viewModel = cameraViewModel,
          uiState = uiState,
          modifier = Modifier.fillMaxSize()
        )

        // Overlay: Top Bar (with status bar insets)
        CameraTopBar(
          viewModel = cameraViewModel,
          uiState = uiState,
          modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
        )

        // Overlay: Bottom Controls & Pro Controls (with navigation bar insets)
        Column(
          modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding(),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          ProControlsBar(
            viewModel = cameraViewModel,
            uiState = uiState
          )
          CameraBottomControls(
            viewModel = cameraViewModel,
            uiState = uiState
          )
        }
      }
    } else {
      // Permission Request / Welcome Screen
      CameraPermissionScreen(
        onRequestPermission = {
          permissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onContinueDemo = {
          hasCameraPermission = true
        }
      )
    }

    // Fullscreen HD Gallery Overlay
    AnimatedVisibility(
      visible = uiState.isGalleryOpen,
      enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
      exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
      modifier = Modifier.fillMaxSize()
    ) {
      GalleryScreen(
        viewModel = cameraViewModel,
        uiState = uiState,
        modifier = Modifier.fillMaxSize()
      )
    }
  }
}

@Composable
fun CameraPermissionScreen(
  onRequestPermission: () -> Unit,
  onContinueDemo: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(CameraBlack)
      .padding(28.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .clip(RoundedCornerShape(24.dp))
        .background(SurfaceCard)
        .padding(28.dp)
        .testTag("camera_permission_card")
    ) {
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(CircleShape)
          .background(AccentCyan.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.Filled.CameraAlt,
          contentDescription = "Camera",
          tint = AccentCyan,
          modifier = Modifier.size(42.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "HD CAMERA",
        color = TextWhite,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        letterSpacing = 1.sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Ultra-HD Clarity & Pro Sensor Quality",
        color = AccentGold,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
      )

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "To take high-definition photos with real-time HDR, pro focus, and maximum camera resolution on any device, please grant camera access.",
        color = TextGray,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        lineHeight = 20.sp
      )

      Spacer(modifier = Modifier.height(24.dp))

      Button(
        onClick = onRequestPermission,
        colors = ButtonDefaults.buttonColors(
          containerColor = AccentCyan,
          contentColor = CameraBlack
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("grant_camera_permission_button")
      ) {
        Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = "Allow Camera Access", fontWeight = FontWeight.Bold, fontSize = 15.sp)
      }

      Spacer(modifier = Modifier.height(12.dp))

      OutlinedButton(
        onClick = onContinueDemo,
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = TextWhite
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("demo_mode_button")
      ) {
        Icon(Icons.Filled.HighQuality, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = "Open in Studio Preview Mode", fontSize = 14.sp)
      }
    }
  }
}
