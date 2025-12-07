package com.toastedsiopao.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageServiceImpl.class);

    @Autowired
    private Cloudinary cloudinary;

    @Override
    @PostConstruct
    public void init() {
        log.info("FileStorageService initialized using Cloudinary.");
    }

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file.");
        }

        try {
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", "mk-toasted-demo"));
            
            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("Uploaded file to Cloudinary: {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Override
    public void delete(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return;
        }

        // Extract public ID from URL to delete from Cloudinary
        try {
            String publicId = extractPublicIdFromUrl(fileUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Deleted file from Cloudinary: {}", publicId);
            }
        } catch (Exception e) {
            log.warn("Failed to delete file from Cloudinary: {}. Error: {}", fileUrl, e.getMessage());
        }
    }

    @Override
    public Resource loadAsResource(String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        try {
            // For remote Cloudinary URLs, we return a UrlResource
            return new UrlResource(filename);
        } catch (MalformedURLException e) {
            log.error("Could not create URL resource for: {}", filename, e);
            return null;
        }
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            int uploadIndex = url.indexOf("/upload/");
            if (uploadIndex == -1) return null;

            // Get the part after /upload/
            String path = url.substring(uploadIndex + 8);
            
            // Remove version if present (e.g., v12345678/)
            if (path.startsWith("v")) {
                int slashIndex = path.indexOf("/");
                if (slashIndex != -1) {
                    path = path.substring(slashIndex + 1);
                }
            }

            // Remove extension
            int dotIndex = path.lastIndexOf(".");
            if (dotIndex != -1) {
                path = path.substring(0, dotIndex);
            }

            return path;
        } catch (Exception e) {
            log.warn("Error parsing Cloudinary URL: {}", url);
            return null;
        }
    }
}