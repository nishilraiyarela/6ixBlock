package com.sixblock.app.ui.map

import android.Manifest
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.sixblock.app.R
import com.sixblock.app.core.util.TorontoDefaults
import com.sixblock.app.databinding.FragmentMapBinding
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.ui.common.sixBlockFactory
import com.sixblock.app.ui.feed.FeedViewModel
import com.sixblock.app.ui.main.MainActivity

class MapFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FeedViewModel by viewModels { sixBlockFactory }
    private var map: GoogleMap? = null
    private var latestPosts: List<CommunityPost> = emptyList()
    private var selectedFilter: MapFilter = MapFilter.All
    private lateinit var filterButtons: List<MaterialButton>
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            refreshForSelectedFilter(forceFreshLocation = true)
        } else {
            binding.mapStatusText.text = "Allow location access to use Near You."
            selectedFilter = MapFilter.All
            styleFilterButtons()
            refreshForSelectedFilter()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (childFragmentManager.findFragmentById(R.id.googleMap) as SupportMapFragment).getMapAsync(this)
        setupFilterRail()
        viewModel.postsState.observe(viewLifecycleOwner) { state ->
            latestPosts = state.data.orEmpty()
            binding.mapStatusText.text = statusText(state.errorMessage, state.emptyMessage)
            renderMarkers()
        }
        refreshForSelectedFilter()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        googleMap.uiSettings.isZoomControlsEnabled = true
        applyMapTheme(googleMap)
        val center = LatLng(TorontoDefaults.center.latitude, TorontoDefaults.center.longitude)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 12f))
        renderMarkers()
    }

    override fun onResume() {
        super.onResume()
        map?.let(::applyMapTheme)
    }

    private fun renderMarkers() {
        val googleMap = map ?: return
        googleMap.clear()
        latestPosts.filter { it.approximateArea.isNotBlank() }.forEach { post ->
            googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(post.location.latitude, post.location.longitude))
                    .title(post.title)
                    .snippet("${post.category.label} - ${post.approximateArea}")
                    .icon(categoryMarker(post.category))
            )?.tag = post.id
        }
        googleMap.setOnInfoWindowClickListener { marker ->
            val postId = marker.tag as? String ?: return@setOnInfoWindowClickListener
            (requireActivity() as MainActivity).openDetail(postId)
        }
        moveCameraToMarkers(googleMap)
    }

    private fun moveCameraToMarkers(googleMap: GoogleMap) {
        if (latestPosts.isEmpty()) return
        binding.root.post {
            if (latestPosts.size == 1) {
                val post = latestPosts.first()
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(post.location.latitude, post.location.longitude), 13f)
                )
                return@post
            }
            val bounds = LatLngBounds.builder()
            latestPosts.forEach { post ->
                bounds.include(LatLng(post.location.latitude, post.location.longitude))
            }
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 96))
        }
    }

    private fun setupFilterRail() {
        filterButtons = mapFilters.map { filter ->
            MaterialButton(requireContext()).apply {
                id = View.generateViewId()
                text = filter.label
                icon = AppCompatResources.getDrawable(requireContext(), filter.iconRes)
                setOnClickListener {
                    selectFilter(filter)
                }
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    46.dp()
                ).apply {
                    marginEnd = 10.dp()
                }
                minHeight = 46.dp()
                minWidth = 76.dp()
                minimumHeight = 46.dp()
                minimumWidth = 76.dp()
                cornerRadius = 10.dp()
                insetTop = 0
                insetBottom = 0
                iconPadding = 8.dp()
                isAllCaps = false
                strokeWidth = 1.dp()
            }
        }
        binding.mapFilterMenu.removeAllViews()
        filterButtons.forEach { binding.mapFilterMenu.addView(it) }
        styleFilterButtons()
    }

    private fun selectFilter(filter: MapFilter) {
        if (selectedFilter == filter) return
        if (filter == MapFilter.NearYou && !hasLocationPermission()) {
            selectedFilter = filter
            styleFilterButtons()
            binding.mapStatusText.text = "Allow location access to show posts within 10 km."
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }
        selectedFilter = filter
        styleFilterButtons()
        smoothScrollFilterIntoView(filter)
        refreshForSelectedFilter(forceFreshLocation = filter == MapFilter.NearYou)
    }

    private fun refreshForSelectedFilter(forceFreshLocation: Boolean = false) {
        viewModel.refresh(
            category = selectedFilter.category,
            radius = if (selectedFilter == MapFilter.NearYou) NEAR_YOU_RADIUS_KM else MAP_RADIUS_KM,
            forceFreshLocation = forceFreshLocation
        )
    }

    private fun hasLocationPermission(): Boolean {
        val fine = checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun styleFilterButtons() {
        if (!::filterButtons.isInitialized) return
        val selectedText = ContextCompat.getColor(requireContext(), R.color.feed_chip_selected_text)
        val defaultText = ContextCompat.getColor(requireContext(), R.color.feed_muted)
        val selectedBackground = ContextCompat.getColor(requireContext(), R.color.feed_chip_selected)
        val defaultBackground = ContextCompat.getColor(requireContext(), android.R.color.transparent)
        val stroke = ContextCompat.getColor(requireContext(), R.color.feed_line)
        filterButtons.forEachIndexed { index, button ->
            val selected = mapFilters[index] == selectedFilter
            button.setTextColor(if (selected) selectedText else defaultText)
            button.typeface = Typeface.DEFAULT_BOLD
            button.backgroundTintList = ColorStateList.valueOf(if (selected) selectedBackground else defaultBackground)
            button.strokeColor = ColorStateList.valueOf(stroke)
            button.iconTint = ColorStateList.valueOf(if (selected) selectedText else defaultText)
            button.animate()
                .scaleX(if (selected) 1.04f else 1f)
                .scaleY(if (selected) 1.04f else 1f)
                .setDuration(160L)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    private fun smoothScrollFilterIntoView(filter: MapFilter) {
        val index = mapFilters.indexOf(filter)
        val button = filterButtons.getOrNull(index) ?: return
        val targetScroll = (button.left - binding.mapFilterRail.width / 2 + button.width / 2).coerceAtLeast(0)
        binding.mapFilterRail.smoothScrollTo(targetScroll, 0)
    }

    private fun statusText(errorMessage: String?, emptyMessage: String?): String {
        errorMessage?.let { return it }
        if (latestPosts.isEmpty()) {
            return when (selectedFilter) {
                MapFilter.NearYou -> "No nearby posts found within 10 km."
                MapFilter.All -> emptyMessage ?: "No map posts found yet."
                else -> "No ${selectedFilter.label.lowercase()} posts found on the map."
            }
        }
        return when (selectedFilter) {
            MapFilter.NearYou -> "${latestPosts.size} posts within 10 km"
            MapFilter.All -> "${latestPosts.size} pins on the map"
            else -> "${latestPosts.size} ${selectedFilter.label.lowercase()} pins"
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun applyMapTheme(googleMap: GoogleMap) {
        val nightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.map_style_dark))
        } else {
            googleMap.setMapStyle(null)
        }
    }

    private fun categoryMarker(category: PostCategory): BitmapDescriptor {
        val size = 116
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val color = category.color()

        val pin = Path().apply {
            moveTo(58f, 108f)
            cubicTo(48f, 90f, 20f, 69f, 20f, 42f)
            cubicTo(20f, 20f, 36f, 8f, 58f, 8f)
            cubicTo(80f, 8f, 96f, 20f, 96f, 42f)
            cubicTo(96f, 69f, 68f, 90f, 58f, 108f)
            close()
        }

        paint.color = Color.argb(50, 0, 0, 0)
        canvas.drawCircle(58f, 110f, 17f, paint)
        paint.color = color
        canvas.drawPath(pin, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.WHITE
        canvas.drawPath(pin, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawCircle(58f, 42f, 29f, paint)

        AppCompatResources.getDrawable(requireContext(), category.iconRes())?.let { drawable ->
            val icon = DrawableCompat.wrap(drawable.mutate())
            DrawableCompat.setTint(icon, color)
            icon.setBounds(38, 22, 78, 62)
            icon.draw(canvas)
        }

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun PostCategory.color(): Int = when (this) {
        PostCategory.LOCAL_EVENT -> Color.rgb(47, 128, 237)
        PostCategory.HELP_REQUEST -> Color.rgb(111, 66, 193)
        PostCategory.FREE_STUFF -> Color.rgb(0, 137, 95)
        PostCategory.SAFETY_ALERT -> Color.rgb(229, 57, 53)
        PostCategory.LOST_PET -> Color.rgb(121, 85, 72)
        PostCategory.RECOMMENDATION -> Color.rgb(38, 166, 154)
    }

    private fun PostCategory.iconRes(): Int = when (this) {
        PostCategory.LOCAL_EVENT -> R.drawable.ic_category_event_20
        PostCategory.HELP_REQUEST -> R.drawable.ic_category_help_20
        PostCategory.FREE_STUFF -> R.drawable.ic_category_free_20
        PostCategory.SAFETY_ALERT -> R.drawable.ic_category_alert_20
        PostCategory.LOST_PET -> R.drawable.ic_category_pet_20
        PostCategory.RECOMMENDATION -> R.drawable.ic_category_recommendation_20
    }

    private companion object {
        const val MAP_RADIUS_KM = 50
        const val NEAR_YOU_RADIUS_KM = 10
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private sealed class MapFilter(
        val label: String,
        val iconRes: Int,
        val category: PostCategory? = null
    ) {
        data object All : MapFilter("All", R.drawable.ic_star_24)
        data object NearYou : MapFilter("Near You", R.drawable.ic_location_24)
        data object Events : MapFilter("Events", R.drawable.ic_category_event_20, PostCategory.LOCAL_EVENT)
        data object Help : MapFilter("Help", R.drawable.ic_category_help_20, PostCategory.HELP_REQUEST)
        data object FreeStuff : MapFilter("Free Stuff", R.drawable.ic_category_free_20, PostCategory.FREE_STUFF)
        data object Safety : MapFilter("Safety", R.drawable.ic_category_alert_20, PostCategory.SAFETY_ALERT)
        data object LostPets : MapFilter("Lost Pets", R.drawable.ic_category_pet_20, PostCategory.LOST_PET)
    }

    private val mapFilters = listOf(
        MapFilter.All,
        MapFilter.NearYou,
        MapFilter.Events,
        MapFilter.Help,
        MapFilter.FreeStuff,
        MapFilter.Safety,
        MapFilter.LostPets
    )
}
