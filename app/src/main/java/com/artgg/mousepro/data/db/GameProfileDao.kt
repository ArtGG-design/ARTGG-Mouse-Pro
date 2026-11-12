package com.artgg.mousepro.data.db

import androidx.room.*
import com.artgg.mousepro.data.model.GameProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface GameProfileDao {

    @Query("SELECT * FROM game_profiles ORDER BY updatedAt DESC")
    fun getAllProfiles(): Flow<List<GameProfile>>

    @Query("SELECT * FROM game_profiles WHERE id = :id")
    suspend fun getProfileById(id: String): GameProfile?

    @Query("SELECT * FROM game_profiles WHERE packageName = :packageName LIMIT 1")
    suspend fun getProfileByPackage(packageName: String): GameProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: GameProfile)

    @Delete
    suspend fun delete(profile: GameProfile)

    @Query("DELETE FROM game_profiles WHERE id = :id")
    suspend fun deleteById(id: String)
}