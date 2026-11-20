package com.citeolivia.library;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class AttachmentRulesTests {

    @Test
    void recognisesPdfBySignature() {
        assertThat(AttachmentService.isPdf("%PDF-1.7\n…".getBytes(StandardCharsets.ISO_8859_1)))
                .isTrue();
        assertThat(AttachmentService.isPdf("\r\n%PDF-1.4".getBytes(StandardCharsets.ISO_8859_1)))
                .isTrue();
        assertThat(AttachmentService.isPdf("<html>%PDF".getBytes(StandardCharsets.ISO_8859_1)))
                .isFalse();
        assertThat(AttachmentService.isPdf("MZ\u0090\u0000".getBytes(StandardCharsets.ISO_8859_1)))
                .isFalse();
        assertThat(AttachmentService.isPdf(new byte[2])).isFalse();
    }

    @Test
    void sanitisesFileNames() {
        assertThat(AttachmentService.safeFileName("C:\\Users\\x\\Müller 2020.pdf"))
                .isEqualTo("Müller 2020.pdf");
        assertThat(AttachmentService.safeFileName("../../etc/passwd")).isEqualTo("passwd.pdf");
        assertThat(AttachmentService.safeFileName("a\"b<c>\n.PDF")).isEqualTo("abc.PDF");
        assertThat(AttachmentService.safeFileName("  ")).isEqualTo("dokument.pdf");
    }
}
