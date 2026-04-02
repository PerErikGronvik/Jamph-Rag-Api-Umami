package no.jamph.bigquery

import java.net.URL

data class SiteIdAndPath(
    val siteId: String,
    val urlPath: String
)

/**
 * Parses a full URL (e.g., "https://aksel.nav.no/designsystemet") and resolves it to:
 * - website_id (from the list of available websites)
 * - url_path (e.g., "/designsystemet", or "/" if no path)
 *
 * @param url Full URL string with protocol (e.g., "https://aksel.nav.no/designsystemet")
 * @param websites List of available websites from BigQuery
 * @return SiteIdAndPath containing the matched website_id and url_path
 * @throws IllegalArgumentException if no website matches the domain
 */
fun urlToSiteIdAndPath(url: String, websites: List<Website>): SiteIdAndPath {
    // Parse URL to extract domain and path
    val parsedUrl = URL(url)
    val domain = parsedUrl.host
    val path = parsedUrl.path.ifEmpty { "/" }
    
    // Find matching website by domain
    val website = websites.find { it.domain == domain }
        ?: throw IllegalArgumentException("No website found for domain: $domain. Available: ${websites.map { it.domain }}")
    
    return SiteIdAndPath(
        siteId = website.websiteId,
        urlPath = path
    )
}
