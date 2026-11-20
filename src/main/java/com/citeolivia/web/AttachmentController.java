package com.citeolivia.web;

import com.citeolivia.library.AttachmentService;
import com.citeolivia.library.ProjectService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Upload, view/download and delete PDF attachments of references. */
@Controller
@RequestMapping("/projects/{projectId}")
class AttachmentController {

    private final ProjectService projects;
    private final AttachmentService attachments;

    AttachmentController(ProjectService projects, AttachmentService attachments) {
        this.projects = projects;
        this.attachments = attachments;
    }

    @PostMapping("/references/{referenceId}/attachments")
    String upload(
            @PathVariable Long projectId,
            @PathVariable Long referenceId,
            @RequestParam(name = "files", required = false) List<MultipartFile> files,
            RedirectAttributes redirect)
            throws IOException {
        projects.get(projectId); // access check before reading the upload
        List<String> errors = new ArrayList<>();
        int stored = 0;
        for (MultipartFile file : files == null ? List.<MultipartFile>of() : files) {
            if (file.isEmpty()
                    && (file.getOriginalFilename() == null
                            || file.getOriginalFilename().isBlank())) {
                continue;
            }
            try {
                attachments.add(projectId, referenceId, file.getOriginalFilename(), file.getBytes());
                stored++;
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }
        if (stored == 0 && errors.isEmpty()) {
            errors.add("Bitte eine PDF-Datei auswählen.");
        }
        if (stored > 0) {
            redirect.addFlashAttribute("message", stored == 1 ? "PDF gespeichert." : stored + " PDFs gespeichert.");
        }
        if (!errors.isEmpty()) {
            redirect.addFlashAttribute("error", String.join(" ", errors));
        }
        return "redirect:/projects/" + projectId + "/references/" + referenceId + "#files";
    }

    /** Opens the PDF in the browser (inline) or downloads it ({@code ?download=true}). */
    @GetMapping("/attachments/{attachmentId}")
    ResponseEntity<byte[]> open(
            @PathVariable Long projectId,
            @PathVariable Long attachmentId,
            @RequestParam(defaultValue = "false") boolean download) {
        AttachmentService.Content content = attachments.content(projectId, attachmentId);
        ContentDisposition disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline())
                .filename(content.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .cacheControl(CacheControl.noStore().cachePrivate())
                .contentType(MediaType.APPLICATION_PDF)
                .body(content.data());
    }

    /** htmx: deletes the attachment and removes its row (empty response). */
    @PostMapping("/attachments/{attachmentId}/delete")
    @ResponseBody
    String delete(@PathVariable Long projectId, @PathVariable Long attachmentId) {
        attachments.delete(projectId, attachmentId);
        return "";
    }
}
