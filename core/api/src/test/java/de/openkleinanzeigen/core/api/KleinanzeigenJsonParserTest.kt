package de.openkleinanzeigen.core.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KleinanzeigenJsonParserTest {

    private val parser = KleinanzeigenJsonParser()

    @Test
    fun parseSearchResponse_extractsListings() {
        val json = """
            {
              "{http://www.ebayclassifiedsgroup.com/schema/ad/v1}ads": {
                "value": {
                  "ad": [{
                    "title": {"value": "Test Bike"},
                    "link": [
                      {"href": "https://api.kleinanzeigen.de/api/ads/123", "rel": "self"},
                      {"href": "https://www.kleinanzeigen.de/s-anzeige/test/123", "rel": "self-public-website"}
                    ],
                    "ad-address": {
                      "state": {"value": "Berlin"},
                      "zip-code": {"value": "10115"}
                    },
                    "price": {
                      "amount": {"value": "100"},
                      "price-type": {"value": "FIXED"}
                    }
                  }]
                }
              }
            }
        """.trimIndent()

        val listings = parser.parseSearchResponse(json)
        assertEquals(1, listings.size)
        assertEquals("123", listings[0].id)
        assertEquals("Test Bike", listings[0].title)
        assertTrue(listings[0].priceLabel?.contains("100") == true)
    }
}
