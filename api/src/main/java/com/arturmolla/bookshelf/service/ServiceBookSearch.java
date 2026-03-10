package com.arturmolla.bookshelf.service;

import com.arturmolla.bookshelf.model.common.PageResponse;
import com.arturmolla.bookshelf.model.dto.BookSearchResultDto;
import com.arturmolla.bookshelf.model.enums.BookSearchSource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceBookSearch {
    private static final String OPEN_LIBRARY_COVERS_URL = "https://covers.openlibrary.org/b/id/{coverId}-M.jpg";
    private static final int GOOGLE_MAX_PAGE_SIZE = 40;
    private static final int OPEN_LIBRARY_MAX_PAGE_SIZE = 100;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    @Value("${book-search.google.api-key:}")
    private String googleApiKey;

    public PageResponse<BookSearchResultDto> search(String query, BookSearchSource source, int page, int size) {
        return switch (source) {
            case GOOGLE_BOOKS -> searchGoogleBooks(query, page, size);
            case OPEN_LIBRARY -> searchOpenLibrary(query, page, size);
        };
    }

    // Google Books
    private PageResponse<BookSearchResultDto> searchGoogleBooks(String query, int page, int size) {
        int pageSize = Math.min(size, GOOGLE_MAX_PAGE_SIZE);
        int startIndex = page * pageSize;
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .newInstance()
                    .scheme("https").host("www.googleapis.com").path("/books/v1/volumes")
                    .queryParam("q", query)
                    .queryParam("maxResults", pageSize)
                    .queryParam("startIndex", startIndex)
                    .queryParam("printType", "books");
            if (googleApiKey != null && !googleApiKey.isBlank()) {
                builder.queryParam("key", googleApiKey);
            }
            URI uri = builder.build().toUri();
            String response = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.path("items");
            long totalItems = root.path("totalItems").asLong(0);
            if (items.isMissingNode() || !items.isArray()) {
                return emptyPage(page, pageSize);
            }
            List<BookSearchResultDto> results = new ArrayList<>();
            for (JsonNode item : items) {
                results.add(parseGoogleBooksItem(item));
            }
            int totalPages = (int) Math.ceil((double) totalItems / pageSize);
            return new PageResponse<>(results, page, pageSize, totalItems, totalPages, page == 0, page >= totalPages - 1);
        } catch (Exception e) {
            log.error("Error searching Google Books for query '{}': {}", query, e.getMessage());
            return emptyPage(page, pageSize);
        }
    }

    private BookSearchResultDto parseGoogleBooksItem(JsonNode item) {
        JsonNode info = item.path("volumeInfo");
        List<String> authors = new ArrayList<>();
        info.path("authors").forEach(a -> authors.add(a.asText()));
        List<String> categories = new ArrayList<>();
        info.path("categories").forEach(c -> categories.add(c.asText()));
        String isbn = null;
        for (JsonNode identifier : info.path("industryIdentifiers")) {
            String type = identifier.path("type").asText();
            if ("ISBN_13".equals(type) || "ISBN_10".equals(type)) {
                isbn = identifier.path("identifier").asText();
                if ("ISBN_13".equals(type)) break;
            }
        }
        String coverUrl = null;
        JsonNode imageLinks = info.path("imageLinks");
        if (!imageLinks.isMissingNode()) {
            coverUrl = imageLinks.has("thumbnail")
                    ? imageLinks.path("thumbnail").asText()
                    : imageLinks.path("smallThumbnail").asText(null);
            if (coverUrl != null) coverUrl = coverUrl.replace("http://", "https://");
        }
        return BookSearchResultDto.builder()
                .title(info.path("title").asText(null))
                .authors(authors)
                .isbn(isbn)
                .synopsis(info.path("description").asText(null))
                .genre(categories.isEmpty() ? null : categories.getFirst())
                .coverUrl(coverUrl)
                .publisher(info.path("publisher").asText(null))
                .publishedDate(info.path("publishedDate").asText(null))
                .pageCount(info.has("pageCount") ? info.path("pageCount").asInt() : null)
                .externalId(item.path("id").asText(null))
                .source(BookSearchSource.GOOGLE_BOOKS)
                .previewLink(info.path("previewLink").asText(null))
                .build();
    }

    // Open Library
    private PageResponse<BookSearchResultDto> searchOpenLibrary(String query, int page, int size) {
        int pageSize = Math.min(size, OPEN_LIBRARY_MAX_PAGE_SIZE);
        int offset = page * pageSize;
        try {
            URI uri = UriComponentsBuilder
                    .newInstance()
                    .scheme("https").host("openlibrary.org").path("/search.json")
                    .queryParam("q", query)
                    .queryParam("limit", pageSize)
                    .queryParam("offset", offset)
                    .queryParam("fields", "key,title,author_name,isbn,subject,cover_i,publisher,first_publish_year,number_of_pages_median")
                    .build().toUri();
            String response = restTemplate.getForObject(uri, String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode docs = root.path("docs");
            long totalItems = root.path("numFound").asLong(0);
            if (docs.isMissingNode() || !docs.isArray()) {
                return emptyPage(page, pageSize);
            }
            List<BookSearchResultDto> results = new ArrayList<>();
            for (JsonNode doc : docs) {
                results.add(parseOpenLibraryDoc(doc));
            }
            int totalPages = (int) Math.ceil((double) totalItems / pageSize);
            return new PageResponse<>(results, page, pageSize, totalItems, totalPages, page == 0, page >= totalPages - 1);
        } catch (Exception e) {
            log.error("Error searching Open Library for query '{}': {}", query, e.getMessage());
            return emptyPage(page, pageSize);
        }
    }

    private BookSearchResultDto parseOpenLibraryDoc(JsonNode doc) {
        List<String> authors = new ArrayList<>();
        doc.path("author_name").forEach(a -> authors.add(a.asText()));
        List<String> subjects = new ArrayList<>();
        doc.path("subject").forEach(s -> subjects.add(s.asText()));
        String isbn = null;
        JsonNode isbns = doc.path("isbn");
        if (isbns.isArray() && !isbns.isEmpty()) {
            for (JsonNode i : isbns) {
                String val = i.asText();
                if (val.length() == 13) {
                    isbn = val;
                    break;
                }
            }
            if (isbn == null) isbn = isbns.get(0).asText();
        }
        String coverUrl = null;
        JsonNode coverId = doc.path("cover_i");
        if (!coverId.isMissingNode() && !coverId.isNull()) {
            coverUrl = OPEN_LIBRARY_COVERS_URL.replace("{coverId}", coverId.asText());
        }
        String publishedDate = null;
        JsonNode year = doc.path("first_publish_year");
        if (!year.isMissingNode() && !year.isNull()) {
            publishedDate = year.asText();
        }
        String publisher = null;
        JsonNode publishers = doc.path("publisher");
        if (publishers.isArray() && !publishers.isEmpty()) {
            publisher = publishers.get(0).asText();
        }
        Integer pageCount = null;
        JsonNode pages = doc.path("number_of_pages_median");
        if (!pages.isMissingNode() && !pages.isNull()) {
            pageCount = pages.asInt();
        }
        String key = doc.path("key").asText(null);
        String previewLink = key != null ? "https://openlibrary.org" + key : null;
        return BookSearchResultDto.builder()
                .title(doc.path("title").asText(null))
                .authors(authors)
                .isbn(isbn)
                .synopsis(null)
                .genre(subjects.isEmpty() ? null : subjects.getFirst())
                .coverUrl(coverUrl)
                .publisher(publisher)
                .publishedDate(publishedDate)
                .pageCount(pageCount)
                .externalId(key)
                .source(BookSearchSource.OPEN_LIBRARY)
                .previewLink(previewLink)
                .build();
    }

    // Helpers
    private PageResponse<BookSearchResultDto> emptyPage(int page, int size) {
        return new PageResponse<>(Collections.emptyList(), page, size, 0, 0, true, true);
    }
}