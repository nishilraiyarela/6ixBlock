package com.sixblock.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.sixblock.app.core.model.Resource
import com.sixblock.app.databinding.FragmentLoginBinding
import com.sixblock.app.ui.common.sixBlockFactory

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels { sixBlockFactory }

    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        runCatching {
            val account = task.getResult(ApiException::class.java)
            val token = account.idToken ?: error("Missing Google ID token.")
            viewModel.signInWithGoogleToken(token)
        }.onFailure {
            showStatus("Google sign-in was cancelled. Please try again.")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.loginButton.setOnClickListener {
            val email = binding.emailInput.text?.toString().orEmpty().trim()
            val password = binding.passwordInput.text?.toString().orEmpty()
            binding.emailLayout.error = null
            binding.passwordLayout.error = null
            when {
                email.isBlank() || "@" !in email -> binding.emailLayout.error = "Please enter a valid email address."
                password.isBlank() -> binding.passwordLayout.error = "Please enter your password."
                else -> viewModel.signIn(email, password)
            }
        }
        binding.googleButton.setOnClickListener { launchGoogleSignIn() }
        binding.createAccountText.setOnClickListener { (requireActivity() as AuthActivity).showSignUp() }

        viewModel.authState.observe(viewLifecycleOwner) { state ->
            binding.loginButton.isEnabled = state !is Resource.Loading
            binding.googleButton.isEnabled = state !is Resource.Loading
            when (state) {
                Resource.Loading -> showStatus("Signing in...")
                is Resource.Success -> (requireActivity() as AuthActivity).openMain()
                is Resource.Error -> showStatus(state.message)
                is Resource.Empty -> showStatus(state.message)
            }
        }
    }

    private fun launchGoogleSignIn() {
        val id = resources.getIdentifier("default_web_client_id", "string", requireContext().packageName)
        if (id == 0) {
            showStatus("Add google-services.json to enable Google sign-in.")
            return
        }
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(id))
            .requestEmail()
            .build()
        googleLauncher.launch(GoogleSignIn.getClient(requireContext(), options).signInIntent)
    }

    private fun showStatus(message: String) {
        binding.authStatusText.text = message
        binding.authStatusText.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
