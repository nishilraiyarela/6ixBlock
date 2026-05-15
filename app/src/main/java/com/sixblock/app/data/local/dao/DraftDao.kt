package com.sixblock.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sixblock.app.data.local.entity.PostDraftEntity

@Dao
interface DraftDao {
    @Query("SELECT * FROM post_drafts ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestDraft(): PostDraftEntity?

    @Query("SELECT * FROM post_drafts ORDER BY updatedAt DESC")
    suspend fun getDrafts(): List<PostDraftEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(draft: PostDraftEntity)

    @Query("DELETE FROM post_drafts WHERE id = :draftId")
    suspend fun deleteDraft(draftId: String)
}
