package com.artgg.mousepro.data.repository

import android.content.Context
import androidx.room.Room
import com.artgg.mousepro.data.db.AppDatabase
import com.artgg.mousepro.data.db.Converters
import com.artgg.mousepro.data.model.GameProfile
import kotlinx.coroutines.flow.Flow

class ProfileRepository(context: Context) {

    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "artgg_mouse_pro.db"
    ).build()

    private val dao = db.gameProfileDao()

    fun getAllProfiles(): Flow<List<GameProfile>> = dao.getAllProfiles()

    suspend fun getProfileById(id: String): GameProfile? = dao.getProfileById(id)

    suspend fun getProfileByPackage(packageName: String): GameProfile? =
        dao.getProfileByPackage(packageName)

    suspend fun saveProfile(profile: GameProfile) {
        dao.insertOrUpdate(profile.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProfile(profile: GameProfile) = dao.delete(profile)

    suspend fun deleteProfileById(id: String) = dao.deleteById(id)
}