package com.sixblock.app.ui.create

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.sixblock.app.R
import com.google.android.material.button.MaterialButton
import com.sixblock.app.databinding.FragmentCreatePostBinding
import com.sixblock.app.domain.model.CommunityPost
import com.sixblock.app.domain.model.GeoPoint
import com.sixblock.app.domain.model.PostCategory
import com.sixblock.app.domain.model.PostDraft
import com.sixblock.app.ui.common.sixBlockFactory
import com.sixblock.app.ui.location.LocationPickerFragment
import com.sixblock.app.ui.main.MainActivity

class CreatePostFragment : Fragment() {
    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CreatePostViewModel by viewModels { sixBlockFactory }
    private var editingPost: CommunityPost? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setFragmentResultListener(LocationPickerFragment.REQUEST_KEY) { _, bundle ->
            viewModel.setLocation(
                GeoPoint(
                    latitude = bundle.getDouble(LocationPickerFragment.RESULT_LAT),
                    longitude = bundle.getDouble(LocationPickerFragment.RESULT_LON)
                ),
                bundle.getString(LocationPickerFragment.RESULT_ADDRESS)
            )
            updateLocationUi()
        }

        setupCategoryButtons()
        binding.clearFormButton.setOnClickListener {
            if (editingPost != null) {
                exitEditMode()
                showStatus("Edit cancelled")
            } else {
                clearForm()
                showStatus("Form cleared")
            }
        }
        binding.shareLocationSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShareLocation(isChecked)
            updateLocationUi()
        }
        binding.locationButton.setOnClickListener {
            (requireActivity() as MainActivity).openFullScreen(LocationPickerFragment(), "location-picker")
        }
        binding.publishButton.setOnClickListener {
            val post = editingPost
            if (post != null) {
                viewModel.updatePost(
                    postId = post.id,
                    title = binding.titleInput.text?.toString().orEmpty(),
                    body = binding.bodyInput.text?.toString().orEmpty(),
                    category = selectedCategory()
                )
            } else {
                viewModel.publish(
                    title = binding.titleInput.text?.toString().orEmpty(),
                    body = binding.bodyInput.text?.toString().orEmpty(),
                    category = selectedCategory()
                )
            }
        }
        binding.saveDraftButton.setOnClickListener {
            viewModel.saveDraft(
                title = binding.titleInput.text?.toString().orEmpty(),
                body = binding.bodyInput.text?.toString().orEmpty(),
                category = selectedCategory()
            )
        }
        binding.viewDraftsButton.setOnClickListener {
            viewModel.refreshDrafts()
            showDraftsDialog(viewModel.draftsState.value.orEmpty())
        }
        viewModel.draftsState.observe(viewLifecycleOwner) { drafts ->
            binding.viewDraftsButton.text = if (drafts.isEmpty()) "Drafts" else "Drafts (${drafts.size})"
            binding.viewDraftsButton.contentDescription = if (drafts.isEmpty()) {
                "View drafts"
            } else {
                "View drafts, ${drafts.size} saved"
            }
        }
        viewModel.locationLabel.observe(viewLifecycleOwner) { label ->
            if (editingPost == null) binding.locationText.text = label
        }
        viewModel.draftState.observe(viewLifecycleOwner) { draft ->
            if (draft != null && editingPost == null) {
                binding.titleInput.setText(draft.title)
                binding.bodyInput.setText(draft.body)
                binding.shareLocationSwitch.isChecked = draft.approximateArea.isNotBlank()
                updateLocationUi(draft.approximateArea)
            }
        }
        viewModel.createState.observe(viewLifecycleOwner) { state ->
            binding.publishButton.isEnabled = !state.isLoading
            when {
                state.isLoading -> showStatus(if (editingPost != null) "Saving changes..." else "Publishing...")
                state.errorMessage != null -> showStatus(state.errorMessage)
                state.data == "draft_saved" -> showStatus("Draft saved")
                state.data == "post_updated" -> {
                    showStatus("Post updated")
                    exitEditMode()
                    (requireActivity() as MainActivity).openFeed()
                }
                state.data != null -> {
                    showStatus("Post published")
                    clearForm()
                }
            }
        }
        editingPost?.let { bindEditPost(it) }
    }

    private fun selectedCategory(): PostCategory = when (binding.createCategoryGroup.checkedButtonId) {
        R.id.createEvent -> PostCategory.LOCAL_EVENT
        R.id.createFree -> PostCategory.FREE_STUFF
        R.id.createHelp -> PostCategory.HELP_REQUEST
        R.id.createAlert -> PostCategory.SAFETY_ALERT
        else -> PostCategory.LOST_PET
    }

    fun startEditing(post: CommunityPost) {
        editingPost = post
        if (_binding != null) bindEditPost(post)
    }

    private fun setupCategoryButtons() {
        val buttons = listOf(
            binding.createLostPet,
            binding.createEvent,
            binding.createFree,
            binding.createHelp,
            binding.createAlert
        )
        binding.createCategoryGroup.addOnButtonCheckedListener { _, _, _ -> styleCategoryButtons(buttons) }
        styleCategoryButtons(buttons)
    }

    private fun styleCategoryButtons(buttons: List<MaterialButton>) {
        val selectedBackground = ContextCompat.getColor(requireContext(), R.color.charcoal)
        val selectedText = ContextCompat.getColor(requireContext(), R.color.surface)
        val defaultBackground = ContextCompat.getColor(requireContext(), R.color.surface)
        val defaultText = ContextCompat.getColor(requireContext(), R.color.charcoal)
        buttons.forEach { button ->
            val selected = button.id == binding.createCategoryGroup.checkedButtonId
            button.backgroundTintList = ColorStateList.valueOf(if (selected) selectedBackground else defaultBackground)
            button.setTextColor(if (selected) selectedText else defaultText)
            button.strokeColor = ColorStateList.valueOf(defaultText)
        }
    }

    private fun showDraftsDialog(drafts: List<PostDraft>) {
        if (drafts.isEmpty()) {
            showStatus("No drafts yet")
            return
        }
        val labels = drafts.map { draft ->
            draft.title.ifBlank { "Untitled draft" } + " - " + draft.category.label
        }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Drafts")
            .setItems(labels) { _, index ->
                val draft = drafts[index]
                binding.titleInput.setText(draft.title)
                binding.bodyInput.setText(draft.body)
                binding.locationText.text = draft.approximateArea
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showStatus(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun bindEditPost(post: CommunityPost) {
        binding.createTitleText.text = "Edit post"
        binding.createSubtitleText.text = "Update the details neighbours will see."
        binding.publishButton.text = "Save changes"
        binding.saveDraftButton.visibility = View.GONE
        binding.viewDraftsButton.visibility = View.GONE
        binding.clearFormButton.text = "Cancel edit"
        binding.titleInput.setText(post.title)
        binding.titleInput.setSelection(binding.titleInput.text?.length ?: 0)
        binding.bodyInput.setText(post.body)
        binding.createCategoryGroup.check(post.category.buttonId())
        binding.shareLocationSwitch.isChecked = post.approximateArea.isNotBlank()
        binding.locationButton.visibility = View.GONE
        binding.locationText.text = post.approximateArea.ifBlank { "Location not shared" }
        styleCategoryButtons(
            listOf(
                binding.createLostPet,
                binding.createEvent,
                binding.createFree,
                binding.createHelp,
                binding.createAlert
            )
        )
    }

    private fun exitEditMode() {
        editingPost = null
        binding.createTitleText.text = "Post to the block"
        binding.createSubtitleText.text = "Share only what neighbours need to know."
        binding.publishButton.text = "Publish post"
        binding.saveDraftButton.visibility = View.VISIBLE
        binding.viewDraftsButton.visibility = View.VISIBLE
        binding.clearFormButton.text = "Clear"
        clearForm()
    }

    private fun PostCategory.buttonId(): Int = when (this) {
        PostCategory.LOCAL_EVENT -> R.id.createEvent
        PostCategory.FREE_STUFF -> R.id.createFree
        PostCategory.HELP_REQUEST -> R.id.createHelp
        PostCategory.SAFETY_ALERT -> R.id.createAlert
        else -> R.id.createLostPet
    }

    private fun updateLocationUi(areaOverride: String? = null) {
        if (editingPost != null) {
            binding.locationButton.visibility = View.GONE
            binding.locationText.text = editingPost?.approximateArea?.ifBlank { "Location not shared" }.orEmpty()
            return
        }
        val sharing = binding.shareLocationSwitch.isChecked
        binding.locationButton.visibility = if (sharing) View.VISIBLE else View.GONE
        binding.locationText.text = if (sharing) {
            areaOverride?.takeIf { it.isNotBlank() } ?: viewModel.locationDisplayLabel()
        } else {
            "Location not shared"
        }
    }

    private fun clearForm() {
        binding.titleInput.text?.clear()
        binding.bodyInput.text?.clear()
        binding.createCategoryGroup.check(R.id.createLostPet)
        binding.shareLocationSwitch.isChecked = false
        updateLocationUi()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
