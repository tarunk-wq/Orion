package org.dspace.content.service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface PdfConverterService {

	File convertHtmlToPdf(byte[] fileBytes, File rootDirectory, String documentName) throws IOException;

    File convertTextToPdf(byte[] fileBytes, File tempDir, String documentName) throws IOException;

    File convertImageToPdf(byte[] fileBytes, File tempDir, String documentName) throws IOException;

    File convertTiffToPdf(byte[] fileBytes, File outputFile) throws IOException;

    Map<String, List<String>> convertEmailToPdf(byte[] fileBytes, File tempDir, String documentName) throws IOException;

    File convertOfficeToPdf(byte[] fileBytes, File tempDir, String documentName) throws IOException;
}