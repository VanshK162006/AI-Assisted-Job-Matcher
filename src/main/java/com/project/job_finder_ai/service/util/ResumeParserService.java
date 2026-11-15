package com.project.job_finder_ai.service.util;

import com.project.job_finder_ai.exception.BadRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ResumeParserService {

    private static final long MAX_BYTES = 2 * 1024 * 1024; // 2 MB

    public String extractTextFromFile(MultipartFile multipartFile) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BadRequestException("Please upload a PDF file.");
        }

        String name = multipartFile.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) {
            throw new BadRequestException("Only PDF files are supported");
        }

        if (multipartFile.getSize() > MAX_BYTES) {
            throw new BadRequestException("Max 2MB PDF is allowed.");
        }

        try (PDDocument document = Loader.loadPDF(multipartFile.getBytes())) {
            if (document.getNumberOfPages() == 0) {
                throw new BadRequestException("PDF has no pages.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document).replaceAll("[ \\t]+", " ")
                    .replaceAll("\\n{3,}", "\n\n")
                    .trim();


            if (text == null || text.isBlank()) {
                throw new BadRequestException("No text found. Is this a scanned PDF?");
            }

            return text.trim();
        }
    }
}