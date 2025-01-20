package com.abc.qrscannerdev.ui.history

import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.abc.qrscannerdev.R
import com.abc.qrscannerdev.databinding.FragmentHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: ScanResultAdapter
    private lateinit var searchAdapter: ScanResultAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupSearchView()
        setupFilterFab()
        observeViewModel()
    }

    private fun setupToolbar() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.history_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_delete_all -> {
                        showDeleteAllDialog()
                        true
                    }
                    R.id.action_export -> {
                        // TODO: Implement export functionality
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupRecyclerView() {
        adapter = ScanResultAdapter(
            onItemClick = { scanResult ->
                findNavController().navigate(
                    HistoryFragmentDirections.actionHistoryToDetail(scanResult.id)
                )
            },
            onFavoriteClick = { scanResult ->
                viewModel.toggleFavorite(scanResult)
            },
            onDeleteClick = { scanResult ->
                viewModel.delete(scanResult)
            }
        )

        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HistoryFragment.adapter
        }
    }

    private fun setupSearchView() {
        searchAdapter = ScanResultAdapter(
            onItemClick = { scanResult ->
                binding.searchView.hide()
                findNavController().navigate(
                    HistoryFragmentDirections.actionHistoryToDetail(scanResult.id)
                )
            },
            onFavoriteClick = { scanResult ->
                viewModel.toggleFavorite(scanResult)
            },
            onDeleteClick = { scanResult ->
                viewModel.delete(scanResult)
            }
        )

        binding.searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }

        binding.searchView.editText.setOnEditorActionListener { textView, _, _ ->
            viewModel.setSearchQuery(textView.text.toString())
            false
        }
    }

    private fun setupFilterFab() {
        binding.filterFab.setOnClickListener {
            viewModel.toggleShowFavoritesOnly()
        }
    }

    private fun observeViewModel() {
        viewModel.scanResults.observe(viewLifecycleOwner) { results ->
            adapter.submitList(results)
            searchAdapter.submitList(results)
            binding.emptyView.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.showFavoritesOnly.observe(viewLifecycleOwner) { showFavoritesOnly ->
            binding.filterFab.text = getString(
                if (showFavoritesOnly) R.string.show_all else R.string.show_favorites
            )
            binding.filterFab.icon = requireContext().getDrawable(
                if (showFavoritesOnly) R.drawable.ic_list else R.drawable.ic_favorite
            )
        }
    }

    private fun showDeleteAllDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.delete_all_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteAll()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 