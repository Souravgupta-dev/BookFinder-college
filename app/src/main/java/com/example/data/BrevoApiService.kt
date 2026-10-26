package com.example.data

import android.util.Log
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class BrevoSender(val name: String, val email: String)

@JsonClass(generateAdapter = true)
data class BrevoRecipient(val email: String, val name: String? = null)

@JsonClass(generateAdapter = true)
data class BrevoEmailRequest(
  val sender: BrevoSender,
  val to: List<BrevoRecipient>,
  val subject: String,
  val htmlContent: String
)

interface BrevoApiService {
  @Headers("Accept: application/json", "Content-Type: application/json")
  @POST("v3/smtp/email")
  suspend fun sendEmail(
    @Header("api-key") apiKey: String,
    @Body request: BrevoEmailRequest
  ): Response<ResponseBody>

  companion object {
    private const val BASE_URL = "https://api.brevo.com/"

    fun create(): BrevoApiService {
      val logging = HttpLoggingInterceptor { message ->
        Log.d("BrevoApiHttp", message)
      }.apply {
        level = HttpLoggingInterceptor.Level.BODY
      }
      val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

      val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()
      return retrofit.create(BrevoApiService::class.java)
    }
  }
}
