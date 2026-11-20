package com.citeolivia.library.lookup;

import com.citeolivia.library.ReferenceDraft;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Looks up bibliographic data for a DOI (Crossref) or ISBN (Open Library, Deutsche Nationalbibliothek,
 * Google Books – in this order). Only the identifier is sent to the services.
 */
@Service
public class LookupService {

    private static final Logger log = LoggerFactory.getLogger(LookupService.class);

    private final RestClient http;

    public LookupService() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader("User-Agent", "CiteOlivia/0.1 (literature management)")
                .build();
    }

    /** Result of a lookup: the draft (if found) and the name of the source or a problem description. */
    public record Result(Optional<ReferenceDraft> draft, String message) {

        public boolean found() {
            return draft.isPresent();
        }
    }

    @PreAuthorize("hasAuthority('library:use')")
    public Result lookup(String input) {
        Optional<Identifier> id = Identifier.parse(input);
        if (id.isEmpty()) {
            return new Result(
                    Optional.empty(),
                    "Keine gültige DOI oder ISBN erkannt. Beispiele: 10.1000/xyz123 oder 978-3-16-148410-0");
        }
        Identifier identifier = id.get();
        if (identifier.kind() == Identifier.Kind.DOI) {
            String doi = identifier.value();
            return attempt(
                            "Crossref",
                            "https://api.crossref.org/works/" + URLEncoder.encode(doi, StandardCharsets.UTF_8),
                            LookupMappers::crossref)
                    .orElseGet(() -> new Result(Optional.empty(), "Zur DOI " + doi + " wurde nichts gefunden."));
        }
        String isbn = identifier.value();
        return attempt(
                        "Open Library",
                        "https://openlibrary.org/api/books?bibkeys=ISBN:" + isbn + "&format=json&jscmd=data",
                        body -> LookupMappers.openLibrary(body, isbn))
                .or(() -> attempt(
                        "Deutsche Nationalbibliothek",
                        "https://services.dnb.de/sru/dnb?version=1.1&operation=searchRetrieve&recordSchema=oai_dc"
                                + "&maximumRecords=1&query=num%3D" + isbn,
                        body -> LookupMappers.dnb(body, isbn)))
                .or(() -> attempt(
                        "Google Books",
                        "https://www.googleapis.com/books/v1/volumes?q=isbn:" + isbn,
                        body -> LookupMappers.googleBooks(body, isbn)))
                .orElseGet(() -> new Result(
                        Optional.empty(),
                        "Zur ISBN " + isbn
                                + " wurde bei Open Library, Nationalbibliothek und Google Books nichts gefunden"
                                + " (oder die Dienste sind gerade nicht erreichbar). Bitte manuell erfassen."));
    }

    private Optional<Result> attempt(String source, String url, Function<String, Optional<ReferenceDraft>> mapper) {
        try {
            String body = http.get().uri(URI.create(url)).retrieve().body(String.class);
            if (body == null) {
                return Optional.empty();
            }
            return mapper.apply(body).map(d -> new Result(Optional.of(d), "Gefunden bei " + source + "."));
        } catch (RestClientException | IllegalStateException | tools.jackson.core.JacksonException e) {
            log.info("Lookup at {} failed: {}", source, e.getMessage());
            return Optional.empty();
        }
    }
}
