package com.citeolivia.library.citation;

import java.util.ArrayList;
import java.util.List;
import org.springframework.web.util.HtmlUtils;

/**
 * Formatted citation text made of plain and italic segments. Rendered as escaped HTML (safe for
 * {@code th:utext}) or as plain text.
 */
public final class Formatted {

    private record Segment(String text, boolean italic) {}

    private final List<Segment> segments = new ArrayList<>();

    public Formatted text(String text) {
        if (text != null && !text.isEmpty()) {
            segments.add(new Segment(text, false));
        }
        return this;
    }

    public Formatted italic(String text) {
        if (text != null && !text.isEmpty()) {
            segments.add(new Segment(text, true));
        }
        return this;
    }

    /** Ends the current sentence with {@code punctuation} unless it already ends with . ? or !. */
    public Formatted end(String punctuation) {
        String plain = plain().stripTrailing();
        if (!plain.isEmpty() && !(plain.endsWith(".") || plain.endsWith("?") || plain.endsWith("!"))) {
            text(punctuation);
        }
        return this;
    }

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    public String plain() {
        StringBuilder sb = new StringBuilder();
        segments.forEach(s -> sb.append(s.text()));
        return sb.toString();
    }

    public String html() {
        StringBuilder sb = new StringBuilder();
        for (Segment s : segments) {
            String escaped = HtmlUtils.htmlEscape(s.text());
            sb.append(s.italic() ? "<i>" + escaped + "</i>" : escaped);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return plain();
    }
}
