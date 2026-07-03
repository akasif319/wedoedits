package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GeoBackground)
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Profile Header card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = GeoSurfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar representation with gradient ring
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(GeoPrimary, GeoPrimaryVariant, GeoPrimary)
                            )
                        )
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎬",
                            fontSize = 36.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Cinematic Director",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )

                Text(
                    text = "Level 8 Verified Editor • Pro Creator",
                    fontSize = 12.sp,
                    color = GeoPrimary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStat("14", "Exports")
                    ProfileStat("128.4s", "Compiled")
                    ProfileStat("5", "Drafts")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Achieved badges list
        Text(
            text = "Your Creator Badges",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BadgeCard("🔥 7-Day Active", Color(0xFFFF5500))
            BadgeCard("⚡ Ultra Render", GeoPrimary)
            BadgeCard("👑 Pro Designer", Color(0xFFFFCC00))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Options List
        Text(
            text = "Settings & Support",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        SettingsRow("Cloud Auto-Backup", Icons.Default.CloudQueue, "Enabled (128MB free)") {
            Toast.makeText(context, "Cloud backup syncing is fully operational!", Toast.LENGTH_SHORT).show()
        }
        SettingsRow("Clear Render Cache", Icons.Default.DeleteOutline, "48.2 MB") {
            Toast.makeText(context, "Cleared 48.2 MB of temporary export cache!", Toast.LENGTH_SHORT).show()
        }
        SettingsRow("Hardware Acceleration", Icons.Default.OfflineBolt, "GPU Compositing (ON)") {
            Toast.makeText(context, "Hardware rendering acceleration is locked & optimized!", Toast.LENGTH_SHORT).show()
        }
        SettingsRow("About CapCut Pro", Icons.Default.Info, "v1.4.2 Production") {
            Toast.makeText(context, "CapCut Pro Android. Built with 100% Native Jetpack Compose.", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
fun BadgeCard(label: String, tint: Color) {
    Box(
        modifier = Modifier
            .background(tint.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .border(1.dp, tint.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    statusText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = GeoSurfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(statusText, color = GeoPrimary, fontSize = 12.sp)
        }
    }
}
