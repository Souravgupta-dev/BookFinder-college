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
data class ResendEmailRequest(
  val from: String,
  val to: List<String>,
  val subject: String,
  val html: String
)

interface ResendApiService {
  @Headers("Accept: application/json", "Content-Type: application/json")
  @POST("emails")
  suspend fun sendEmail(
    @Header("Authorization") authHeader: String,
    @Body request: ResendEmailRequest
  ): Response<ResponseBody>

  companion object {
    private const val BASE_URL = "https://api.resend.com/"

    fun create(): ResendApiService {
      val logging = HttpLoggingInterceptor { message ->
        Log.d("ResendApiHttp", message)
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
      return retrofit.create(ResendApiService::class.java)
    }
  }
}
