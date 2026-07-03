package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.data.ProjectEntity
import com.example.data.TrackType
import com.example.data.TemplateAssets
import com.example.engine.VideoEditorViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: VideoEditorViewModel,
    onNavigateToEditor: () -> Unit,
    onNavigateToDrafts: () -> Unit
) {
    val context = LocalContext.current
    val drafts by viewModel.drafts.collectAsState()
    val scrollState = rememberScrollState()

    // Photo/Video Picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        // Start a new project
        viewModel.createNewProject()
        
        if (uris.isNotEmpty()) {
            // Import chosen clips into timeline
            uris.forEachIndexed { index, uri ->
                val fileName = uri.path?.substringAfterLast('/') ?: "Clip ${index + 1}"
                viewModel.addClipToTimeline(
                    type = TrackType.VIDEO,
                    title = fileName,
                    duration = 5000L, // default 5 seconds
                    filePathOrAssetId = uri.toString()
                )
            }
            Toast.makeText(context, "Imported ${uris.size} clips into timeline!", Toast.LENGTH_SHORT).show()
        } else {
            // Emulator fallback: load gorgeous predefined cinematic Stock Clips!
            val tokyo = TemplateAssets.stockVideos[0]
            val forest = TemplateAssets.stockVideos[1]
            val ocean = TemplateAssets.stockVideos[2]

            viewModel.addClipToTimeline(TrackType.VIDEO, tokyo.title, tokyo.durationMs, tokyo.id)
            viewModel.addClipToTimeline(TrackType.VIDEO, forest.title, forest.durationMs, forest.id) {
                it.copy(startInTimeline = tokyo.durationMs)
            }
            viewModel.addClipToTimeline(TrackType.VIDEO, ocean.title, ocean.durationMs, ocean.id) {
                it.copy(startInTimeline = tokyo.durationMs + forest.durationMs)
            }

            // Also insert automatic bg music!
            val music = TemplateAssets.stockAudios[0]
            viewModel.addClipToTimeline(TrackType.AUDIO, music.title, 15000L, music.id)

            Toast.makeText(context, "No gallery files chosen. Loaded Cinematic Stock Templates!", Toast.LENGTH_LONG).show()
        }
        onNavigateToEditor()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GeoBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 80.dp) // nav padding
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "CapCut Pro",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Create cinematic magic",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        Toast.makeText(context, "Help & Tutorials center loaded!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Help", tint = Color.LightGray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Outlined.AccountCircle,
                        contentDescription = "Profile",
                        tint = GeoPrimary,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                Toast.makeText(context, "Creator Profile panel opened!", Toast.LENGTH_SHORT).show()
                            }
                    )
                }
            }

            // Hero Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_hero_banner_1783075046939),
                        contentDescription = "Hero workspace banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Gradient overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xCC000000)),
                                    startY = 100f
                                )
                            )
                    )
                    
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "AI Smart Subtitles & Color Grading",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Create professional cinematic videos with a single tap.",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main "New Project" launcher
            Button(
                onClick = { galleryLauncher.launch("video/* image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
                    .testTag("new_project_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GeoPrimaryContainer,
                    contentColor = GeoOnPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Project icon", modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("New Project", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Recent Drafts section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, contentDescription = "Drafts clock", tint = GeoPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recent Drafts", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text(
                    text = "View All (${drafts.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GeoPrimary,
                    modifier = Modifier.clickable { onNavigateToDrafts() }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (drafts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(GeoSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No drafts found", color = Color.Gray, fontSize = 14.sp)
                        Text(
                            "Tap 'New Project' to start a session",
                            color = Color.DarkGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(drafts.take(5)) { draft ->
                        DraftCard(draft = draft, onClick = {
                            viewModel.loadProject(draft)
                            onNavigateToEditor()
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Tutorial cards
            Text(
                text = "Editing Tips & Tricks",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = GeoSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🔑 Professional Keyframes", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text(
                        "Tap on a clip and position the playhead, then tap the keyframe button at the bottom. Animate scales, positions, and opacities easily!",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = GeoSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🤖 Auto Captions Speech-to-Text", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text(
                        "Save time on adding subtitles. Go to the Text tools, tap 'Auto Captions' and watch our Speech-to-text algorithm transcribing automatically.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun DraftCard(draft: ProjectEntity, onClick: () -> Unit) {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateStr = formatter.format(Date(draft.lastModified))
    
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = GeoSurfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(GeoSurfaceVariant, GeoBorder)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Movie, contentDescription = "Draft icon", tint = GeoPrimary, modifier = Modifier.size(32.dp))
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    val durationSeconds = draft.duration / 1000f
                    Text(
                        text = String.format(Locale.US, "%.1fs", durationSeconds),
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = draft.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
