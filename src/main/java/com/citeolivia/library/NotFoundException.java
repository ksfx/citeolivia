package com.citeolivia.library;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A requested element does not exist (or does not belong to the project). */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends RuntimeException {

    public NotFoundException(String what) {
        super(what + " nicht gefunden");
    }
}
