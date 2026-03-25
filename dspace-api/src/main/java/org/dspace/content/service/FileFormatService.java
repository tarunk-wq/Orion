package org.dspace.content.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.dspace.content.exception.InvalidFileFormatException;

public interface FileFormatService {

    String checkIfValidFile(byte[] bytes)
            throws IOException, InvalidFileFormatException;
    
    String checkIfValidFile(File file) throws IOException, InvalidFileFormatException;
    
    List<String> getPdfFormats();

    List<String> getImageFormats();

    List<String> getTiffFormats();

    List<String> getEmailFormats();

    List<String> getOfficeFormats();

    List<String> getHtmlFormats();

    List<String> getTxtFormats();

    List<String> getAudioFormats();

    List<String> getVideoFormats();
}