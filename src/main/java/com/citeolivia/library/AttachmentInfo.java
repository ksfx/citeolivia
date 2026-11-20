package com.citeolivia.library;

import java.time.Instant;
import java.util.Locale;

/** Metadata of an attached file (without contents). */
public record AttachmentInfo(Long id, Long referenceEntryId, String fileName, long sizeBytes, Instant createdAt) {

    /** "1,2 MB" */
    public String sizeLabel() {
        if (sizeBytes < 1024) {
            return sizeBytes + " B";
        }
        if (sizeBytes < 1024 * 1024) {
            return String.format(Locale.GERMAN, "%.0f KB", sizeBytes / 1024.0);
        }
        return String.format(Locale.GERMAN, "%.1f MB", sizeBytes / (1024.0 * 1024.0));
    }
}
