package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Domain
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.LandServiceCategory
import com.example.model.LandServicePortal
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.LocalNeuPalette
import com.example.ui.theme.SurveyGold
import com.example.ui.theme.TealSecondary
import com.example.ui.theme.neuClickable
import com.example.ui.theme.neuSurface

private val allBangladeshLandPortals = listOf(
    LandServicePortal(
        id = "dlrms",
        titleBn = "DLRMS ডিজিটাল ভূমি রেকর্ড ও মৌজা ম্যাপ",
        subtitleBn = "খতিয়ান অনুসন্ধান, মৌজা ম্যাপ যাচাই ও অনলাইন আবেদন",
        url = "https://dlrmslandgovbd.com",
        category = LandServiceCategory.OWN_PORTALS,
        badgeText = "Featured",
        isFeatured = true
    ),
    LandServicePortal(
        id = "dlrms_mouza",
        titleBn = "মৌজা ম্যাপ (dlrmslandgovbd)",
        subtitleBn = "মৌজা ভিত্তিক ডিজিটাল নকশা ও দাগ অনুসন্ধান",
        url = "https://dlrmslandgovbd.com/mouza-map",
        category = LandServiceCategory.OWN_PORTALS,
        badgeText = "Map"
    ),
    LandServicePortal(
        id = "eporcha",
        titleBn = "E-Porcha Tech (ই-পর্চা ও খতিয়ান)",
        subtitleBn = "সার্টিফাইড খতিয়ান, পর্চা ও মৌজা ম্যাপ সেবা",
        url = "https://eporcha.tech/",
        category = LandServiceCategory.OWN_PORTALS,
        badgeText = "Featured",
        isFeatured = true
    ),
    LandServicePortal(
        id = "eporcha_survey",
        titleBn = "সার্ভে খতিয়ান (eporcha.tech)",
        subtitleBn = "সিএস, এসএ, আরএস ও বিএস সার্ভে খতিয়ান তথ্য",
        url = "https://eporcha.tech/survey-khatian",
        category = LandServiceCategory.OWN_PORTALS,
        badgeText = "Khatian"
    ),
    LandServicePortal(
        id = "eporcha_namjari",
        titleBn = "নামজারী খতিয়ান (eporcha.tech)",
        subtitleBn = "অনলাইন নামজারি ও মিউটেশন খতিয়ান সেবা",
        url = "https://eporcha.tech/namjari-khatian",
        category = LandServiceCategory.OWN_PORTALS,
        badgeText = "Namjari"
    ),
    LandServicePortal(
        id = "eporcha_mouza",
        titleBn = "মৌজা ম্যাপ (eporcha.tech)",
        subtitleBn = "ডিজিটাল মৌজা ম্যাপ ও প্লট তথ্য সহায়তা",
        url = "https://eporcha.tech/mouza-map",
        category = LandServiceCategory.OWN_PORTALS,
        badgeText = "Map"
    ),
    LandServicePortal(
        id = "eporcha_khatian_book",
        titleBn = "খতিয়ান বই (মৌজা ভিত্তিক)",
        subtitleBn = "মৌজা অনুযায়ী সম্পূর্ণ খতিয়ান তালিকা",
        url = "https://eporcha.tech/mouza-vittik-khatian",
        category = LandServiceCategory.OWN_PORTALS,
        badgeText = "Book"
    ),
    LandServicePortal(
        id = "eporcha_dag_book",
        titleBn = "দাগ বই (মৌজা ভিত্তিক)",
        subtitleBn = "মৌজা অনুযায়ী দাগ নাম্বার ও জমির বিবরণ",
        url = "https://eporcha.tech/mouza-vittik-dag",
        category = LandServiceCategory.OWN_PORTALS,
        badgeText = "Dag"
    ),
    LandServicePortal(
        id = "dlrms_gov",
        titleBn = "ই-পর্চা / খতিয়ান (DLRMS সরকারি)",
        subtitleBn = "ভূমি রেকর্ড ও জরিপ অধিদপ্তরের অফিসিয়াল পোর্টাল",
        url = "https://dlrms.land.gov.bd",
        category = LandServiceCategory.GOVT_LAND,
        badgeText = "Official"
    ),
    LandServicePortal(
        id = "settlement_khatian",
        titleBn = "খতিয়ান অনুসন্ধান (Settlement)",
        subtitleBn = "অনলাইন জরিপ ও সেটেলমেন্ট খতিয়ান যাচাই",
        url = "https://settlement.gov.bd/Khatian/",
        category = LandServiceCategory.GOVT_LAND,
        badgeText = "Official"
    ),
    LandServicePortal(
        id = "minland",
        titleBn = "ভূমি মন্ত্রণালয় (Ministry of Land)",
        subtitleBn = "সরকারি সকল ডিজিটাল ভূমি সেবা ও প্রজ্ঞাপন",
        url = "https://minland.gov.bd",
        category = LandServiceCategory.GOVT_LAND,
        badgeText = "Official"
    ),
    LandServicePortal(
        id = "eporcha_gov",
        titleBn = "সরকারি ই-পর্চা পোর্টাল (ePorcha)",
        subtitleBn = "সিএস, এসএ, আরএস ও বিএস খতিয়ান আবেদন",
        url = "https://eporcha.gov.bd",
        category = LandServiceCategory.GOVT_LAND,
        badgeText = "Free"
    ),
    LandServicePortal(
        id = "smart_map",
        titleBn = "স্মার্ট ভূমি নকশা / মৌজা ম্যাপ",
        subtitleBn = "ডিজিটাল মৌজা নকশা ও দাগ অনুসন্ধান",
        url = "https://map.land.gov.bd",
        category = LandServiceCategory.GOVT_LAND,
        badgeText = "Map"
    ),
    LandServicePortal(
        id = "lmap",
        titleBn = "ভূমি ম্যাপ (LMAP)",
        subtitleBn = "ভূমি মন্ত্রণালয়ের ইন্টারেক্টিভ ডিজিটাল ম্যাপ",
        url = "https://lmap.minland.gov.bd",
        category = LandServiceCategory.GOVT_LAND,
        badgeText = "LMAP"
    ),
    LandServicePortal(
        id = "dlrs",
        titleBn = "ভূমি রেকর্ড ও জরিপ অধিদপ্তর (DLRS)",
        subtitleBn = "সরকারি জরিপ ও রেকর্ড সংরক্ষণ অধিদপ্তর",
        url = "https://dlrs.gov.bd",
        category = LandServiceCategory.GOVT_LAND,
        badgeText = "Official"
    ),
    LandServicePortal(
        id = "mutation",
        titleBn = "ই-নামজারি (E-Mutation)",
        subtitleBn = "অনলাইন নামজারি আবেদন, খতিয়ান ও ডিসিআর ফি",
        url = "https://mutation.land.gov.bd",
        category = LandServiceCategory.TAX_DEED,
        badgeText = "Online"
    ),
    LandServicePortal(
        id = "ldtax",
        titleBn = "ভূমি উন্নয়ন কর (অনলাইন খাজনা)",
        subtitleBn = "জমির খাজনা পরিশোধ ও দাখিলা যাচাই",
        url = "https://ldtax.gov.bd",
        category = LandServiceCategory.TAX_DEED,
        badgeText = "Tax"
    ),
    LandServicePortal(
        id = "dolil",
        titleBn = "রেজিস্ট্রেশন ও দলিল ফি (Dolil)",
        subtitleBn = "জমির মৌজা রেট, স্ট্যাম্প ও দলিল খরচ",
        url = "https://dolil.gov.bd",
        category = LandServiceCategory.TAX_DEED,
        badgeText = "Deed"
    ),
    LandServicePortal(
        id = "uttoradhikar",
        titleBn = "উত্তরাধিকার ক্যালকুলেটর (আইন মন্ত্রণালয়)",
        subtitleBn = "সরকারি ওয়ারিশ সম্পত্তি বণ্টন পোর্টাল",
        url = "https://uttoradhikar.gov.bd",
        category = LandServiceCategory.SURVEY_HELP,
        badgeText = "Free"
    ),
    LandServicePortal(
        id = "landpedia",
        titleBn = "স্মার্ট ভূমি পিডিয়া (ভূমি আইন ও নীতি)",
        subtitleBn = "সকল ভূমি আইন, বিধিমালা ও নির্দেশিকা",
        url = "https://landpedia.land.gov.bd",
        category = LandServiceCategory.SURVEY_HELP,
        badgeText = "Law"
    ),
    LandServicePortal(
        id = "land_portal",
        titleBn = "ভূমি সেবা ড্যাশবোর্ড (land.gov.bd)",
        subtitleBn = "নাগরিক কর্নার ও সমন্বিত ভূমি সেবা",
        url = "https://land.gov.bd",
        category = LandServiceCategory.SURVEY_HELP,
        badgeText = "Portal"
    ),
    LandServicePortal(
        id = "grs",
        titleBn = "অভিযোগ প্রতিকার ব্যবস্থা (GRS)",
        subtitleBn = "সরকারি ভূমি সেবা সংক্রান্ত অনলাইন অভিযোগ",
        url = "https://www.grs.gov.bd",
        category = LandServiceCategory.SURVEY_HELP,
        badgeText = "GRS"
    )
)

