package com.abc.qrscannerdev.ui.history

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.abc.qrscannerdev.R
import com.abc.qrscannerdev.databinding.FragmentHistoryBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

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
                        exportHistory()
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
            .setTitle(R.string.history_delete_all)
            .setMessage(R.string.delete_all_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deleteAll()
                showSnackbar(getString(R.string.history_deleted))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun exportHistory() {
        viewModel.getAllScans { scans ->
            if (scans.isEmpty()) {
                showSnackbar(getString(R.string.history_empty))
                return@getAllScans
            }

            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(Date())
                val filename = "scan_history_$timestamp.csv"
                val file = File(requireContext().cacheDir, filename)

                FileWriter(file).use { writer ->
                    writer.append("Content,Format,Date\n")
                    scans.forEach { scan ->
                        writer.append("${scan.content},${scan.format},${scan.timestamp}\n")
                    }
                }

                val uri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    file
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                startActivity(Intent.createChooser(intent, getString(R.string.history_export)))
            } catch (e: Exception) {
                showSnackbar(getString(R.string.export_failed))
            }
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 