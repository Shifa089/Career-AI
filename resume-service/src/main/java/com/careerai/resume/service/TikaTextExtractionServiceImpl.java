package com.careerai.resume.service;

import com.careerai.resume.exception.FileProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Apache Tika implementation of {@link TextExtractionService}. Uses an {@link AutoDetectParser}
 * with a 500k-character body limit and normalizes whitespace in the result.
 */
@Service
@Slf4j
public class TikaTextExtractionServiceImpl implements TextExtractionService {

    private static final int MAX_CHARS = 500_000;

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain");

    @Override
    public String extractText(InputStream input, String contentType) {
        try {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(MAX_CHARS);
            Metadata metadata = new Metadata();
            if (contentType != null) {
                metadata.set(Metadata.CONTENT_TYPE, contentType);
            }
            parser.parse(input, handler, metadata, new ParseContext());
            return normalize(handler.toString());
        } catch (Exception e) {
            log.error("Tika failed to extract text (contentType={})", contentType, e);
            throw new FileProcessingException("Could not extract text from the document");
        }
    }

    @Override
    public Map<String, String> extractMetadata(InputStream input) {
        try {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(MAX_CHARS);
            Metadata metadata = new Metadata();
            parser.parse(input, handler, metadata, new ParseContext());

            Map<String, String> result = new HashMap<>();
            for (String name : metadata.names()) {
                result.put(name, metadata.get(name));
            }
            return result;
        } catch (Exception e) {
            log.error("Tika failed to extract metadata", e);
            throw new FileProcessingException("Could not extract document metadata");
        }
    }

    @Override
    public boolean isSupported(String contentType) {
        return contentType != null && SUPPORTED_TYPES.contains(contentType);
    }

    /** Collapses runs of whitespace and trims, while preserving paragraph line breaks. */
    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
