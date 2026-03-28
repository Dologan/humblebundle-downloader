package com.dologan.humblebrowser.di

import android.content.Context
import androidx.room.Room
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dologan.humblebrowser.data.api.AuthInterceptor
import com.dologan.humblebrowser.data.api.HumbleBundleApi
import com.dologan.humblebrowser.data.db.HumbleDatabase
import com.dologan.humblebrowser.data.db.dao.BundleDao
import com.dologan.humblebrowser.data.db.dao.ExclusionRuleDao
import com.dologan.humblebrowser.data.db.dao.FileDao
import com.dologan.humblebrowser.data.db.dao.HiddenPathDao
import com.dologan.humblebrowser.data.db.dao.ProductDao
import com.dologan.humblebrowser.data.prefs.AuthPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAuthPreferences(@ApplicationContext context: Context): AuthPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val prefs = EncryptedSharedPreferences.create(
            context,
            "humble_auth",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
        return AuthPreferences(prefs)
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(authPreferences: AuthPreferences): AuthInterceptor {
        return AuthInterceptor(authPreferences)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://www.humblebundle.com/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideHumbleBundleApi(retrofit: Retrofit): HumbleBundleApi {
        return retrofit.create(HumbleBundleApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HumbleDatabase {
        return Room.databaseBuilder(
            context,
            HumbleDatabase::class.java,
            "humble_browser.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideBundleDao(db: HumbleDatabase): BundleDao = db.bundleDao()

    @Provides
    fun provideProductDao(db: HumbleDatabase): ProductDao = db.productDao()

    @Provides
    fun provideFileDao(db: HumbleDatabase): FileDao = db.fileDao()

    @Provides
    fun provideHiddenPathDao(db: HumbleDatabase): HiddenPathDao = db.hiddenPathDao()

    @Provides
    fun provideExclusionRuleDao(db: HumbleDatabase): ExclusionRuleDao = db.exclusionRuleDao()
}
