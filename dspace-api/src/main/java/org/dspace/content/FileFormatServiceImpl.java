package org.dspace.content;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import org.dspace.content.exception.InvalidFileFormatException;
import java.util.Arrays;
import java.util.List;

import org.apache.tika.Tika;
import org.apache.commons.io.IOUtils;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.dspace.content.service.FileFormatService;
import org.springframework.stereotype.Service;

@Service
public class FileFormatServiceImpl implements FileFormatService {

	@Autowired
	private ConfigurationService configurationService;
	
	/*
     * CONFIG METHODS (REPLACES STATIC LISTS)
     */

	private String getAcceptedFormats() {
        return configurationService.getProperty("webui.upload.accept.format");
    }

    public List<String> getPdfFormats() {
        return Arrays.asList(configurationService.getArrayProperty("webui.upload.accept.format.pdf"));
    }

    public List<String> getImageFormats() {
        return Arrays.asList(configurationService.getArrayProperty("webui.upload.accept.format.image"));
    }

    public List<String> getTiffFormats() {
        return Arrays.asList(configurationService.getArrayProperty("webui.upload.accept.format.image.tif"));
    }

    public List<String> getEmailFormats() {
        return Arrays.asList(configurationService.getArrayProperty("webui.upload.accept.format.email"));
    }

    public List<String> getOfficeFormats() {
        return Arrays.asList(configurationService.getArrayProperty("webui.upload.accept.format.office"));
    }

    public List<String> getHtmlFormats() {
        return Arrays.asList(configurationService.getArrayProperty("webui.upload.accept.format.html"));
    }

    public List<String> getTxtFormats() {
        return Arrays.asList(configurationService.getArrayProperty("webui.upload.accept.format.txt"));
    }

    public List<String> getAudioFormats() {
        return Arrays.asList(configurationService.getArrayProperty("webui.upload.accept.format.audio"));
    }

    public List<String> getVideoFormats() {
        return Arrays.asList(configurationService.getArrayProperty("webui.upload.accept.format.video"));
    }
	
    /*
     * Equivalent to:
     * checkIfValidFile(File fileToCheck)
     *
     * This method:
     * 1. Reads file into byte array
     * 2. Calls byte[] version
     */
    public String checkIfValidFile(File fileToCheck)
            throws IOException, InvalidFileFormatException {

        // Use try-with-resources to auto-close stream
        try (InputStream inputStream = new FileInputStream(fileToCheck)) {

            // Convert file -> byte array
            byte[] bytes = IOUtils.toByteArray(inputStream);

            // Call main method
            return checkIfValidFile(bytes);
        }
    }

    /*
     * Equivalent to:
     * checkIfValidFile(File fileToCheck, String formatList)
     *
     * This method:
     * 1. Reads file into byte array
     * 2. Calls byte[] + formatList version
     */
    public String checkIfValidFile(File fileToCheck, String formatList)
            throws IOException, InvalidFileFormatException {

        // Use try-with-resources to auto-close stream
        try (InputStream inputStream = new FileInputStream(fileToCheck)) {

            // Convert file -> byte array
            byte[] bytes = IOUtils.toByteArray(inputStream);

            // Call main method with custom formats
            return checkIfValidFile(bytes, formatList);
        }
    }

    /*
     * Equivalent to:
     * checkIfValidFile(byte[] bytes)
     *
     * This method:
     * Uses default ACCEPTED_FORMATS
     */
    @Override
    public String checkIfValidFile(byte[] bytes) throws IOException, InvalidFileFormatException {

        // exact replacement of ACCEPTED_FILE_FORMATS in File
        return checkIfValidFile(bytes, getAcceptedFormats());
    }

    /*
     * Equivalent to:
     * checkIfValidFile(byte[] bytes, String formatList)
     * Steps:
     * 1. Detect file type using Apache Tika
     * 2. Compare with allowed formats
     * 3. If not allowed -> throw error
     * 4. If allowed -> return MIME type
     * 
     */
    public String checkIfValidFile(byte[] bytes, String formatList) throws IOException, InvalidFileFormatException {

        boolean allowed = false;     // flag to check if file is allowed
        String mediaType = null;     // detected file type (MIME type)

        if (formatList != null) {

            TikaInputStream tikaInputStream = null;
            Tika tika = new Tika();

            try {
                // Convert byte[] -> TikaInputStream
                tikaInputStream = TikaInputStream.get(bytes);

                // Detect MIME type (example: application/pdf)
                mediaType = tika.getDetector().detect(tikaInputStream, new Metadata()).toString();

                // Split allowed formats (comma separated)
                String[] allowedTypes = formatList.split(",");

                // Compare detected type with allowed list
                for (String type : allowedTypes) {

                    // trim() removes extra spaces
                    if (type.trim().equals(mediaType)) {
                        allowed = true;
                        break;
                    }
                }

                // If file type NOT allowed -> throw error
                if (!allowed) {
                	throw new InvalidFileFormatException(
                		    "File format " + mediaType + " is not specified in the accepted format list."
                		);
                }

            } finally {
                // Always close stream (important for memory)
                if (tikaInputStream != null) {
                    tikaInputStream.close();
                }
            }
        }

        // Return detected file type
        return mediaType;
    }
}