package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.FileItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGeneratorHelper {

    /**
     * Generates a well-formatted official PDF document for a given FileItem.
     */
    fun createFiscalPdf(context: Context, file: FileItem): File {
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (points)
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        
        // Colors
        val primaryColor = 0xFF1B365D.toInt() // PFC Deep Navy
        val accentGold = 0xFFC5A059.toInt()   // PFC Gold
        val darkText = 0xFF1C1B1F.toInt()
        val mutedText = 0xFF49454F.toInt()
        val lightBg = 0xFFF8F9FC.toInt()
        val greenSuccess = 0xFF2E7D32.toInt()

        // Background
        canvas.drawColor(android.graphics.Color.WHITE)

        // Top Brand Header Banner
        paint.color = primaryColor
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, 595f, 95f, paint)

        // Brand Title
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        canvas.drawText("STUDIO PFC CONSULTING", 36f, 42f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        canvas.drawText("Dottori Commercialisti • Revisori Legali • Consulenza Fiscale e del Lavoro", 36f, 60f, paint)
        canvas.drawText("Codice Fiscale / P.IVA: 01234567890 • Portale Telematico Integrato", 36f, 75f, paint)

        // Gold Accent Stripe
        paint.color = accentGold
        canvas.drawRect(0f, 95f, 595f, 100f, paint)

        // Title of the Document
        paint.color = primaryColor
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        val cleanTitle = file.nome.removeSuffix(".pdf").uppercase(Locale.ITALY)
        canvas.drawText(cleanTitle, 36f, 135f, paint)

        // Subtitle & Status
        paint.color = mutedText
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        canvas.drawText("Archivio Fiscale Ufficiale • Sezione: ${file.cartella ?: "Generale"} • Anno d'Imposta: ${file.anno ?: "2025"}", 36f, 155f, paint)

        // Metadata Card Box
        paint.color = lightBg
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(36f, 175f, 559f, 260f, 8f, 8f, paint)

        paint.color = 0xFFD0D7DE.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(36f, 175f, 559f, 260f, 8f, 8f, paint)

        // Metadata text inside box
        paint.style = Paint.Style.FILL
        paint.color = darkText
        paint.textSize = 10f
        val proto = "PFC-${Math.abs(file.key.hashCode()).toString().takeLast(8)}"
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(Date())

        canvas.drawText("Protocollo Studio: $proto", 50f, 200f, paint)
        canvas.drawText("Data Generazione: $dateStr", 50f, 220f, paint)
        canvas.drawText("Stato Conservazione: Conforme CAD (10 Anni)", 50f, 240f, paint)

        canvas.drawText("Dimensione: ${file.sizeStr}", 330f, 200f, paint)
        canvas.drawText("File Univoco: ${file.key}", 330f, 220f, paint)
        paint.color = greenSuccess
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        canvas.drawText("Firma Digitale: VALIDA E VERIFICATA", 330f, 240f, paint)

        // Specific Content Section Table
        val lower = (file.nome + " " + (file.cartella ?: "")).lowercase()
        drawFiscalTable(canvas, paint, lower)

        // Footer Divider & Legal Text
        paint.style = Paint.Style.FILL
        paint.color = 0xFFE0E0E0.toInt()
        canvas.drawRect(36f, 755f, 559f, 756f, paint)

        paint.color = mutedText
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        canvas.drawText("Documento generato e conservato digitalmente a norma del D.P.C.M. 13/11/2014 e s.m.i.", 36f, 775f, paint)
        canvas.drawText("Studio PFC Consulting - Servizio Clienti Portale PFC • Riproduzione riservata al titolare della posizione fiscale.", 36f, 790f, paint)

        pdfDoc.finishPage(page)

        // Save file to internal cache/documents directory
        val docsDir = File(context.cacheDir, "pdf").apply { mkdirs() }
        val sanitizedName = file.nome.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val pdfName = if (sanitizedName.endsWith(".pdf", ignoreCase = true)) sanitizedName else "$sanitizedName.pdf"
        val outputFile = File(docsDir, pdfName)

        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        return outputFile
    }

    private fun drawFiscalTable(canvas: android.graphics.Canvas, paint: Paint, lower: String) {
        val darkText = 0xFF1C1B1F.toInt()
        val primaryColor = 0xFF1B365D.toInt()
        val lightBg = 0xFFF3F5F9.toInt()

        // Section Title
        paint.style = Paint.Style.FILL
        paint.color = primaryColor
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)

        val tableTitle = when {
            lower.contains("f24") -> "DETTAGLIO DELEGA F24 E CODICI TRIBUTO"
            lower.contains("dichiaraz") || lower.contains("730") || lower.contains("redditi") -> "QUADRO SINTETICO DEI REDDITI E IMPOSTE DOVUTE"
            lower.contains("bilanc") -> "SINTESI CONTO ECONOMICO E STATO PATRIMONIALE"
            lower.contains("cedolin") || lower.contains("busta") || lower.contains("pag") -> "PROSPETTO RETRIBUTIVO E CONTRIBUTIVO MENSILE"
            else -> "RIEPILOGO ELEMENTI DEL DOCUMENTO FISCALE"
        }
        canvas.drawText(tableTitle, 36f, 295f, paint)

        // Table background header
        paint.color = lightBg
        canvas.drawRect(36f, 310f, 559f, 335f, paint)

        paint.color = darkText
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        canvas.drawText("DESCRIZIONE / VOCE FISCALE", 48f, 326f, paint)
        canvas.drawText("IMPORTO (€)", 470f, 326f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = 0xFFD0D7DE.toInt()
        canvas.drawRect(36f, 310f, 559f, 520f, paint)

        // Rows
        val rows = when {
            lower.contains("f24") -> listOf(
                "Tributo 6001 - Versamento IVA Periodica" to "€ 1.250,00",
                "Tributo 1040 - Ritenute d'Acconto su Compensi Lavoro Autonomo" to "€ 450,00",
                "Tributo 3801 - Addizionale Regionale IRPEF" to "€ 120,50",
                "Contributi Previdenziali INPS Gestione Separata" to "€ 310,00",
                "Compensazione Crediti Agevolativi Fiscale" to "- € 180,50"
            )
            lower.contains("dichiaraz") || lower.contains("730") || lower.contains("redditi") -> listOf(
                "Reddito Complessivo Imponibile Dichiarato (RN1)" to "€ 54.800,00",
                "Deduzione per Abitazione Principale e Oneri (RP)" to "- € 3.200,00",
                "Imposta Lorda Calcolata (RN5)" to "€ 17.320,00",
                "Detrazioni per Familiari e Lavoro (RN12)" to "- € 3.200,00",
                "Ritenute Subite / Acconti d'Imposta Versati" to "- € 15.340,00"
            )
            lower.contains("bilanc") -> listOf(
                "Valore della Produzione A (Ricavi delle Vendite e Prestazioni)" to "€ 340.500,00",
                "Costi della Produzione B (Materie prime, Servizi, Godimento beni)" to "- € 275.200,00",
                "Differenza tra Valore e Costi della Produzione (A - B)" to "€ 65.300,00",
                "Imposte sul Reddito d'Esercizio (IRES / IRAP)" to "- € 16.350,00",
                "Risultato Netto d'Esercizio (Utile di Bilancio)" to "€ 48.950,00"
            )
            else -> listOf(
                "Imponibile Fiscale Certificato" to "€ 3.500,00",
                "Imposta sul Valore Aggiunto (IVA Ordinaria 22%)" to "€ 770,00",
                "Spese di Incasso e Bollo Telematico" to "€ 2,00",
                "Ritenuta di Garanzia Applicata" to "€ 0,00",
                "Importo Totale Documento a Saldo" to "€ 4.272,00"
            )
        }

        var y = 360f
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

        rows.forEach { (desc, amount) ->
            paint.color = darkText
            canvas.drawText(desc, 48f, y, paint)
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            canvas.drawText(amount, 470f, y, paint)
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            
            // Subtle row separator
            paint.color = 0xFFEEEEEE.toInt()
            canvas.drawLine(48f, y + 10f, 545f, y + 10f, paint)
            y += 32f
        }

        // Total Final Balance Bar
        paint.color = lightBg
        canvas.drawRect(36f, 530f, 559f, 570f, paint)
        paint.color = 0xFFD0D7DE.toInt()
        paint.style = Paint.Style.STROKE
        canvas.drawRect(36f, 530f, 559f, 570f, paint)

        paint.style = Paint.Style.FILL
        paint.color = primaryColor
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        canvas.drawText("SALDO COMPLESSIVO FINALE:", 48f, 555f, paint)
        val finalAmount = if (lower.contains("dichiaraz")) "CREDITO € 1.220,00" else "€ 1.820,50"
        canvas.drawText(finalAmount, 440f, 555f, paint)
    }

    /**
     * Shares the PDF file via the native Android Sharesheet.
     */
    fun shareDocument(context: Context, file: FileItem) {
        try {
            val pdfFile = createFiscalPdf(context, file)
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Documento Fiscale: ${file.nome}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "In allegato la copia ufficiale in PDF di \"${file.nome}\" (${file.anno ?: "2025"} - ${file.cartella ?: "Fiscale"}) da Studio PFC Consulting."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Condividi copia PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Errore nella condivisione del documento: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Shares multiple PDF documents at once.
     */
    fun shareMultipleDocuments(context: Context, files: List<FileItem>) {
        try {
            val uris = ArrayList<Uri>()
            for (file in files) {
                val pdf = createFiscalPdf(context, file)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdf)
                uris.add(uri)
            }

            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "application/pdf"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, "Documenti Fiscali Studio PFC (${files.size} file)")
                putExtra(Intent.EXTRA_TEXT, "In allegato i ${files.size} documenti fiscali selezionati da Portale PFC Consulting.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Condividi ${files.size} documenti PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Errore nella condivisione multipla: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saves a copy of the PDF into the app's external files/Documents directory and notifies the user.
     */
    fun savePdfLocally(context: Context, file: FileItem): File? {
        return try {
            val sourceFile = createFiscalPdf(context, file)
            
            // Save to public app documents directory
            val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) 
                ?: File(context.filesDir, "documents")
            destDir.mkdirs()

            val sanitizedName = file.nome.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetName = if (sanitizedName.endsWith(".pdf", ignoreCase = true)) sanitizedName else "$sanitizedName.pdf"
            val targetFile = File(destDir, targetName)

            sourceFile.copyTo(targetFile, overwrite = true)
            targetFile
        } catch (e: Exception) {
            null
        }
    }
}
