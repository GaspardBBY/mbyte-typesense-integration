/*
 * Copyright (C) 2025 Jerome Blanchard <jayblanc@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.jayblanc.mbyte.store.index;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Startup
@Singleton
public class IndexStoreServiceBean implements IndexStoreService {

    private static final Logger LOGGER = Logger.getLogger(IndexStoreServiceBean.class.getName());

    @Inject IndexStoreConfig config;
    @Inject ObjectMapper mapper;

    private HttpClient client;
    private URI baseUri;

    @PostConstruct
    public void init() {
        LOGGER.log(Level.INFO, "Initializing Typesense index service");
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        baseUri = URI.create(String.format("%s://%s:%d", config.typesense().protocol(), config.typesense().host(), config.typesense().port()));
        try {
            ensureCollection();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to initialize Typesense collection", e);
            throw new RuntimeException("Unable to initialize Typesense collection", e);
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public void index(IndexableContent object) throws IndexStoreException {
        LOGGER.log(Level.INFO, "Indexing object in Typesense: {0}", object.getIdentifier());
        try {
            String payload = mapper.writeValueAsString(IndexStoreDocumentBuilder.buildDocument(object));
            HttpRequest request = baseRequest("/collections/" + encode(config.typesense().collection()) + "/documents?action=upsert")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            sendExpectSuccess(request, "upsert document " + object.getIdentifier());
        } catch (Exception e) {
            throw new IndexStoreException("Can't index an object", e);
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public void remove(String identifier) throws IndexStoreException {
        LOGGER.log(Level.INFO, "Removing document from Typesense: {0}", identifier);
        try {
            HttpRequest request = baseRequest("/collections/" + encode(config.typesense().collection()) + "/documents/" + encode(identifier))
                    .DELETE()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 && response.statusCode() != 404) {
                throw new IndexStoreException("Can't remove object " + identifier + " from index, status=" + response.statusCode() + " body=" + response.body());
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IndexStoreException("Can't remove object " + identifier + " from index", e);
        }
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<IndexStoreResult> search(String scope, String queryString) throws IndexStoreException {
        LOGGER.log(Level.INFO, "Searching query in Typesense: {0}", queryString);
        try {
            String path = "/collections/" + encode(config.typesense().collection()) + "/documents/search"
                    + "?q=" + encode(queryString == null || queryString.isBlank() ? "*" : queryString)
                    + "&query_by=" + encode(String.join(",", IndexStoreDocumentBuilder.CONTENT_FIELD, IndexStoreDocumentBuilder.NAME_FIELD, IndexStoreDocumentBuilder.MIMETYPE_FIELD))
                    + "&highlight_fields=" + encode(String.join(",", IndexStoreDocumentBuilder.CONTENT_FIELD, IndexStoreDocumentBuilder.NAME_FIELD))
                    + "&filter_by=" + encode(IndexStoreDocumentBuilder.STORE_ID_FIELD + ":=" + config.typesense().storeId() + " && "
                    + IndexStoreDocumentBuilder.SCOPE_FIELD + ":=" + scope)
                    + "&per_page=100";
            HttpRequest request = baseRequest(path).GET().build();
            HttpResponse<String> response = sendExpectSuccess(request, "search query " + queryString);
            JsonNode root = mapper.readTree(response.body());
            List<IndexStoreResult> results = new ArrayList<>();
            for (JsonNode hit : root.path("hits")) {
                JsonNode document = hit.path("document");
                IndexStoreResult result = new IndexStoreResult();
                result.setIdentifier(document.path(IndexStoreDocumentBuilder.ID_FIELD).asText());
                result.setType(document.path(IndexStoreDocumentBuilder.TYPE_FIELD).asText());
                result.setScore((float) hit.path("text_match").asDouble(0));
                result.setExplain(extractExplain(hit, document));
                results.add(result);
            }
            return results;
        } catch (Exception e) {
            throw new IndexStoreException("Can't search in index using '" + queryString + "'", e);
        }
    }

    private void ensureCollection() throws IOException, InterruptedException, IndexStoreException {
        String collection = config.typesense().collection();
        HttpRequest get = baseRequest("/collections/" + encode(collection)).GET().build();
        HttpResponse<String> existing = client.send(get, HttpResponse.BodyHandlers.ofString());
        if (existing.statusCode() == 200) {
            LOGGER.log(Level.INFO, "Typesense collection already exists: {0}", collection);
            return;
        }
        if (existing.statusCode() != 404) {
            throw new IOException("Unable to inspect Typesense collection, status=" + existing.statusCode() + " body=" + existing.body());
        }
        String payload = """
                {
                  "name": "%s",
                  "fields": [
                    { "name": "id", "type": "string" },
                    { "name": "store_id", "type": "string", "facet": true },
                    { "name": "type", "type": "string", "facet": true },
                    { "name": "scope", "type": "string", "facet": true },
                    { "name": "name", "type": "string", "optional": true },
                    { "name": "mimetype", "type": "string", "facet": true, "optional": true },
                    { "name": "node_type", "type": "string", "facet": true, "optional": true },
                    { "name": "parent", "type": "string", "optional": true },
                    { "name": "content", "type": "string" },
                    { "name": "modified_at", "type": "int64", "sort": true }
                  ],
                  "default_sorting_field": "modified_at"
                }
                """.formatted(collection);
        HttpRequest create = baseRequest("/collections")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        sendExpectSuccess(create, "create collection " + collection);
        LOGGER.log(Level.INFO, "Created Typesense collection: {0}", collection);
    }

    private String extractExplain(JsonNode hit, JsonNode document) {
        for (JsonNode highlight : hit.path("highlights")) {
            JsonNode snippet = highlight.path("snippet");
            if (!snippet.isMissingNode() && !snippet.asText().isBlank()) {
                return snippet.asText();
            }
            for (JsonNode snippets : highlight.path("snippets")) {
                if (!snippets.asText().isBlank()) {
                    return snippets.asText();
                }
            }
        }
        JsonNode legacyHighlight = hit.path("highlight");
        if (legacyHighlight.isObject()) {
            for (String field : List.of(IndexStoreDocumentBuilder.CONTENT_FIELD, IndexStoreDocumentBuilder.NAME_FIELD)) {
                JsonNode value = legacyHighlight.path(field);
                if (!value.isMissingNode() && !value.asText().isBlank()) {
                    return value.asText();
                }
            }
        }
        String fallback = document.path(IndexStoreDocumentBuilder.CONTENT_FIELD).asText(document.path(IndexStoreDocumentBuilder.NAME_FIELD).asText(""));
        return fallback.length() > 240 ? fallback.substring(0, 240) : fallback;
    }

    private HttpRequest.Builder baseRequest(String path) {
        return HttpRequest.newBuilder(baseUri.resolve(path))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("X-TYPESENSE-API-KEY", config.typesense().apiKey());
    }

    private HttpResponse<String> sendExpectSuccess(HttpRequest request, String action) throws IOException, InterruptedException, IndexStoreException {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IndexStoreException("Unable to " + action + ", status=" + response.statusCode() + " body=" + response.body());
        }
        return response;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