@Composable
fun ServicesScreen(
    onNavigateBack: () -> Unit,
    onOpenUrlInApp: (url: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) allBangladeshLandPortals
        else allBangladeshLandPortals.filter {
            it.titleBn.contains(searchQuery, ignoreCase = true) ||
                it.subtitleBn.contains(searchQuery, ignoreCase = true) ||
                it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    val grouped = remember(filtered) {
        filtered.groupBy { it.category }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LocalNeuPalette.current.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        val portalCols = when {
            maxWidth >= 900.dp -> 3
            maxWidth >= 600.dp -> 2
            else -> 1
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 1120.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        .testTag("services_back_button")
                        .neuClickable(cornerRadius = 14.dp, elevation = 4.dp, onClick = onNavigateBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "বাংলাদেশ ভূমি সেবা সমূহ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "সব লিংক সরাসরি এই অ্যাপের ভেতরেই খুলবে",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("সেবা খুঁজুন (যেমন: খতিয়ান, নামজারি, খাজনা, dlrms)...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("services_search_input")
            )
        }

        // Helpline 16122 Quick Call Card
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("services_helpline_card")
                    .neuClickable(
                        cornerRadius = 18.dp,
                        elevation = 5.dp,
                        backgroundColor = EmeraldPrimary.copy(alpha = 0.12f),
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:16122")))
                        }
                    )
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = "Call Land Helpline", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ভূমি সেবা হটলাইন কল (১৬১২২)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "যেকোনো ভূমি সমস্যা বা অভিযোগে সরাসরি ২৪/৭ সরকারি হটলাইনে কল করুন",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        grouped.forEach { (category, portals) ->
            item(key = "cat_${category.name}") {
                Text(
                    text = category.titleBn,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            items(portals.chunked(portalCols), key = { row -> row.joinToString("_") { it.id } }) { rowPortals ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowPortals.forEach { portal ->
                        ServicePortalCard(
                            portal = portal,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenUrlInApp(portal.url, portal.titleBn) }
                        )
                    }
                    repeat(portalCols - rowPortals.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
    }
}

@Composable
private fun ServicePortalCard(
    portal: LandServicePortal,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (icon: ImageVector, accent: Color) = when (portal.id) {
        "dlrms", "dlrms_gov" -> Icons.Rounded.Public to EmeraldPrimary
        "eporcha", "eporcha_gov", "eporcha_survey", "eporcha_namjari", "eporcha_khatian_book", "eporcha_dag_book" ->
            Icons.Rounded.Description to TealSecondary
        "settlement_khatian" -> Icons.Rounded.Search to Color(0xFF00897B)
        "smart_map", "lmap", "dlrms_mouza", "eporcha_mouza" -> Icons.Rounded.Map to Color(0xFF1A73E8)
        "mutation" -> Icons.Rounded.Domain to Color(0xFF7B3FE4)
        "ldtax" -> Icons.Rounded.Payments to SurveyGold
        "dolil" -> Icons.Rounded.ReceiptLong to Color(0xFFD81B60)
        "uttoradhikar" -> Icons.Rounded.Balance to Color(0xFF00897B)
        "landpedia" -> Icons.Rounded.Gavel to Color(0xFF5E35B1)
        "grs" -> Icons.Rounded.Call to Color(0xFFD6405A)
        else -> Icons.Rounded.AccountBalance to EmeraldPrimary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("portal_card_${portal.id}")
            .neuClickable(cornerRadius = 18.dp, elevation = 5.dp, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = portal.titleBn, tint = accent, modifier = Modifier.size(24.dp))
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = portal.titleBn,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = portal.badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = portal.subtitleBn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = "Open In App",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
