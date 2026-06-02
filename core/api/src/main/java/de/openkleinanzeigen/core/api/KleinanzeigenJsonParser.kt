package de.openkleinanzeigen.core.api

import de.openkleinanzeigen.core.domain.model.Listing
import de.openkleinanzeigen.core.domain.model.Location
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class KleinanzeigenJsonParser(private val json: Json = Json { ignoreUnknownKeys = true }) {

    fun parseSearchResponse(body: String): List<Listing> {
        val root = json.parseToJsonElement(body).jsonObject
        val adsKey = root.keys.firstOrNull { it.contains("ads") } ?: return emptyList()
        val adsWrapper = root[adsKey]?.jsonObject ?: return emptyList()
        val value = adsWrapper["value"]?.jsonObject ?: return emptyList()
        val adElement = value["ad"] ?: return emptyList()
        val ads = when (adElement) {
            is JsonArray -> adElement
            is JsonObject -> JsonArray(listOf(adElement))
            else -> return emptyList()
        }
        return ads.mapNotNull { parseAd(it.jsonObject) }
    }

    fun parseAdDetail(body: String): Listing? {
        val root = json.parseToJsonElement(body).jsonObject
        val adKey = root.keys.firstOrNull { it.endsWith("ad") && !it.contains("ads") }
        val adObj = adKey?.let { root[it]?.jsonObject } ?: root
        return parseAd(adObj)
    }

    fun parseLocations(body: String): List<Location> {
        val root = json.parseToJsonElement(body).jsonObject
        val locationsKey = root.keys.firstOrNull { it.contains("locations") } ?: return emptyList()
        val wrapper = root[locationsKey]?.jsonObject ?: return emptyList()
        val value = wrapper["value"]?.jsonObject ?: return emptyList()
        val locElement = value["location"] ?: return emptyList()
        return flattenLocationElements(locElement)
    }

    private fun flattenLocationElements(element: JsonElement): List<Location> {
        val items = when (element) {
            is JsonArray -> element
            is JsonObject -> JsonArray(listOf(element))
            else -> return emptyList()
        }
        val results = mutableListOf<Location>()
        for (item in items) {
            collectLocations(item.jsonObject, results)
        }
        return results.distinctBy { it.id }
    }

    private fun collectLocations(obj: JsonObject, out: MutableList<Location>) {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
            ?: obj["id"]?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull?.toIntOrNull()
        val name = textValue(obj["localized-name"]) ?: textValue(obj["id-name"])
        if (id != null && !name.isNullOrBlank()) {
            val region = obj["regions"]?.jsonObject?.get("region")?.let { regionEl ->
                when (regionEl) {
                    is JsonArray -> regionEl.firstOrNull()?.jsonObject
                    is JsonObject -> regionEl
                    else -> null
                }
            }?.let { textValue(it["localized-name"]) }
            out.add(Location(id = id, name = name, subtitle = region))
        }
        val nested = obj["location"] ?: return
        when (nested) {
            is JsonArray -> nested.forEach { collectLocations(it.jsonObject, out) }
            is JsonObject -> collectLocations(nested, out)
            else -> Unit
        }
    }

    private fun parseAd(obj: JsonObject): Listing? {
        val id = extractAdId(obj) ?: return null
        val title = textValue(obj["title"]) ?: "Untitled"
        val priceLabel = formatPrice(obj["price"])
        val state = textValue(obj["ad-address"]?.jsonObject?.get("state"))
        val zip = textValue(obj["ad-address"]?.jsonObject?.get("zip-code"))
        val location = listOfNotNull(zip, state).joinToString(" ").ifBlank { null }
        val webUrl = obj["link"]?.jsonArray?.firstOrNull { link ->
            link.jsonObject["rel"]?.jsonPrimitive?.contentOrNull == "self-public-website"
        }?.jsonObject?.get("href")?.jsonPrimitive?.contentOrNull
        val imageUrl = obj["pictures"]?.jsonObject?.get("picture")?.let { pic ->
            when (pic) {
                is JsonArray -> pic.firstOrNull()?.jsonObject
                is JsonObject -> pic
                else -> null
            }
        }?.let { firstPic ->
            firstPic["link"]?.jsonArray?.firstOrNull { link ->
                link.jsonObject["rel"]?.jsonPrimitive?.contentOrNull == "thumbnail"
            }?.jsonObject?.get("href")?.jsonPrimitive?.contentOrNull
        }
        val description = textValue(obj["description"])
        return Listing(
            id = id,
            title = title,
            priceLabel = priceLabel,
            location = location,
            imageUrl = imageUrl,
            webUrl = webUrl,
            description = description,
        )
    }

    private fun extractAdId(obj: JsonObject): String? {
        obj["id"]?.jsonPrimitive?.contentOrNull?.let { return it }
        val selfLink = obj["link"]?.jsonArray?.firstOrNull {
            it.jsonObject["rel"]?.jsonPrimitive?.contentOrNull == "self"
        }?.jsonObject?.get("href")?.jsonPrimitive?.contentOrNull
        return selfLink?.substringAfterLast("/")
    }

    private fun textValue(element: JsonElement?): String? {
        if (element == null) return null
        return when (element) {
            is JsonObject -> element["value"]?.jsonPrimitive?.contentOrNull
                ?: element.values.firstOrNull()?.let { textValue(it) }
            else -> element.jsonPrimitive.contentOrNull
        }
    }

    private fun formatPrice(priceObj: JsonElement?): String? {
        val price = priceObj?.jsonObject ?: return null
        val amount = price["amount"]?.jsonObject?.get("value")?.jsonPrimitive?.contentOrNull
        if (!amount.isNullOrBlank()) return "$amount €"
        val type = textValue(price["price-type"])
        return type?.replace('_', ' ')
    }
}
