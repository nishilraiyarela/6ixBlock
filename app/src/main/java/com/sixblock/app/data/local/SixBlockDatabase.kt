package com.sixblock.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sixblock.app.data.local.dao.CommentDao
import com.sixblock.app.data.local.dao.DraftDao
import com.sixblock.app.data.local.dao.HiddenContentDao
import com.sixblock.app.data.local.dao.PostDao
import com.sixblock.app.data.local.entity.CachedCommentEntity
import com.sixblock.app.data.local.entity.CachedPostEntity
import com.sixblock.app.data.local.entity.HiddenContentEntity
import com.sixblock.app.data.local.entity.PostDraftEntity

@Database(
    entities = [
        CachedPostEntity::class,
        CachedCommentEntity::class,
        PostDraftEntity::class,
        HiddenContentEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SixBlockDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun draftDao(): DraftDao
    abstract fun hiddenContentDao(): HiddenContentDao
}
