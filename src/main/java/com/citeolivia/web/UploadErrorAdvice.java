package com.citeolivia.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.support.RequestContextUtils;

/** Too large uploads: back to the reference with a readable message instead of an error page. */
@ControllerAdvice
class UploadErrorAdvice {

    private static final Pattern ATTACHMENT_UPLOAD =
            Pattern.compile("^/projects/(\\d+)/references/(\\d+)/attachments$");
    private static final Pattern IMPORT = Pattern.compile("^/projects/(\\d+)/import$");

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    String tooLarge(HttpServletRequest request) {
        String path = request.getRequestURI();
        Matcher upload = ATTACHMENT_UPLOAD.matcher(path);
        Matcher bibImport = IMPORT.matcher(path);
        if (upload.matches()) {
            RequestContextUtils.getOutputFlashMap(request)
                    .put("error", "Die Datei ist zu groß (maximal 50 MB pro PDF).");
            return "redirect:/projects/" + upload.group(1) + "/references/" + upload.group(2) + "#files";
        }
        if (bibImport.matches()) {
            RequestContextUtils.getOutputFlashMap(request).put("importError", "Die Datei ist zu groß (maximal 5 MB).");
            return "redirect:/projects/" + bibImport.group(1) + "/bibliography#import";
        }
        return "redirect:/dashboard";
    }
}
