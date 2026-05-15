package com.sixblock.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sixblock.app.data.local.entity.HiddenContentEntity

@Dao
interface HiddenContentDao {
    @Query("SELECT targetId FROM hidden_content WHERE targetType = :targetType")
    suspend fun hiddenIds(targetType: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun hide(content: HiddenContentEntity)
}
