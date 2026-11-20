package com.citeolivia.library;

import com.citeolivia.security.ProjectAccess;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PDF attachments of references, stored in PostgreSQL. Only PDFs (checked by file signature) up to
 * {@link #MAX_BYTES} are accepted; the same file can be attached to a reference only once.
 */
@Service
public class AttachmentService {

    public static final long MAX_BYTES = 50L * 1024 * 1024;
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final JdbcClient jdbc;

    AttachmentService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** File name and contents for download. */
    public record Content(String fileName, String contentType, byte[] data) {}

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public List<AttachmentInfo> list(Long projectId, Long referenceId) {
        return jdbc.sql("""
                        SELECT id, reference_entry_id, file_name, size_bytes, created_at FROM reference_attachment
                        WHERE project_id = :p AND reference_entry_id = :r ORDER BY created_at, id
                        """)
                .param("p", projectId)
                .param("r", referenceId)
                .query(AttachmentInfo.class)
                .list();
    }

    /** Number of attachments per reference of the project. */
    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public Map<Long, Long> counts(Long projectId) {
        Map<Long, Long> counts = new LinkedHashMap<>();
        jdbc.sql("""
                        SELECT reference_entry_id, count(*) FROM reference_attachment
                        WHERE project_id = :p GROUP BY reference_entry_id
                        """)
                .param("p", projectId)
                .query((rs, n) -> counts.put(rs.getLong(1), rs.getLong(2)))
                .list();
        return counts;
    }

    /**
     * Stores a PDF. Throws {@link IllegalArgumentException} with a user-facing message if the file is
     * empty, too large, not a PDF or already attached.
     */
    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public long add(Long projectId, Long referenceId, String originalFileName, byte[] data) {
        boolean referenceExists = jdbc.sql(
                        "SELECT EXISTS (SELECT 1 FROM reference_entry WHERE id = :r AND project_id = :p)")
                .param("r", referenceId)
                .param("p", projectId)
                .query(Boolean.class)
                .single();
        if (!referenceExists) {
            throw new NotFoundException("Titel");
        }
        String name = safeFileName(originalFileName);
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("„" + name + "“ ist leer.");
        }
        if (data.length > MAX_BYTES) {
            throw new IllegalArgumentException("„" + name + "“ ist größer als 50 MB.");
        }
        if (!isPdf(data)) {
            throw new IllegalArgumentException("„" + name + "“ ist keine PDF-Datei.");
        }
        try {
            return jdbc.sql("""
                            INSERT INTO reference_attachment
                              (project_id, reference_entry_id, file_name, content_type, size_bytes, sha256, data)
                            VALUES (:p, :r, :name, 'application/pdf', :size, :sha, :data) RETURNING id
                            """)
                    .param("p", projectId)
                    .param("r", referenceId)
                    .param("name", name)
                    .param("size", (long) data.length)
                    .param("sha", sha256(data))
                    .param("data", data)
                    .query(Long.class)
                    .single();
        } catch (DuplicateKeyException e) {
            throw new IllegalArgumentException("„" + name + "“ ist bei diesem Titel schon hinterlegt.");
        }
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional(readOnly = true)
    public Content content(Long projectId, Long attachmentId) {
        Optional<Content> content = jdbc.sql("""
                        SELECT file_name, content_type, data FROM reference_attachment WHERE id = :id AND project_id = :p
                        """)
                .param("id", attachmentId)
                .param("p", projectId)
                .query((rs, n) -> new Content(rs.getString(1), rs.getString(2), rs.getBytes(3)))
                .optional();
        return content.orElseThrow(() -> new NotFoundException("Datei"));
    }

    @PreAuthorize(ProjectAccess.PROJECT)
    @Transactional
    public void delete(Long projectId, Long attachmentId) {
        int deleted = jdbc.sql("DELETE FROM reference_attachment WHERE id = :id AND project_id = :p")
                .param("id", attachmentId)
                .param("p", projectId)
                .update();
        if (deleted == 0) {
            throw new NotFoundException("Datei");
        }
    }

    static boolean isPdf(byte[] data) {
        // The signature must be at the start; some generators put up to 1 KB of junk before it.
        int limit = Math.min(data.length - PDF_SIGNATURE.length, 1024);
        outer:
        for (int i = 0; i <= limit; i++) {
            for (int j = 0; j < PDF_SIGNATURE.length; j++) {
                if (data[i + j] != PDF_SIGNATURE[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    /** Keeps the visible name, strips paths and control characters, ensures ".pdf". */
    static String safeFileName(String original) {
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}\"<>|:*?]", "")
                .strip();
        if (name.isEmpty()) {
            name = "dokument.pdf";
        }
        if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
            name = name + ".pdf";
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
