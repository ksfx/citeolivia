package com.citeolivia.web;

import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Marketing video, poster and subtitles under /video/** may be cached by browsers (the video is 12 MB).
 * Files there must get a new name when their content changes. Byte ranges for seeking are supported by
 * Spring's resource handler. The text/vtt type for subtitles comes from server.mime-mappings.
 */
@Configuration(proxyBeanMethods = false)
class MediaResourceConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/video/**")
                .addResourceLocations("classpath:/static/video/")
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
    }
}
