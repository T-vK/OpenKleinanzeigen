package de.openkleinanzeigen.core.api

import de.openkleinanzeigen.core.domain.model.SearchQuery
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Live integration test against kleinanzeigen.de API.
 * Skips when network blocks the request (e.g. CI datacenter).
 */
class KleinanzeigenLiveApiTest {

    private val client = KleinanzeigenApiClient()

    @Test
    fun search_returnsResults() {
        try {
            val results = client.search(SearchQuery(query = "fahrrad", size = 5))
            assumeTrue("API returned no results (may be blocked)", results.isNotEmpty())
            assert(results.all { it.id.isNotBlank() && it.title.isNotBlank() })
        } catch (e: Exception) {
            assumeTrue("Live API unavailable: ${e.message}", false)
        }
    }
}
