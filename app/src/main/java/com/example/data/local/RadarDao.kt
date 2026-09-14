package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.RadarEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RadarDao {

    @Query("SELECT * FROM radars ORDER BY id ASC")
    fun getAllRadarsFlow(): Flow<List<RadarEntity>>

    @Query("SELECT * FROM radars ORDER BY id ASC")
    suspend fun getAllRadars(): List<RadarEntity>

    @Query("SELECT * FROM radars ORDER BY id ASC")
    suspend fun getAllRadarsDirect(): List<RadarEntity>

    @Query("SELECT COUNT(*) FROM radars")
    fun getRadarsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM radars")
    suspend fun getRadarsCountSync(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(radars: List<RadarEntity>)

    @Query("DELETE FROM radars")
    suspend fun clearAll()

    @Query("SELECT * FROM radars WHERE name LIKE '%' || :query || '%' OR road LIKE '%' || :query || '%' OR maxSpeed LIKE '%' || :query || '%'")
    fun searchRadars(query: String): Flow<List<RadarEntity>>

    @Query("SELECT * FROM radars WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLon AND :maxLon")
    suspend fun getRadarsInBoundingBox(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): List<RadarEntity>
}
