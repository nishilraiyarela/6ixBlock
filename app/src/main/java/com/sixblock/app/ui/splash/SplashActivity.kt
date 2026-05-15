package com.sixblock.app.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.sixblock.app.SixBlockApplication
import com.sixblock.app.core.model.Resource
import com.sixblock.app.core.util.AppSettings
import com.sixblock.app.core.util.TorontoDefaults
import com.sixblock.app.databinding.ActivitySplashBinding
import com.sixblock.app.ui.auth.AuthActivity
import com.sixblock.app.ui.main.MainActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, bars.top, 0, bars.bottom)
            insets
        }
        binding.splashLogo.animate()
            .alpha(0.72f)
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(620L)
            .withEndAction {
                binding.splashLogo.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(620L)
                    .start()
            }
            .start()

        lifecycleScope.launch {
            val startTime = SystemClock.elapsedRealtime()
            val container = (application as SixBlockApplication).container
            val user = container.authRepository.currentUser.first()
            if (user != null) {
                warmFeedCache(container)
            }
            val elapsed = SystemClock.elapsedRealtime() - startTime
            delay((MIN_SPLASH_MS - elapsed).coerceAtLeast(0L))
            val destination = if (user == null) {
                AuthActivity::class.java
            } else {
                MainActivity::class.java
            }
            startActivity(Intent(this@SplashActivity, destination).putExtras(intent))
            finish()
        }
    }

    private suspend fun warmFeedCache(container: com.sixblock.app.core.di.AppContainer) {
        withTimeoutOrNull(FEED_WARMUP_TIMEOUT_MS) {
            val origin = when (val location = container.locationRepository.getBestLocation()) {
                is Resource.Success -> location.data
                else -> TorontoDefaults.center
            }
            container.postRepository
                .observeNearbyPosts(origin, AppSettings.radiusKm(this@SplashActivity), category = null)
                .first { resource ->
                    resource is Resource.Empty ||
                        resource is Resource.Error ||
                        (resource is Resource.Success && !resource.fromCache)
                }
        }
    }

    private companion object {
        const val MIN_SPLASH_MS = 2_200L
        const val FEED_WARMUP_TIMEOUT_MS = 4_000L
    }
}
