package com.example.phaze.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phaze.data.model.SearchResults
import com.example.phaze.data.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

sealed interface SearchMode {
    /** No query yet — show recent searches + browse grid. */
    data object Idle : SearchMode
    data object Loading : SearchMode
    data class Results(val results: SearchResults) : SearchMode
    data class Error(val message: String) : SearchMode
}

data class SearchUiState(
    val query: String = "",
    val recentSearches: List<String> = emptyList(),
    val mode: SearchMode = SearchMode.Idle,
)

/**
 * Debounced `search3` (PLAN.md §5, mockup search.html).
 *
 * The query text updates immediately; the network search is debounced by
 * [DEBOUNCE_MS] and the latest query wins (in-flight searches are cancelled via
 * `flatMapLatest`). Recent searches are kept in memory (a shared preference /
 * DataStore list is a later refinement).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _recent = MutableStateFlow<List<String>>(emptyList())

    private val mode: Flow<SearchMode> = _query
        .debounce(DEBOUNCE_MS)
        .flatMapLatest { query -> searchFlow(query) }

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        mode,
        _recent,
    ) { query, searchMode, recent ->
        SearchUiState(query = query, recentSearches = recent, mode = searchMode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onSearchSubmit() {
        persistRecent(_query.value)
    }

    fun onRecentClick(term: String) {
        _query.value = term
        persistRecent(term)
    }

    fun clearQuery() {
        _query.value = ""
    }

    private fun persistRecent(term: String) {
        val t = term.trim()
        if (t.isEmpty()) return
        _recent.update { (listOf(t) + it.filter { s -> !s.equals(t, ignoreCase = true) }).take(MAX_RECENT) }
    }

    private fun searchFlow(query: String): Flow<SearchMode> = flow {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            emit(SearchMode.Idle)
        } else {
            emit(SearchMode.Loading)
            val result = searchRepository.search(trimmed)
            emit(
                result.fold(
                    onSuccess = { SearchMode.Results(it) },
                    onFailure = { SearchMode.Error(it.message ?: "Search failed") },
                )
            )
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 300L
        const val MAX_RECENT = 8
    }
}
