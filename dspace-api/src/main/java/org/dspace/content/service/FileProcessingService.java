package org.dspace.content.service;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.List;

import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.content.dto.FileProcessingResult;
import org.dspace.content.dto.OriginalFileProcessingResult;
import org.dspace.content.dto.EmlProcessingResult;

/*
 * Service responsible for handling file processing logic
 *
 * Replaces:
 * - UploadUtil.handleEMLFile()
 * - UploadUtil.handleOriginalFile()
 */
public interface FileProcessingService {

    /*
     * Handles EML (email) file processing
     *
     * Returns:
     * - InputStream for final bitstream
     * - Original bitstream
     * - Updated working bundle
     */
    EmlProcessingResult handleEmlFile(
            Context context,
            Item item,
            Bundle workingBundle,
            File tempFile,
            String documentName,
            String createdDate,
            String source,
            String requirementId,
            String createdBy,
            Map<String, List<String>> unconvertedFileMap
    ) throws Exception;

    /*
     * Handles non-EML files (doc, html, txt, image)
     *
     * Returns:
     * - InputStream
     * - Original bitstream
     */
    OriginalFileProcessingResult handleOriginalFile(
            Context context,
            Item item,
            File tempFile,
            String createdDate,
            String source,
            String requirementId,
            String createdBy
    ) throws Exception;
}