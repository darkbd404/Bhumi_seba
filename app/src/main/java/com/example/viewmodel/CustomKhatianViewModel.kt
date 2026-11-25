package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppPreferences
import com.example.engine.KhatianSheetExporter
import com.example.engine.MouzaPdfEngine
import com.example.model.CustomKhatianDocument
import com.example.model.KhatianDagEntry
import com.example.model.KhatianOwnerEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class CustomKhatianUiState(
    val document: CustomKhatianDocument = CustomKhatianDocument(
        id = "khatian_draft_${System.currentTimeMillis()}"
    ),
    val savedKhatians: List<CustomKhatianDocument> = emptyList(),
    val referenceBitmap: Bitmap? = null,
    val referencePageCount: Int = 0,
    val referenceCurrentPage: Int = 0,
    val isLoadingReference: Boolean = false,
    val isExporting: Boolean = false,
    val statusMessage: String? = null
)

class CustomKhatianViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefs = AppPreferences(appContext)
    private var activeRefFile: File? = null
    private var activeRefIsPdf: Boolean = true

    private val _uiState = MutableStateFlow(CustomKhatianUiState())
    val uiState: StateFlow<CustomKhatianUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefs.customKhatiansFlow.collect { list ->
                _uiState.update { it.copy(savedKhatians = list) }
            }
        }
    }

    fun updateHeaderFields(
        khatianType: String? = null,
        division: String? = null,
        district: String? = null,
        upazila: String? = null,
        mouzaName: String? = null,
        jlNumber: String? = null,
        khatianNumber: String? = null,
        resaNumber: String? = null,
        tauziNumber: String? = null,
        totalKhajna: String? = null,
        notes: String? = null
    ) {
        _uiState.update { st ->
            val d = st.document
            st.copy(
                document = d.copy(
                    khatianType = khatianType ?: d.khatianType,
                    division = division ?: d.division,
                    district = district ?: d.district,
                    upazila = upazila ?: d.upazila,
                    mouzaName = mouzaName ?: d.mouzaName,
                    jlNumber = jlNumber ?: d.jlNumber,
                    khatianNumber = khatianNumber ?: d.khatianNumber,
                    resaNumber = resaNumber ?: d.resaNumber,
                    tauziNumber = tauziNumber ?: d.tauziNumber,
                    totalKhajna = totalKhajna ?: d.totalKhajna,
                    notes = notes ?: d.notes,
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun addOrUpdateOwner(
        editId: String?,
        name: String,
        fatherOrHusband: String,
        address: String,
        shareText: String,
        shareRatio: Double
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        val ratio = shareRatio.coerceIn(0.0, 1.0)
        val entry = KhatianOwnerEntry(
            id = editId ?: "own_${System.currentTimeMillis()}",
            name = cleanName,
            fatherOrHusband = fatherOrHusband.trim(),
            address = address.trim(),
            shareText = shareText.trim().ifBlank { String.format(java.util.Locale.US, "%.3f", ratio) },
            shareRatio = ratio
        )
        _uiState.update { st ->
            val list = st.document.owners
            val updated = if (editId != null && list.any { it.id == editId }) {
                list.map { if (it.id == editId) entry else it }
            } else {
                list + entry
            }
            st.copy(
                document = st.document.copy(owners = updated, updatedAtMillis = System.currentTimeMillis()),
                statusMessage = "মালিকের তথ্য যুক্ত হয়েছে"
            )
        }
    }

    fun removeOwner(ownerId: String) {
        _uiState.update { st ->
            st.copy(
                document = st.document.copy(
                    owners = st.document.owners.filterNot { it.id == ownerId },
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun addOrUpdateDag(
        editId: String?,
        dagNumber: String,
        landClass: String,
        totalDecimal: Double,
        thisKhatianShare: Double,
        explicitThisKhatianDecimal: Double?,
        remarks: String
    ) {
        val cleanDag = dagNumber.trim()
        if (cleanDag.isBlank()) return
        val safeTotal = totalDecimal.coerceAtLeast(0.0)
        val safeShare = thisKhatianShare.coerceIn(0.0, 1.0)
        val computedDecimal = explicitThisKhatianDecimal?.coerceAtLeast(0.0)
            ?: (safeTotal * safeShare)
        val entry = KhatianDagEntry(
            id = editId ?: "dag_${System.currentTimeMillis()}",
            dagNumber = cleanDag,
            landClass = landClass.trim().ifBlank { "নাল" },
            totalDecimal = safeTotal,
            thisKhatianShare = safeShare,
            thisKhatianDecimal = computedDecimal,
            remarks = remarks.trim()
        )
        _uiState.update { st ->
            val list = st.document.dags
            val updated = if (editId != null && list.any { it.id == editId }) {
                list.map { if (it.id == editId) entry else it }
            } else {
                list + entry
            }
            st.copy(
                document = st.document.copy(dags = updated, updatedAtMillis = System.currentTimeMillis()),
                statusMessage = "দাগ নং $cleanDag যুক্ত হয়েছে"
            )
        }
    }

    fun removeDag(dagId: String) {
        _uiState.update { st ->
            st.copy(
                document = st.document.copy(
                    dags = st.document.dags.filterNot { it.id == dagId },
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun totalKhatianDecimal(): Double = _uiState.value.document.dags.sumOf { it.thisKhatianDecimal }

    fun saveCustomKhatian() {
        val doc = _uiState.value.document.copy(
            referenceDocPath = activeRefFile?.takeIf { it.exists() }?.absolutePath ?: _uiState.value.document.referenceDocPath,
            updatedAtMillis = System.currentTimeMillis()
        )
        viewModelScope.launch {
            prefs.saveCustomKhatian(doc)
            _uiState.update {
                it.copy(
                    document = doc,
                    statusMessage = "কাস্টম খতিয়ান সেভ হয়েছে — পরে আবার এডিট বা এক্সপোর্ট করতে পারবেন"
                )
            }
        }
    }

    fun loadCustomKhatianById(id: String) {
        viewModelScope.launch {
            val doc = prefs.getCustomKhatianById(id) ?: return@launch
            loadCustomKhatian(doc)
        }
    }

    fun loadCustomKhatian(doc: CustomKhatianDocument) {
        viewModelScope.launch {
            var bmp: Bitmap? = null
            var pages = 0
            val file = doc.referenceDocPath?.let { File(it) }?.takeIf { it.exists() }
            activeRefFile = file
            activeRefIsPdf = doc.referenceIsPdf
            if (file != null) {
                runCatching {
                    pages = if (doc.referenceIsPdf) {
                        withContext(Dispatchers.IO) {
                            android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                                android.graphics.pdf.PdfRenderer(pfd).use { it.pageCount }
                            }
                        }
                    } else 1
                    bmp = MouzaPdfEngine.renderBaseDocument(appContext, file, doc.referenceIsPdf, 0, ultraHd = false).bitmap
                }
            }
            _uiState.update {
                it.copy(
                    document = doc,
                    referenceBitmap = bmp,
                    referencePageCount = pages,
                    referenceCurrentPage = 0,
                    statusMessage = "খতিয়ান নং ${doc.khatianNumber.ifBlank { "-" }} লোড হয়েছে"
                )
            }
        }
    }

    fun deleteSavedKhatian(id: String) {
        viewModelScope.launch {
            prefs.deleteCustomKhatian(id)
        }
    }

    fun createNewBlankKhatian() {
        activeRefFile = null
        _uiState.update {
            it.copy(
                document = CustomKhatianDocument(id = "khatian_draft_${System.currentTimeMillis()}"),
                referenceBitmap = null,
                referencePageCount = 0,
                referenceCurrentPage = 0,
                statusMessage = "নতুন খতিয়ান ফরম প্রস্তুত"
            )
        }
    }

    fun openReferenceDocumentUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReference = true, statusMessage = "অরিজিনাল খতিয়ান লোড হচ্ছে...") }
            try {
                val displayName = resolveFileName(uri)
                val mime = appContext.contentResolver.getType(uri)
                val info = MouzaPdfEngine.copyDocumentToCache(
                    context = appContext,
                    uri = uri,
                    fileName = displayName,
                    mimeType = mime,
                    storageDirName = "active_work_docs",
                    cachePrefix = "active_ref_khatian_"
                )
                val savedDir = File(appContext.filesDir, "saved_khatian_refs").apply { mkdirs() }
                val dest = withContext(Dispatchers.IO) {
                    val ext = info.tempFile.extension.ifBlank { if (info.isPdf) "pdf" else "png" }
                    val target = File(savedDir, "ref_kh_${System.currentTimeMillis()}.$ext")
                    info.tempFile.copyTo(target, overwrite = true)
                    target
                }
                activeRefFile = dest
                activeRefIsPdf = info.isPdf
                val rendered = MouzaPdfEngine.renderBaseDocument(appContext, dest, info.isPdf, 0, ultraHd = true)
                _uiState.update { st ->
                    st.copy(
                        isLoadingReference = false,
                        referenceBitmap = rendered.bitmap,
                        referencePageCount = info.pageCount,
                        referenceCurrentPage = 0,
                        document = st.document.copy(
                            referenceDocPath = dest.absolutePath,
                            referenceDocName = info.fileName,
                            referenceIsPdf = info.isPdf
                        ),
                        statusMessage = "অরিজিনাল খতিয়ান ওপেন হয়েছে — দেখে দেখে নিচে তথ্য লিখুন"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingReference = false,
                        statusMessage = "ফাইল খোলা যায়নি: ${e.localizedMessage ?: "ত্রুটি"}"
                    )
                }
            }
        }
    }

    fun selectReferencePage(pageIndex: Int) {
        val file = activeRefFile ?: return
        if (!activeRefIsPdf || pageIndex !in 0 until _uiState.value.referencePageCount) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReference = true) }
            runCatching {
                MouzaPdfEngine.renderBaseDocument(appContext, file, true, pageIndex, ultraHd = true)
            }.onSuccess { rendered ->
                _uiState.update {
                    it.copy(
                        isLoadingReference = false,
                        referenceCurrentPage = pageIndex,
                        referenceBitmap = rendered.bitmap
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isLoadingReference = false) }
            }
        }
    }

    fun exportKhatian(asPdf: Boolean) {
        val doc = _uiState.value.document
        if (doc.owners.isEmpty() && doc.dags.isEmpty() && doc.khatianNumber.isBlank()) {
            _uiState.update { it.copy(statusMessage = "আগে খতিয়ান নং, মালিক অথবা দাগ তথ্য লিখুন") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    statusMessage = if (asPdf) "খতিয়ান PDF তৈরি হচ্ছে..." else "খতিয়ান PNG তৈরি হচ্ছে..."
                )
            }
            try {
                val profile = prefs.surveyorProfileFlow.first()
                prefs.saveCustomKhatian(doc)
                val res = if (asPdf) {
                    KhatianSheetExporter.exportCustomKhatianPdf(appContext, doc, profile)
                } else {
                    KhatianSheetExporter.exportCustomKhatianPng(appContext, doc, profile)
                }
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        statusMessage = "সফলভাবে সেভ হয়েছে: ${res.displayPath}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        statusMessage = "সেভ ব্যর্থ: ${e.localizedMessage ?: "ত্রুটি"}"
                    )
                }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    private fun resolveFileName(uri: Uri): String {
        appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                return cursor.getString(idx) ?: "khatian_original.pdf"
            }
        }
        return "khatian_original.pdf"
    }
}
