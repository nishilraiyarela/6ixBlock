package com.sixblock.app.ui.location

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.sixblock.app.R
import com.sixblock.app.core.util.TorontoDefaults
import com.sixblock.app.databinding.FragmentLocationPickerBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class LocationPickerFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentLocationPickerBinding? = null
    private val binding get() = _binding!!
    private var map: GoogleMap? = null
    private var areaCircle: Circle? = null
    private var areaMarker: Marker? = null
    private var selectedAddressLabel: String = ""
    private lateinit var addressAdapter: ArrayAdapter<String>
    private val addressSuggestions = mutableListOf<AddressSuggestion>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLocationPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (childFragmentManager.findFragmentById(R.id.locationPickerMap) as SupportMapFragment).getMapAsync(this)
        addressAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, mutableListOf())
        binding.addressResultsList.adapter = addressAdapter
        binding.searchAddressButton.setOnClickListener { searchAddress() }
        binding.addressSearchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchAddress()
                true
            } else {
                false
            }
        }
        binding.addressResultsList.setOnItemClickListener { _, _, position, _ ->
            val suggestion = addressSuggestions[position]
            selectedAddressLabel = suggestion.label
            binding.addressSearchInput.setText(suggestion.label)
            binding.addressResultsList.visibility = View.GONE
            binding.selectedAddressText.text = suggestion.label
            moveMapTo(suggestion.point, 16f)
        }
        binding.useAreaButton.setOnClickListener {
            val center = map?.cameraPosition?.target ?: LatLng(TorontoDefaults.center.latitude, TorontoDefaults.center.longitude)
            setFragmentResult(
                REQUEST_KEY,
                Bundle().apply {
                    putDouble(RESULT_LAT, center.latitude)
                    putDouble(RESULT_LON, center.longitude)
                    putString(RESULT_ADDRESS, selectedAddressLabel)
                }
            )
            parentFragmentManager.popBackStack()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val toronto = LatLng(TorontoDefaults.center.latitude, TorontoDefaults.center.longitude)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(toronto, 13f))
        areaMarker = googleMap.addMarker(MarkerOptions().position(toronto).title("Approximate area"))
        updateAreaCircle(toronto)
        googleMap.setOnCameraIdleListener {
            val center = googleMap.cameraPosition.target
            areaMarker?.position = center
            updateAreaCircle(center)
        }
        googleMap.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                selectedAddressLabel = ""
                binding.selectedAddressText.text = "Map moved. Use this approximate area."
            }
        }
    }

    private fun searchAddress() {
        val query = binding.addressSearchInput.text?.toString().orEmpty().trim()
        if (query.length < 3) {
            Toast.makeText(requireContext(), "Enter an address or postal code", Toast.LENGTH_SHORT).show()
            return
        }
        binding.searchAddressButton.isEnabled = false
        lifecycleScope.launch {
            val results = findAddresses(query)
            binding.searchAddressButton.isEnabled = true
            addressSuggestions.clear()
            addressAdapter.clear()
            if (results.isEmpty()) {
                binding.addressResultsList.visibility = View.GONE
                Toast.makeText(requireContext(), "No address options found", Toast.LENGTH_SHORT).show()
                return@launch
            }
            addressSuggestions.addAll(results)
            addressAdapter.addAll(results.map { it.label })
            binding.addressResultsList.visibility = View.VISIBLE
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun findAddresses(query: String): List<AddressSuggestion> = withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(requireContext(), Locale.CANADA)
            geocoder.getFromLocationName("$query, Toronto, Ontario, Canada", 5).orEmpty()
                .filter { it.hasLatitude() && it.hasLongitude() }
                .map { address ->
                    AddressSuggestion(
                        label = address.fullAddressLabel(),
                        point = LatLng(address.latitude, address.longitude)
                    )
                }
                .distinctBy { it.label }
        }.getOrDefault(emptyList())
    }

    private fun Address.fullAddressLabel(): String {
        getAddressLine(0)?.takeIf { it.isNotBlank() }?.let { return it }
        return listOfNotNull(
            featureName,
            thoroughfare,
            locality,
            adminArea,
            postalCode,
            countryName
        ).filter { it.isNotBlank() }.joinToString(", ")
    }

    private fun moveMapTo(point: LatLng, zoom: Float) {
        areaMarker?.position = point
        updateAreaCircle(point)
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(point, zoom))
    }

    private fun updateAreaCircle(center: LatLng) {
        areaCircle?.remove()
        areaCircle = map?.addCircle(
            CircleOptions()
                .center(center)
                .radius(450.0)
                .strokeColor(0xCC000000.toInt())
                .strokeWidth(3f)
                .fillColor(0x22000000)
        )
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val REQUEST_KEY = "location_picker"
        const val RESULT_LAT = "lat"
        const val RESULT_LON = "lon"
        const val RESULT_ADDRESS = "address"
    }

    private data class AddressSuggestion(
        val label: String,
        val point: LatLng
    )
}
