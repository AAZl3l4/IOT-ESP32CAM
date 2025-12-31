package com.springboot.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;


@RestController
@RequestMapping("/file")
public class FileController {
    
    /** 图片保存目录 */
    @Value("${photos-dir}")
    private String photosDir;

    @PostMapping("/upload")
    // 保存文件到服务器
    private String saveFile(@RequestPart("file") MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return "文件名获取失败";
        }
        String fileExtension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex > 0) {
            fileExtension = originalFilename.substring(lastDotIndex);
        }
        String randomFilename = UUID.randomUUID().toString() + fileExtension;

        try {
            File directory = new File(photosDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            String paths = directory.getCanonicalPath();
            File dest = new File(paths + '/' + randomFilename);
            file.transferTo(dest);
            return "http://localhost:8080/file/photos/" + randomFilename;
        } catch (IOException e) {
            e.printStackTrace();
            return "文件保存失败: " + e.getMessage();
        }
    }

    // 预览文件
    @GetMapping("/photos/{filename}")
    public ResponseEntity<Resource> preview(@PathVariable String filename) {
        File file = new File(photosDir + "/" + filename);
        if (!file.exists()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Resource resource;
        try {
            resource = new UrlResource(file.toURI());
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    // 下载文件
    @GetMapping("/photos/download/{filename}")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        File file = new File(photosDir + "/" + filename);
        if (!file.exists()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Resource resource;
        try {
            resource = new UrlResource(file.toURI());
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

}
