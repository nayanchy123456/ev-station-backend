package com.evstation.ev_charging_backend.controller;

import com.evstation.ev_charging_backend.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final CloudinaryService cloudinaryService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImages(@RequestParam("images") MultipartFile[] files) {
        try {
            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest().body("No files provided");
            }

            List<String> urls = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file.getOriginalFilename() == null || file.getOriginalFilename().isEmpty()) {
                    return ResponseEntity.badRequest().body("File name is invalid");
                }
                String url = cloudinaryService.uploadFile(file, "ev-charging/general");
                urls.add(url);
            }

            return ResponseEntity.ok(urls);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not upload files: " + e.getMessage());
        }
    }
}