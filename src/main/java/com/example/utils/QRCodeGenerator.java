package com.example.utils;


import com.example.exception.general.GenerateQRCodeException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Component
public class QRCodeGenerator {
    public String generateVerificationToken() {
        return UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().substring(0, 16);
    }

    public String generateQrCodeBase64(String content) {
        byte[] pngData = generateQrCode(content);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
    }

    public byte[] generateQrCode(String content) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, 320, 320);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);

            return pngOutputStream.toByteArray();
        } catch(IOException | WriterException e) {
            throw new GenerateQRCodeException("Failed to generate QR code");
        }
    }
}
