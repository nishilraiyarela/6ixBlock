package com.sixblock.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sixblock.app.SixBlockApplication
import com.sixblock.app.core.util.AppSettings
import com.sixblock.app.data.remote.messaging.SixBlockReminderReceiver
import com.sixblock.app.databinding.FragmentSettingsBinding
import com.sixblock.app.ui.auth.AuthActivity
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val themeLabels = listOf("As per system", "On", "Off")
    private val themeModes = listOf(AppSettings.THEME_SYSTEM, AppSettings.THEME_ON, AppSettings.THEME_OFF)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.settingsBackButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
        binding.settingsSignOutButton.setOnClickListener {
            (requireActivity().application as SixBlockApplication).container.authRepository.signOut()
            startActivity(Intent(requireContext(), AuthActivity::class.java))
            requireActivity().finish()
        }
        val radius = AppSettings.radiusKm(requireContext())
        binding.radiusSlider.value = radius.toFloat()
        updateRadiusLabel(radius)
        binding.radiusSlider.addOnChangeListener { _, value, _ ->
            val chosen = value.toInt()
            AppSettings.saveRadiusKm(requireContext(), chosen)
            updateRadiusLabel(chosen)
        }
        setupThemeDropdown()
        setupNotificationSwitches()
    }

    private fun setupThemeDropdown() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, themeLabels)
        binding.themeModeDropdown.setAdapter(adapter)
        val selectedMode = AppSettings.themeMode(requireContext())
        binding.themeModeDropdown.setText(themeLabels[themeModes.indexOf(selectedMode).coerceAtLeast(0)], false)
        binding.themeModeDropdown.setOnItemClickListener { _, _, position, _ ->
            AppSettings.saveThemeMode(requireContext(), themeModes[position])
            AppSettings.applyThemeMode(requireContext())
        }
    }

    private fun updateRadiusLabel(radiusKm: Int) {
        binding.radiusValueText.text = "$radiusKm km"
    }

    private fun setupNotificationSwitches() {
        binding.likeNotificationsSwitch.isChecked = AppSettings.likeNotificationsEnabled(requireContext())
        binding.commentNotificationsSwitch.isChecked = AppSettings.commentNotificationsEnabled(requireContext())
        binding.reminderNotificationsSwitch.isChecked = AppSettings.reminderNotificationsEnabled(requireContext())

        binding.likeNotificationsSwitch.setOnCheckedChangeListener { _, enabled ->
            AppSettings.saveLikeNotificationsEnabled(requireContext(), enabled)
            saveRemoteNotificationPreference(AppSettings.NOTIFICATION_LIKES, enabled)
        }
        binding.commentNotificationsSwitch.setOnCheckedChangeListener { _, enabled ->
            AppSettings.saveCommentNotificationsEnabled(requireContext(), enabled)
            saveRemoteNotificationPreference(AppSettings.NOTIFICATION_COMMENTS, enabled)
        }
        binding.reminderNotificationsSwitch.setOnCheckedChangeListener { _, enabled ->
            AppSettings.saveReminderNotificationsEnabled(requireContext(), enabled)
            saveRemoteNotificationPreference(AppSettings.NOTIFICATION_REMINDERS, enabled)
            if (enabled) SixBlockReminderReceiver.schedule(requireContext()) else SixBlockReminderReceiver.cancel(requireContext())
        }
    }

    private fun saveRemoteNotificationPreference(key: String, enabled: Boolean) {
        val repository = (requireActivity().application as SixBlockApplication).container.notificationRepository
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { repository.setNotificationPreference(key, enabled) }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
