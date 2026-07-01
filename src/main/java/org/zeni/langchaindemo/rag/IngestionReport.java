package org.zeni.langchaindemo.rag;

import java.util.List;

/**
 * Result of an {@code /assistant/ingest} request.
 *
 * @param filesIngested  per-file summary (filename + number of segments stored)
 * @param totalSegments  total number of text segments embedded and stored
 */
public record IngestionReport(List<FileReport> filesIngested, int totalSegments) {

    public record FileReport(String fileName, int segments) {
    }
}

