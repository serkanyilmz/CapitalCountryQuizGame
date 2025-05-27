package org.example;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Scraper:
 * - getCountryCapitalMap(): downloads JSON from restcountries.com and builds a Country→Capital map.
 * - getCountryFlagUrl(), getCountryLanguages(), getCountryCurrency(), getCountrySummary():
 *   scrape Wikipedia pages using Jsoup.
 * - Logs errors and information via Log4j 2.
 */
public class Scraper {
    private static final Logger logger = LogManager.getLogger(Scraper.class);

    /**
     * Finds “Official languages” (or “Languages”) row in the infobox and returns its plain text.
     *
     * @param countryName e.g. "Italy", "France"
     * @return "Languages: …" or "Languages: N/A"/"Languages: Error" on failure
     */
    public String getCountryLanguages(String countryName) {
        String wikiUrl = "https://en.wikipedia.org/wiki/" + countryName.trim().replace(" ", "_");
        try {
            logger.debug("Loading languages for: " + countryName);
            Document doc = Jsoup.connect(wikiUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(12_000)
                    .get();

            Element infobox = doc.selectFirst("table.infobox");
            if (infobox == null) {
                logger.warn("Infobox not found (getCountryLanguages): " + countryName);
                return "Languages: N/A";
            }

            Elements rows = infobox.select("tr");
            for (Element row : rows) {
                Element th = row.selectFirst("th");
                Element td = row.selectFirst("td");
                if (th != null && td != null) {
                    String key = th.text().trim().toLowerCase();
                    if (key.contains("official language") || key.contains("languages")) {
                        String value = td.text().trim();
                        logger.info("Found languages: " + countryName + " → " + value);
                        return "Languages: " + (value.isEmpty() ? "N/A" : value);
                    }
                }
            }
            logger.info("Languages not found (getCountryLanguages): " + countryName);
            return "Languages: N/A";
        } catch (IOException e) {
            logger.error("Error loading languages: " + countryName + " → " + e.getMessage());
            return "Languages: Error";
        }
    }

    /**
     * Retrieves the first non-trivial paragraph (“lead summary”) from the Wikipedia page.
     */
    public String getCountrySummary(String countryName) {
        String wikiUrl = "https://en.wikipedia.org/wiki/" + countryName.trim().replace(" ", "_");
        try {
            logger.debug("Loading summary for: " + countryName);
            Document doc = Jsoup.connect(wikiUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(12_000)
                    .get();

            Elements leadParagraphs = doc.select("#mw-content-text .mw-parser-output > p");
            for (Element p : leadParagraphs) {
                String text = p.text().trim();
                if (text.length() < 40 || text.startsWith("[")) {
                    continue;
                }
                logger.info("Found summary for: " + countryName);
                return text;
            }
            logger.warn("Summary not found: " + countryName);
            return "No summary available for \"" + countryName + "\".";
        } catch (IOException e) {
            logger.error("Error loading summary: " + countryName + " → " + e.getMessage());
            return "Error loading Wikipedia info: " +
                    e.getClass().getSimpleName() + " – " + e.getMessage();
        }
    }

    /**
     * Scrapes the Wikipedia infobox for the flag image URL.
     */
    public String getCountryFlagUrl(String countryName) {
        String wikiUrl = "https://en.wikipedia.org/wiki/" + countryName.trim().replace(" ", "_");
        try {
            logger.debug("Searching flag URL for: " + countryName);
            Document doc = Jsoup.connect(wikiUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(12_000)
                    .get();

            Element infobox = doc.selectFirst("table.infobox");
            if (infobox == null) {
                logger.warn("Infobox not found (getCountryFlagUrl): " + countryName);
                return "";
            }

            Element img = infobox.selectFirst("img");
            if (img == null) {
                logger.warn("Flag <img> not found (getCountryFlagUrl): " + countryName);
                return "";
            }

            String src = img.attr("src");
            String fullUrl;
            if (src.startsWith("//")) {
                fullUrl = "https:" + src;
            } else if (src.startsWith("http")) {
                fullUrl = src;
            } else {
                fullUrl = "https://en.wikipedia.org" + src;
            }
            logger.info("Found flag URL: " + countryName + " → " + fullUrl);
            return fullUrl;
        } catch (IOException e) {
            logger.error("Error loading flag URL: " + countryName + " → " + e.getMessage());
            return "";
        }
    }

    /**
     * Scrapes the Wikipedia infobox for the country’s currency.
     */
    public String getCountryCurrency(String countryName) {
        String wikiUrl = "https://en.wikipedia.org/wiki/" + countryName.trim().replace(" ", "_");
        try {
            logger.debug("Searching currency for: " + countryName);
            Document doc = Jsoup.connect(wikiUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(12_000)
                    .get();

            Element infobox = doc.selectFirst("table.infobox");
            if (infobox == null) {
                logger.warn("Infobox not found (getCountryCurrency): " + countryName);
                return "Currency: N/A";
            }

            Elements rows = infobox.select("tr");
            for (Element row : rows) {
                Element th = row.selectFirst("th");
                Element td = row.selectFirst("td");
                if (th != null && td != null) {
                    String key = th.text().trim().toLowerCase();
                    if (key.contains("currency")) {
                        String value = td.text().trim();
                        logger.info("Found currency: " + countryName + " → " + value);
                        return "Currency: " + (value.isEmpty() ? "N/A" : value);
                    }
                }
            }
            logger.info("Currency not found: " + countryName);
            return "Currency: N/A";
        } catch (IOException e) {
            logger.error("Error loading currency: " + countryName + " → " + e.getMessage());
            return "Currency: Error";
        }
    }

    /**
     * Downloads JSON from restcountries.com and builds a Country→Capital map.
     */
    public Map<String, String> getCountryCapitalMap() {
        logger.info("Retrieving Country→Capital map");
        Map<String, String> countryCapitalMap = new HashMap<>();
        try {
            String url = "https://restcountries.com/v3.1/all";
            String json = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .timeout(10 * 1000)
                    .execute()
                    .body();

            JSONArray countries = new JSONArray(json);
            for (int i = 0; i < countries.length(); i++) {
                JSONObject country = countries.getJSONObject(i);

                String cname = "";
                if (country.has("name") && country.getJSONObject("name").has("common")) {
                    cname = country.getJSONObject("name").getString("common");
                }

                if (country.has("capital")) {
                    JSONArray capitals = country.getJSONArray("capital");
                    if (capitals.length() == 1) {
                        String capital = capitals.getString(0);
                        countryCapitalMap.put(cname, capital);
                    }
                }
            }
            logger.info("Country→Capital map built, size: " + countryCapitalMap.size());
        } catch (IOException e) {
            logger.error("Error retrieving Country→Capital data: " + e.getMessage());
        }

        return countryCapitalMap;
    }
}
    