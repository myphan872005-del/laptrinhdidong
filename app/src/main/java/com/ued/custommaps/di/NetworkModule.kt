package com.ued.custommaps.di

import com.ued.custommaps.data.SessionManager
import com.ued.custommaps.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.inject.Named
import android.util.Log
import com.ued.custommaps.network.NetworkConfig

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // 🚀 ĐÂY CHỈ LÀ LINK PHÒNG HỜ (FALLBACK)
    // Nếu sếp chưa nhập link mới vào App, nó sẽ dùng cái này.
    private const val DEFAULT_BASE_URL = NetworkConfig.BASE_URL

    @Provides
    @Singleton
    @Named("AuthInterceptor")
    fun provideAuthInterceptor(sessionManager: SessionManager): Interceptor {
        return Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            val token = runBlocking { sessionManager.userSession.first()?.token }
            if (!token.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }
    }

    // 🚀 BÍ QUYẾT Ở ĐÂY: TRẠM BẺ LÁI URL ĐỘNG
    @Provides
    @Singleton
    @Named("DynamicUrlInterceptor")
    fun provideDynamicUrlInterceptor(sessionManager: SessionManager): Interceptor {
        return Interceptor { chain ->
            var request = chain.request()

            // Lấy link mới nhất mà sếp nhập trong App
            val customUrl = runBlocking { sessionManager.serverUrlFlow.first() }

            if (!customUrl.isNullOrEmpty()) {
                try {
                    val newBaseUrl = customUrl.toHttpUrlOrNull()
                    if (newBaseUrl != null) {
                        // Cắt ghép: Giữ nguyên đường dẫn con (Vd: /api/login), chỉ đổi phần đầu (Host)
                        val newUrl = request.url.newBuilder()
                            .scheme(newBaseUrl.scheme)
                            .host(newBaseUrl.host)
                            .port(newBaseUrl.port)
                            .build()

                        request = request.newBuilder().url(newUrl).build()
                    }
                } catch (e: Exception) {
                    // Nếu link nhập sai định dạng, cứ kệ nó chạy link mặc định
                }
            }
            chain.proceed(request)
        }
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return loggingInterceptor
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @Named("AuthInterceptor") authInterceptor: Interceptor,
        @Named("DynamicUrlInterceptor") dynamicUrlInterceptor: Interceptor,
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(dynamicUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            // 🚀 QUAN TRỌNG: Thêm Header vượt rào Ngrok cho TOÀN BỘ API
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(DEFAULT_BASE_URL) // Vẫn phải gán 1 cái mặc định để Retrofit không chửi
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}