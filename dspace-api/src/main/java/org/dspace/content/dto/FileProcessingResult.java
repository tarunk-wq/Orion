package org.dspace.content.dto;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * This class replaces the legacy Object[] used in convertToPDF()
 *
 * Instead of using indexes like:
 * objArr[0], objArr[1], objArr[2]...
 *
 * We store everything in named variables
 */
public class FileProcessingResult {

    // Whether file was converted to PDF
    private boolean isConverted;

    // Flags to identify original file type
    private boolean isEml;
    private boolean isDoc;
    private boolean isHtml;
    private boolean isTxt;
    private boolean isImg;

    // Temporary file created after conversion
    private File tempFile;

    // InputStream used later to create Bitstream
    private InputStream inputStream;

    // Used only for EML files (stores attachments / metadata)
    private Map<String, List<String>> unconvertedFileMap = new HashMap<>();


    /*
     * GETTERS AND SETTERS
     */

    public boolean isConverted() {
        return isConverted;
    }

    public void setConverted(boolean converted) {
        isConverted = converted;
    }

    public boolean isEml() {
        return isEml;
    }

    public void setEml(boolean eml) {
        isEml = eml;
    }

    public boolean isDoc() {
        return isDoc;
    }

    public void setDoc(boolean doc) {
        isDoc = doc;
    }

    public boolean isHtml() {
        return isHtml;
    }

    public void setHtml(boolean html) {
        isHtml = html;
    }

    public boolean isTxt() {
        return isTxt;
    }

    public void setTxt(boolean txt) {
        isTxt = txt;
    }

    public boolean isImg() {
        return isImg;
    }

    public void setImg(boolean img) {
        isImg = img;
    }

    public File getTempFile() {
        return tempFile;
    }

    public void setTempFile(File tempFile) {
        this.tempFile = tempFile;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public Map<String, List<String>> getUnconvertedFileMap() {
        return unconvertedFileMap;
    }

    public void setUnconvertedFileMap(Map<String, List<String>> unconvertedFileMap) {
        this.unconvertedFileMap = unconvertedFileMap;
    }
}