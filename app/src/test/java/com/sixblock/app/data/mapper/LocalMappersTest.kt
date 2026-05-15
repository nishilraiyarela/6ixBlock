package com.sixblock.app.data.mapper

import com.sixblock.app.data.local.entity.CachedPostEntity
import com.sixblock.app.domain.model.PostCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalMappersTest {
    @Test
    fun cachedPost_mapsToDomainCategoryAndLocation() {
        val entity = CachedPostEntity(
            id = "post-1",
            title = "Found keys",
            body = "Set of keys found near the station.",
            categoryId = PostCategory.HELP_REQUEST.id,
            authorId = "user-1",
            authorName = "Nishi",
            latitude = 43.65,
            longitude = -79.38,
            geohash = "dpz83",
            cityKey = "toronto",
            approximateArea = "Toronto area",
            imageUrl = null,
            createdAt = 100,
            updatedAt = 100,
            commentCount = 2,
            reportCount = 0,
            statusId = "active"
        )

        val post = entity.toDomain()

        assertEquals("post-1", post.id)
        assertEquals(PostCategory.HELP_REQUEST, post.category)
        assertEquals(43.65, post.location.latitude, 0.0)
    }
}
