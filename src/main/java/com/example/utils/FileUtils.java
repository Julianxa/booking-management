package com.example.utils;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Component
public class FileUtils {
    public MultipartFile createMultipartFileFromBytes(byte[] bytes, String fileName, String mimeType) {
        return new MultipartFile() {
            @Override
            public String getName() {
                return fileName;
            }

            @Override
            public String getOriginalFilename() {
                return fileName;
            }

            @Override
            public String getContentType() {
                return mimeType != null && !mimeType.trim().isEmpty() ? mimeType : "application/octet-stream";
            }

            @Override
            public boolean isEmpty() {
                return bytes.length == 0;
            }

            @Override
            public long getSize() {
                return bytes.length;
            }

            @Override
            public byte[] getBytes() {
                return bytes.clone();
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(bytes.clone());
            }

            @Override
            public void transferTo(File dest) throws IllegalStateException {
                throw new UnsupportedOperationException("Transfer to file not supported in this implementation");
            }
        };
    }

    public boolean isValidImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (
                contentType.equals("image/jpeg") ||
                        contentType.equals("image/png") ||
                        contentType.equals("image/heic") ||
                        contentType.equals("image/gif") ||
                        contentType.equals("image/bmp")
        );
    }
}

