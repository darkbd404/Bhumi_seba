package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.model.SurveyorProfile
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.BottomSwipeToastBanner
import com.example.ui.components.PcInstallLinkCard
import com.example.ui.components.VintageStampPaperCard
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
    surveyorProfile: SurveyorProfile = SurveyorProfile(),
    onPhotoPicked: (String) -> Unit,
    onSaveSurveyorProfile: (SurveyorProfile) -> Unit = {},
    onNavigateBack: () -> Unit,
    onOpenUrlInApp: (url: String, title: String) -> Unit,
    onOpenDocumentArchiver: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Zero-permission Android Photo Picker so user can customize their profile photo anytime
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

    var visible by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val activePhotoUri = surveyorProfile.photoUri ?: savedPhotoUri

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp),
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
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(320)) + slideInVertically(tween(320)) { it / 6 }
            ) {
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
                        if (!activePhotoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = Uri.parse(activePhotoUri),
                                contentDescription = surveyorProfile.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.img_dev_avatar),
                                contentDescription = surveyorProfile.name,
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
                        text = surveyorProfile.name.ifBlank { "Abdus Salam" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Rounded.Verified,
                        contentDescription = "Verified",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = surveyorProfile.designation.ifBlank { "ডিজিটাল সার্ভেয়ার • ভূমি সেবা ডিজিটাল" },
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("ছবি সেট", maxLines = 1)
                    }
                    Button(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_surveyor_profile_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("PNG/PDF তথ্য এডিট", maxLines = 1)
                    }
                }
            }
            }
        }

        // Icon-Only Contact Actions (Privacy preserved: numbers, email & URLs not displayed as text)
        item {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(380, delayMillis = 80)) + slideInVertically(tween(380, delayMillis = 80)) { it / 6 }
            ) {
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
        }

        // Document Archiver UI (Scrollable Vintage Bangladeshi Legal Deed Parchment Section)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .neuSurface(cornerRadius = 24.dp, elevation = 6.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.HistoryEdu, contentDescription = null, tint = EmeraldPrimary)
                    Text(
                        text = "ঐতিহাসিক দলিল সংরক্ষণাগার (Document Archiver)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "সাফ-কবলা, বণ্টননামা, হেবা, বায়না ও তফসিলসহ বাংলাদেশের ঐতিহাসিক দলিলের টেমপ্লেট পুরাতন স্ট্যাম্প কাগজের শৈলীতে দেখুন ও সংরক্ষণ করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(185.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    VintageStampPaperCard(
                        headerTitle = "নমুনা ঐতিহাসিক দলিল — সাফ-কবলা / বণ্টননামা",
                        stampBadge = "১০০ টাকা নন-জুডিশিয়াল স্ট্যাম্প শৈলী",
                        serialNumber = "ARCH-BD-1962",
                        disclaimerText = "দ্রষ্টব্য: খসড়া ও নমুনা দলিল — সরকারি সত্যায়িত নকল বা নিবন্ধিত দলিল নয়।"
                    ) {
                        Text(
                            text = "মৌজা: পুবাইল, জে.এল. নং: ৮৫, খতিয়ান নং: ৪১২, দাগ নং: ১০৪২।\n" +
                                "১ম পক্ষ (দাতা): হাজী ছমির উদ্দিন • ২য় পক্ষ (গ্রহীতা): মোঃ রফিকুল ইসলাম।\n" +
                                "তফসিল বর্ণনা ও চৌহদ্দি: উত্তরে দাগ ১০৪১, দক্ষিণে ১২ ফুট রাস্তা, পূর্বে দাগ ১০৪৩, পশ্চিমে পুকুর পাড়। পরিমাণ: ২৪.৭৯ শতাংশ।",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Serif,
                            color = Color(0xFF2B1C10)
                        )
                    }
                }
                Button(
                    onClick = onOpenDocumentArchiver,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_doc_archiver_from_about")
                ) {
                    Icon(Icons.Rounded.HistoryEdu, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Document Archiver খুলুন (সব দলিল ও টেমপ্লেট)")
                }
            }
        }

        item {
            PcInstallLinkCard(
                surveyorProfile = surveyorProfile,
                onPreviewPcMode = { pcUrl ->
                    onOpenUrlInApp(pcUrl, "PC ভার্সন — ভূমি সেবা ডিজিটাল")
                },
                onCopySuccess = { msg -> toastMessage = msg }
            )
        }

        item { Spacer(Modifier.height(14.dp)) }
        }

        BottomSwipeToastBanner(
            message = toastMessage,
            onDismiss = { toastMessage = null },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
        )
    }

    if (showEditProfileDialog) {
        var nameInput by remember(surveyorProfile) { mutableStateOf(surveyorProfile.name) }
        var phoneInput by remember(surveyorProfile) { mutableStateOf(surveyorProfile.phone) }
        var emailInput by remember(surveyorProfile) { mutableStateOf(surveyorProfile.email) }
        var desigInput by remember(surveyorProfile) { mutableStateOf(surveyorProfile.designation) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("PNG / PDF শিট বিবরণ এডিট", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "মৌজা নকশা PNG বা PDF সেভ করার সময় নিচে আপনার ছবি, নাম, মোবাইল ও Gmail প্রিন্ট হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("নাম (Name)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("মোবাইল নম্বর (Phone)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Gmail / ইমেইল") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desigInput,
                        onValueChange = { desigInput = it },
                        label = { Text("পদবি / বিবরণ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveSurveyorProfile(
                            surveyorProfile.copy(
                                name = nameInput.trim().ifBlank { "Abdus Salam" },
                                phone = phoneInput.trim().ifBlank { "01620230864" },
                                email = emailInput.trim().ifBlank { "salam230864@gmail.com" },
                                designation = desigInput.trim().ifBlank { "ডিজিটাল সার্ভেয়ার • ভূমি সেবা ডিজিটাল" }
                            )
                        )
                        showEditProfileDialog = false
                        toastMessage = "PNG ও PDF প্রোফাইল আপডেট হয়েছে!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("সংরক্ষণ করুন")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditProfileDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
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
