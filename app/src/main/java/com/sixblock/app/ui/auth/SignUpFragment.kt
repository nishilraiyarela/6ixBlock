package com.sixblock.app.ui.auth

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.sixblock.app.core.model.Resource
import com.sixblock.app.databinding.FragmentSignupBinding
import com.sixblock.app.ui.common.sixBlockFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SignUpFragment : Fragment() {
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels { sixBlockFactory }
    private var selectedBirthday = ""
    private val areaOptions = listOf(
        "Downtown",
        "Scarborough",
        "North York",
        "Etobicoke",
        "York",
        "East York",
        "The Beaches",
        "Leslieville",
        "Liberty Village",
        "Queen West",
        "Danforth",
        "Parkdale",
        "Roncesvalles",
        "Midtown",
        "Other GTA area"
    )
    private val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupDropdowns()
        binding.birthdayInput.inputType = InputType.TYPE_NULL
        binding.birthdayInput.setOnClickListener { showBirthdayPicker() }
        binding.birthdayLayout.setEndIconOnClickListener { showBirthdayPicker() }
        binding.signupButton.setOnClickListener {
            submit()
        }
        binding.loginText.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            binding.signupButton.isEnabled = state !is Resource.Loading
            when (state) {
                Resource.Loading -> showStatus("Creating account...")
                is Resource.Success -> (requireActivity() as AuthActivity).openMain()
                is Resource.Error -> showStatus(state.message)
                is Resource.Empty -> showStatus(state.message)
            }
        }
    }

    private fun setupDropdowns() {
        binding.neighbourhoodInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, areaOptions)
        )
        binding.genderInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, genderOptions)
        )
        binding.neighbourhoodInput.setOnClickListener { binding.neighbourhoodInput.showDropDown() }
        binding.genderInput.setOnClickListener { binding.genderInput.showDropDown() }
    }

    private fun showBirthdayPicker() {
        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointBackward.now())
            .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select birthday")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            selectedBirthday = saveDateFormat().format(Date(selection))
            binding.birthdayInput.setText(displayDateFormat().format(Date(selection)))
            binding.birthdayLayout.error = null
        }
        picker.show(parentFragmentManager, "birthday_picker")
    }

    private fun submit() {
        val name = binding.nameInput.text?.toString().orEmpty().trim()
        val email = binding.emailInput.text?.toString().orEmpty().trim()
        val password = binding.passwordInput.text?.toString().orEmpty()
        val neighbourhood = binding.neighbourhoodInput.text?.toString().orEmpty().trim()
        val gender = binding.genderInput.text?.toString().orEmpty().trim()
        listOf(
            binding.nameLayout,
            binding.emailLayout,
            binding.birthdayLayout,
            binding.neighbourhoodLayout,
            binding.genderLayout,
            binding.passwordLayout
        ).forEach { it.error = null }
        when {
            name.isBlank() -> binding.nameLayout.error = "Please enter your name."
            email.isBlank() || "@" !in email -> binding.emailLayout.error = "Please enter a valid email address."
            selectedBirthday.isBlank() -> binding.birthdayLayout.error = "Please select your birthday."
            neighbourhood.isBlank() -> binding.neighbourhoodLayout.error = "Please select your neighbourhood."
            gender.isBlank() -> binding.genderLayout.error = "Please select your gender."
            password.length < 6 -> binding.passwordLayout.error = "Use at least 6 characters for your password."
            else -> viewModel.signUp(name, email, password, selectedBirthday, neighbourhood, gender)
        }
    }

    private fun saveDateFormat(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd", Locale.CANADA).apply { timeZone = TimeZone.getTimeZone("UTC") }

    private fun displayDateFormat(): SimpleDateFormat =
        SimpleDateFormat("MMM d, yyyy", Locale.CANADA).apply { timeZone = TimeZone.getTimeZone("UTC") }

    private fun showStatus(message: String) {
        binding.authStatusText.text = message
        binding.authStatusText.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
