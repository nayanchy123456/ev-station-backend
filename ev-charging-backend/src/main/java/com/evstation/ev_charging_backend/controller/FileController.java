package com.evstation.ev_charging_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    // Upload folder (relative to project root)
    private final String uploadDir = "./uploads";

    /**
     * Upload multiple images and return accessible URLs for frontend
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImages(@RequestParam("images") MultipartFile[] files) {
        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().body("No files provided");
            }

            List<String> urls = new ArrayList<>();

            for (MultipartFile file : files) {
                // Clean original filename
                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null || originalFilename.isEmpty()) {
                    return ResponseEntity.badRequest().body("File name is invalid");
                }
                originalFilename = StringUtils.cleanPath(originalFilename);
                String fileName = System.currentTimeMillis() + "_" + originalFilename;

                // Create upload directory if it doesn't exist
                Path uploadPath = Paths.get(uploadDir);
                if (!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                // Save file
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // Generate frontend-accessible URL
                // Use /uploads/<filename> which Spring will serve statically
                String fileUrl = "/uploads/" + fileName;
                urls.add(fileUrl);
            }

            return ResponseEntity.ok(urls);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not upload files: " + e.getMessage());
        }
    }
}
