package com.sixblock.app.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.sixblock.app.R
import com.sixblock.app.databinding.FragmentActivityBinding
import com.sixblock.app.ui.common.sixBlockFactory
import com.sixblock.app.ui.main.MainActivity

class ActivityFragment : Fragment() {
    private var _binding: FragmentActivityBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ActivityViewModel by viewModels { sixBlockFactory }
    private val adapter = ActivityAdapter { item ->
        viewModel.markRead(item.id)
        item.postId?.let { (requireActivity() as MainActivity).openDetail(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.activityRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.activityRecycler.adapter = adapter
        viewModel.activityState.observe(viewLifecycleOwner) { state ->
            val activity = state.data.orEmpty()
            adapter.submitList(activity)
            binding.activityEmptyState.visibility = if (!state.isLoading && activity.isEmpty()) View.VISIBLE else View.GONE
            binding.activityEmptyMessage.text = state.errorMessage
                ?: state.emptyMessage
                ?: getString(R.string.activity_empty_message)
        }
        viewModel.load()
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) viewModel.markAllRead()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) viewModel.markAllRead()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
