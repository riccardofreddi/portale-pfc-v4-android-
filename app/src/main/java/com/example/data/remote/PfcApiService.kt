package com.example.data.remote

import com.example.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface PfcApiService {

    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): Response<LoginResponse>

    @GET("api/auth/me")
    suspend fun getMe(): Response<MeResponse>

    @POST("api/auth/logout")
    suspend fun logout(): Response<GenericOkResponse>

    @GET("api/documenti/list")
    suspend fun listDocumenti(
        @Query("username") username: String? = null,
        @Query("anno") anno: String? = null,
        @Query("cartella") cartella: String? = null,
        @Query("year") year: String? = null,
        @Query("folder") folder: String? = null
    ): Response<ListResponse>

    @GET("api/preferiti")
    suspend fun getPreferiti(): Response<PreferitiResponse>

    @POST("api/preferiti")
    suspend fun togglePreferito(@Body req: PreferitoToggleRequest): Response<PreferitoToggleResponse>

    @GET("api/ricerca")
    suspend fun search(
        @Query("q") query: String,
        @Query("username") username: String? = null
    ): Response<SearchResponse>

    @GET("api/messaggi")
    suspend fun getMessaggi(@Query("username") username: String? = null): Response<MessaggiRawResponse>

    @PATCH("api/messaggi")
    suspend fun patchMessaggioAction(
        @Query("id") id: String? = null,
        @Query("action") action: String
    ): Response<GenericOkResponse>

    @PATCH("api/messaggi")
    suspend fun patchMessaggio(@Body req: Map<String, @JvmSuppressWildcards Any>): Response<GenericOkResponse>

    @DELETE("api/messaggi")
    suspend fun deleteMessaggio(@Query("id") id: String): Response<GenericOkResponse>

    @Multipart
    @POST("api/risposte/upload")
    suspend fun uploadRisposta(
        @Part("messaggioId") messaggioId: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<GenericOkResponse>

    @GET("api/cassetto/list")
    suspend fun getCassettoList(@Query("username") username: String? = null): Response<CassettoListResponse>

    @Multipart
    @POST("api/cassetto/upload")
    suspend fun uploadCassetto(
        @Part file: MultipartBody.Part,
        @Query("username") username: String? = null
    ): Response<GenericOkResponse>

    @POST("api/cassetto/delete")
    suspend fun deleteCassettoFile(@Body req: KeyRequest): Response<GenericOkResponse>

    @POST("api/cassetto/rename")
    suspend fun renameCassettoFile(@Body req: RenameRequest): Response<GenericOkResponse>

    @GET("api/notifiche")
    suspend fun getNotifiche(): Response<NotificheResponse>

    @POST("api/notifiche")
    suspend fun postNotificheAction(
        @Query("action") action: String,
        @Query("id") id: String? = null,
        @Query("tipi") tipi: String? = null,
        @Query("year") year: String? = null,
        @Query("folder") folder: String? = null
    ): Response<GenericOkResponse>

    @POST("api/notifiche")
    suspend fun updateNotifiche(@Body req: Map<String, @JvmSuppressWildcards Any>): Response<GenericOkResponse>

    // Scadenze
    @GET("api/documenti/scadenza/list")
    suspend fun getScadenzeList(): Response<ScadenzeListResponse>

    @POST("api/documenti/scadenza/paga")
    suspend fun pagaScadenza(@Body req: PagaScadenzaRequest): Response<GenericOkResponse>

    @GET("api/audit/me")
    suspend fun getAuditLogs(
        @Query("limit") limit: Int = 100,
        @Query("cursor") cursor: String? = null
    ): Response<AuditResponse>

    @GET("api/push/fcm/status")
    suspend fun getFcmStatus(): Response<FcmStatusResponse>

    @POST("api/push/fcm/test")
    suspend fun testFcm(): Response<FcmTestResponse>

    @POST("api/push/fcm")
    suspend fun registerFcm(@Body req: FcmTokenRequest): Response<GenericOkResponse>

    @DELETE("api/push/fcm")
    suspend fun unregisterFcm(): Response<GenericOkResponse>

    @GET("api/documenti/download")
    @Streaming
    suspend fun downloadDocument(@Query("key") key: String): Response<ResponseBody>

    @GET("api/documenti/preview")
    @Streaming
    suspend fun previewDocument(@Query("key") key: String): Response<ResponseBody>

    @POST("api/documenti/zip")
    @Streaming
    suspend fun downloadZip(@Body req: ZipRequest): Response<ResponseBody>
}
