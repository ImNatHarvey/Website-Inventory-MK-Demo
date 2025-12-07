package com.toastedsiopao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // No local resource handlers needed for Cloudinary.
    // Images are accessed via absolute URLs (https://res.cloudinary.com/...)
    // Static assets (css/js) are handled automatically by Spring Boot.

}
