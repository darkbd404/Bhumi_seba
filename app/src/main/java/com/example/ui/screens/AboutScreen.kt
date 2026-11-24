package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.SurveyGold
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuClickable
import com.example.ui.theme.neuSurface

// Encapsulated contact actions so raw numbers/emails/links are never rendered visibly on screen
private object DeveloperContactActions {
    const val PRIMARY_PHONE = "tel:01620230864"
    const val SUPPORT_PHONE = "tel:09696590864"
    const val EMAIL_URI = "mailto:salam230864@gmail.com"
    const val FACEBOOK_URL = "https://www.facebook.com/Salam.864"
    const val MESSENGER_URL = "https://m.me/Salam.864"
}

@Composable
fun AboutScreen(
    savedPhotoUri: String?,
    onPhotoPicked: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onOpenUrlInApp: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Zero-permission Android Photo Picker so Abdus Salam can customize his profile photo anytime
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onPhotoPicked(uri.toString())
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("about_back_button")
                        .neuClickable(cornerRadius = 14.dp, elevation = 4.dp, onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "পরিচিতি ও যোগাযোগ",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Developer Profile Card with Photo & Name (No raw phone/email/URL text shown)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .neuSurface(cornerRadius = 26.dp, elevation = 8.dp)
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(126.dp)
                            .neuSurface(cornerRadius = 63.dp, elevation = 6.dp)
                            .padding(5.dp)
                            .clip(CircleShape)
                    ) {
                        if (!savedPhotoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = Uri.parse(savedPhotoUri),
                                contentDescription = "Abdus Salam",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_dev_avatar),
                                contentDescription = "Abdus Salam",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("change_dev_photo_button")
                            .neuClickable(
                                cornerRadius = 18.dp,
                                elevation = 4.dp,
                                backgroundColor = EmeraldPrimary
                            ) {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Update Profile Photo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Abdus Salam",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = "Verified Developer",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "ডিজিটাল ভূমি সেবা ও মৌজা সার্ভে ডেভেলপার",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "বাংলাদেশের সকল মানুষের জন্য সহজ, নির্ভুল ও আধুনিক ভূমি পরিমাপ এবং সরকারি ডিজিটাল সেবা এক প্ল্যাটফর্মে পৌঁছে দেওয়ার লক্ষ্যে নির্মিত।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("নিজের ছবি সেট করুন")
                }
            }
        }

        // Icon-Only Contact Actions (Privacy preserved: numbers, email & URLs not displayed as text)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .neuSurface(cornerRadius = 24.dp, elevation = 6.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "সরাসরি যোগাযোগ করুন (আইকনে ট্যাপ করুন)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ContactIconTile(
                        label = "সরাসরি কল",
                        icon = Icons.Rounded.Call,
                        accent = EmeraldPrimary,
                        testTag = "contact_call_primary",
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(DeveloperContactActions.PRIMARY_PHONE)))
                        }
                    )
                    ContactIconTile(
                        label = "হেল্পলাইন",
                        icon = Icons.Rounded.SupportAgent,
                        accent = TealSecondary,
                        testTag = "contact_call_support",
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(DeveloperContactActions.SUPPORT_PHONE)))
                        }
                    )
                    ContactIconTile(
                        label = "ইমেইল",
                        icon = Icons.Rounded.Email,
                        accent = SurveyGold,
                        testTag = "contact_email",
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse(DeveloperContactActions.EMAIL_URI)))
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ContactIconTile(
                        label = "Facebook",
                        icon = Icons.Rounded.Public,
                        accent = Color(0xFF1877F2),
                        testTag = "contact_facebook",
                        onClick = {
                            onOpenUrlInApp(DeveloperContactActions.FACEBOOK_URL, "Facebook Profile")
                        }
                    )
                    ContactIconTile(
                        label = "Messenger",
                        icon = Icons.AutoMirrored.Rounded.Chat,
                        accent = Color(0xFF8E24AA),
                        testTag = "contact_messenger",
                        onClick = {
                            onOpenUrlInApp(DeveloperContactActions.MESSENGER_URL, "Messenger Chat")
                        }
                    )
                }
            }
        }

        item { Spacer(Modifier.height(14.dp)) }
    }
}

@Composable
private fun ContactIconTile(
    label: String,
    icon: ImageVector,
    accent: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.width(88.dp)
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .testTag(testTag)
                .neuClickable(
                    cornerRadius = 20.dp,
                    elevation = 6.dp,
                    backgroundColor = accent.copy(alpha = 0.14f),
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}
