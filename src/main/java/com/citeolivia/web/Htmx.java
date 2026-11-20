package com.citeolivia.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Helpers for htmx requests. */
final class Htmx {

    static final String REQUEST_HEADER = "HX-Request";

    private Htmx() {}

    /** Full page navigation after an htmx action (htmx follows HX-Redirect, plain browsers the 303). */
    static ResponseEntity<Void> redirect(boolean htmx, String url) {
        if (htmx) {
            return ResponseEntity.ok().header("HX-Redirect", url).build();
        }
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .header(HttpHeaders.LOCATION, url)
                .build();
    }
}
