package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.model.FileItem
import com.example.data.model.ZipRequest
import com.example.data.remote.PfcApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

object DocumentFileManager {

    private const val TAG = "DocumentFileManager"

    private fun getCacheFolder(context: Context): File {
        val folder = File(context.cacheDir, "pfc_docs")
        if (!folder.exists()) folder.mkdirs()
        return folder
    }

    fun getLocalCachedFile(context: Context, key: String, nome: String): File {
        val safeHash = md5(key)
        val ext = nome.substringAfterLast('.', "pdf")
        val cleanName = nome.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        return File(getCacheFolder(context), "${safeHash}_$cleanName")
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Downloads real document from backend or returns cached copy if already valid.
     */
    suspend fun getOrDownloadDocument(
        context: Context,
        fileItem: FileItem,
        apiService: PfcApiService
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetFile = getLocalCachedFile(context, fileItem.key, fileItem.nome)

        // If file already exists and has valid size (> 0 bytes and matches expected size if known)
        if (targetFile.exists() && targetFile.length() > 0L) {
            if (fileItem.size <= 0L || targetFile.length() == fileItem.size) {
                Log.d(TAG, "Using existing cached file for ${fileItem.nome} (${targetFile.length()} bytes)")
                return@withContext Result.success(targetFile)
            }
        }

        try {
            Log.d(TAG, "Fetching real document from backend: ${fileItem.key}")
            val response = apiService.downloadDocument(fileItem.key)
            if (response.isSuccessful && response.body() != null) {
                val tempFile = File(getCacheFolder(context), "${targetFile.name}.tmp")
                response.body()!!.byteStream().use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.exists() && tempFile.length() > 0L) {
                    if (targetFile.exists()) targetFile.delete()
                    tempFile.renameTo(targetFile)
                    Log.d(TAG, "Successfully downloaded and cached: ${fileItem.nome} (${targetFile.length()} bytes)")
                    return@withContext Result.success(targetFile)
                }
            } else {
                Log.w(TAG, "Download failed with code ${response.code()}, trying preview endpoint...")
                val previewRes = apiService.previewDocument(fileItem.key)
                if (previewRes.isSuccessful && previewRes.body() != null) {
                    val tempFile = File(getCacheFolder(context), "${targetFile.name}.tmp")
                    previewRes.body()!!.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (tempFile.exists() && tempFile.length() > 0L) {
                        if (targetFile.exists()) targetFile.delete()
                        tempFile.renameTo(targetFile)
                        Log.d(TAG, "Successfully cached from preview: ${fileItem.nome}")
                        return@withContext Result.success(targetFile)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error downloading document ${fileItem.nome}: ${e.message}")
            if (targetFile.exists() && targetFile.length() > 0L) {
                return@withContext Result.success(targetFile)
            }
            return@withContext Result.failure(e)
        }

        if (targetFile.exists() && targetFile.length() > 0L) {
            return@withContext Result.success(targetFile)
        }

        Result.failure(Exception("Impossibile scaricare il file ${fileItem.nome} dal server"))
    }

    /**
     * Saves a local file into the public user Downloads folder (/Download/PortalePFC/).
     */
    suspend fun saveToPublicDownloads(
        context: Context,
        sourceFile: File,
        displayName: String,
        mimeType: String = "application/pdf"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/PortalePFC")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val uri: Uri? = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)

                    val finalDest = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "PortalePFC/$displayName")
                    return@withContext Result.success(finalDest)
                }
            }

            // Legacy external storage fallback
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val pfcDir = File(downloadsDir, "PortalePFC")
            if (!pfcDir.exists()) pfcDir.mkdirs()

            val destFile = File(pfcDir, displayName)
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), arrayOf(mimeType), null)
            Result.success(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving file to public downloads: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Downloads multiple documents as a single ZIP archive from backend and saves to Downloads.
     */
    suspend fun downloadBatchZip(
        context: Context,
        keys: List<String>,
        zipName: String,
        apiService: PfcApiService
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Requesting ZIP of ${keys.size} files from backend...")
            val response = apiService.downloadZip(ZipRequest(keys = keys, zipName = zipName))
            if (response.isSuccessful && response.body() != null) {
                val tempZip = File(getCacheFolder(context), "temp_$zipName")
                response.body()!!.byteStream().use { input ->
                    FileOutputStream(tempZip).use { output ->
                        input.copyTo(output)
                    }
                }

                if (tempZip.exists() && tempZip.length() > 0L) {
                    val publicSave = saveToPublicDownloads(context, tempZip, zipName, "application/zip")
                    tempZip.delete()
                    return@withContext publicSave
                }
            }
            Result.failure(Exception("Errore generazione archivio ZIP (${response.code()})"))
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading batch ZIP: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Opens real file with external system viewer (Adobe Acrobat, Drive, etc.).
     */
    fun openWithExternalApp(context: Context, file: File, mimeType: String = "application/pdf") {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Apri ${file.name}"))
        } catch (e: Exception) {
            Log.e(TAG, "Cannot launch external viewer: ${e.message}")
        }
    }

    /**
     * Shares real file using Android share sheet.
     */
    fun shareFile(context: Context, file: File, mimeType: String = "application/pdf") {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Condividi ${file.name}"))
        } catch (e: Exception) {
            Log.e(TAG, "Cannot share file: ${e.message}")
        }
    }
}
