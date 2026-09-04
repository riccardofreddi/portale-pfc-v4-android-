package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PfcDatabase
import com.example.data.local.entity.CachedMessaggioEntity
import com.example.data.repository.PfcRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MessaggiFunctionalTest {

    private lateinit var context: Context
    private lateinit var database: PfcDatabase
    private lateinit var repository: PfcRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = PfcDatabase.getInstance(context)
        repository = PfcRepository(context)
    }

    @Test
    fun testInitialMessaggiAreReadableAndSeeded() = runBlocking {
        // Ensure initial messages are generated
        repository.ensureInitialMessaggi()

        val messages = repository.getMessaggiFlow(false).first()
        assertTrue("Devono essere presenti messaggi attivi", messages.isNotEmpty())

        val uploadMsg = messages.find { it.richiedeUpload }
        assertNotNull("Deve esistere un messaggio con richiesta allegato", uploadMsg)

        // Verify message readability (title and body must be non-empty and legible)
        assertTrue("Il titolo del messaggio deve essere leggibile", !uploadMsg!!.titolo.isBlank())
        assertTrue("Il corpo del messaggio deve essere leggibile e descrittivo", uploadMsg.corpo.length > 20)
        assertTrue("Le istruzioni di upload devono essere chiare", !uploadMsg.uploadDescrizione.isNullOrBlank())
    }

    @Test
    fun testUploadReplyAttachmentFunctionality() = runBlocking {
        val testMsgId = "test-msg-${System.currentTimeMillis()}"
        val testEntity = CachedMessaggioEntity(
            id = testMsgId,
            titolo = "Richiesta Documento Test",
            corpo = "Si prega di allegare il documento di identità richiesto.",
            dataInvio = "Oggi",
            letto = false,
            archiviato = false,
            richiedeUpload = true,
            uploadDescrizione = "Inviare file documento.pdf",
            haRisposta = false,
            allegatoNome = null
        )

        database.messaggioDao().insert(testEntity)

        // Create a dummy file to simulate client attachment
        val testFile = File(context.cacheDir, "documento_identita.pdf").apply {
            writeText("Contenuto file di test per lo Studio Commercialista")
        }

        // Execute upload response
        val result = repository.uploadRispostaMessaggio(testMsgId, testFile)
        assertTrue("L'invio dell'allegato deve avere esito positivo", result.isSuccess)

        // Verify database reflects that the client answered and attached the file
        val updatedList = database.messaggioDao().getMessaggi(false).first()
        val updatedMsg = updatedList.find { it.id == testMsgId }

        assertNotNull(updatedMsg)
        assertTrue("Il messaggio deve risultare risposto", updatedMsg!!.haRisposta)
        assertEquals("documento_identita.pdf", updatedMsg.allegatoNome)
    }

    @Test
    fun testToggleReadAndArchiveStatus() = runBlocking {
        val testMsgId = "test-msg-status-${System.currentTimeMillis()}"
        val testEntity = CachedMessaggioEntity(
            id = testMsgId,
            titolo = "Avviso Circolare Studio",
            corpo = "Testo della comunicazione di prova per la lettura.",
            dataInvio = "Oggi",
            letto = false,
            archiviato = false,
            richiedeUpload = false,
            uploadDescrizione = null,
            haRisposta = false,
            allegatoNome = null
        )

        database.messaggioDao().insert(testEntity)

        // Mark as read
        repository.setMessaggioLetto(testMsgId, true)
        var msg = database.messaggioDao().getMessaggi(false).first().find { it.id == testMsgId }
        assertNotNull(msg)
        assertTrue("Il messaggio deve essere segnato come letto", msg!!.letto)

        // Archive message
        repository.setMessaggioArchiviato(testMsgId, true)
        val activeList = database.messaggioDao().getMessaggi(false).first()
        val archivedList = database.messaggioDao().getMessaggi(true).first()

        assertNull("Il messaggio archiviato non deve comparire negli attivi", activeList.find { it.id == testMsgId })
        assertNotNull("Il messaggio archiviato deve comparire negli archiviati", archivedList.find { it.id == testMsgId })
    }
}
