package org.dspace.content;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.input.CharSequenceReader;
import org.apache.commons.text.WordUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.dspace.content.exception.InvalidFileFormatException;
import org.dspace.content.service.FileFormatService;
import org.dspace.content.service.PdfConverterService;
import org.dspace.services.ConfigurationService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.io.Files;

import cli.CommandLineParameters;
import mimeparser.MimeMessageConverter;

@Service
public class PdfConverterServiceImpl implements PdfConverterService {
	
	@Autowired
	private FileFormatService fileFormatService;
	
	@Autowired
	private ConfigurationService configurationService;

	// Common temp directory (same as processFile logic)
	private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");
	private static final Logger log = LoggerFactory.getLogger(PdfConverterServiceImpl.class);

	// HTML -> PDF CONFIG (LEGACY CONSTANTS)

	private static final String CHARSET_NAME = "UTF-8";
	private static final String VIEWPORT_SIZE = "2480x3508";
	private static final int CONVERSION_DPI = 300;
	private static final int IMAGE_QUALITY = 100;

	// HTML → PDF
	@Override
	public File convertHtmlToPdf(byte[] fileBytes, File rootDirectory, String documentName) throws IOException {

		// Extract file extension (example: "file.html" -> "html")
		String fileExtension = documentName.contains(".") ? documentName.substring(documentName.lastIndexOf('.') + 1)
				: "html";

		// Extract base name without extension (example: "file.html" -> "file")
		String baseName = documentName.contains(".") ? documentName.substring(0, documentName.lastIndexOf('.'))
				: documentName;

		// TEMP FILE (used internally for HTML processing)

		// This temporary HTML file will be passed to wkhtmltopdf
		File tmpHtml = File.createTempFile(
				"orghtml_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId(), ".html");

		OutputStream os = null;

		try {

			// STEP 1: CREATE ORIGINAL HTML FILE (LEGACY BEHAVIOR)

			// Save original uploaded HTML file in temp directory
			String htmlFileName = baseName + "_original." + fileExtension;

			File orgHtmlFile = new File(rootDirectory, htmlFileName);

			// Create file and log if already exists
			if (!orgHtmlFile.createNewFile()) {
				log.error(htmlFileName + " already exists.");
			}

			// Write raw HTML content into file
			os = new FileOutputStream(orgHtmlFile);
			os.write(fileBytes);

			// STEP 2: CREATE OUTPUT PDF FILE

			String htmlToPdfName;

			// If already PDF, keep same name
			if (fileExtension.equalsIgnoreCase("pdf")) {
				htmlToPdfName = documentName;
			} else {
				// Otherwise, convert to .pdf
				htmlToPdfName = baseName + ".pdf";
			}

			// Create final PDF file inside same directory
			File htmlPdfFile = new File(rootDirectory, htmlToPdfName);

			if (!htmlPdfFile.createNewFile()) {
				log.error(htmlToPdfName + " already exists.");
			}

			// STEP 3: CLEAN HTML USING JSOUP

			// Parse HTML content from byte array
			Document doc = Jsoup.parse(new ByteArrayInputStream(fileBytes), CHARSET_NAME, "");

			// Remove external images (images with src starting with http)
			// This avoids issues during PDF conversion
			Elements links = doc.select("img[src^=http]");
			for (Element link : links) {
				link.remove();
			}

			// Write cleaned HTML into temporary file
			Files.asCharSink(tmpHtml, Charset.forName(CHARSET_NAME)).write(doc.html());

			// STEP 4: CALL wkhtmltopdf (EXTERNAL TOOL)

			// Build command to execute wkhtmltopdf
			List<String> cmd = new ArrayList<>(Arrays.asList("wkhtmltopdf", "--viewport-size", VIEWPORT_SIZE,
					"--enable-local-file-access", "--dpi", String.valueOf(CONVERSION_DPI), "--image-quality",
					String.valueOf(IMAGE_QUALITY), "--encoding", CHARSET_NAME));

			// Input HTML file path
			cmd.add(tmpHtml.getAbsolutePath());

			// Output PDF file path
			cmd.add(htmlPdfFile.getAbsolutePath());

			// Execute external process
			ProcessBuilder pb = new ProcessBuilder(cmd);

			Process p = pb.start();

			// Wait until conversion is completed
			p.waitFor();

			// RETURN FINAL PDF FILE

			return htmlPdfFile;

		} catch (Exception ex) {

			// Log error and throw IOException
			log.error("Error in converting html to pdf", ex);
			throw new IOException("Error in html to pdf conversion");

		} finally {

			// CLEANUP

			// Delete temporary HTML file after conversion
			if (!tmpHtml.delete()) {
				tmpHtml.deleteOnExit();
			}

			// Close file stream
			if (os != null) {
				os.close();
			}
		}
	}

	// TEXT → PDF
	@Override
	public File convertTextToPdf(byte[] fileBytes, File tempDir, String documentName) throws IOException {

		// Extract file extension from filename (example: "file.txt" -> "txt")
		String fileExtension = documentName.contains(".") ? documentName.substring(documentName.lastIndexOf('.') + 1)
				: "txt";

		// Extract base name without extension (example: "file.txt" -> "file")
		String baseName = documentName.contains(".") ? documentName.substring(0, documentName.lastIndexOf('.'))
				: documentName;

		// STEP 1: Create ORIGINAL FILE (same as legacy createOrgFile)

		// This file stores the raw uploaded content before conversion
		File orgFile;

		try (OutputStream os = new FileOutputStream(
				orgFile = new File(tempDir, baseName + "_original." + fileExtension))) {

			// Create file in temp directory
			if (!orgFile.createNewFile()) {
				log.error(orgFile.getName() + " already exists.");
			}

			// Write decoded bytes into file
			os.write(fileBytes);
		}

		// STEP 2: Create OUTPUT PDF FILE (same as legacy logic)

		String pdfFileName;

		// If file is already PDF, keep same name
		if (fileExtension.equalsIgnoreCase("pdf")) {
			pdfFileName = documentName;
		}
		// Otherwise, convert name to .pdf
		else {
			pdfFileName = baseName + ".pdf";
		}

		// Create final PDF file inside same temp directory
		File convPdfFile = new File(tempDir, pdfFileName);

		if (!convPdfFile.createNewFile()) {
			log.error(pdfFileName + " already exists.");
		}

		// STEP 3: PDF CREATION LOGIC (MAIN CONVERSION PART)

		// Margin from top/bottom of page
		int marginHeight = 50;

		// X position (left margin)
		int xpoint = 25;

		// Y position (changes per line)
		int ypoint = 0;

		// Line counter
		int i = 0;

		// Font settings
		int fontSize = 10;
		PDFont font = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);

		// Max characters per line (for wrapping)
		int charPerLine = 75;

		// Space between lines
		int lineGapHeight = 15;

		// Create new PDF document
		PDDocument doc = new PDDocument();

		// Set page size (A4)
		float pageWidth = PDRectangle.A4.getWidth();
		float pageHeight = PDRectangle.A4.getHeight();

		PDPage page = new PDPage(new PDRectangle(pageWidth, pageHeight));

		// Add first page to document
		doc.addPage(page);

		// Content stream to write text into PDF
		PDPageContentStream contents = new PDPageContentStream(doc, page);

		contents.setFont(font, fontSize);
		contents.setNonStrokingColor(Color.black);

		BufferedReader br = null;

		try {
			// Convert byte array into readable text
			br = new BufferedReader(new CharSequenceReader(new String(fileBytes)));

			String strLine;

			// Read file line by line
			while ((strLine = br.readLine()) != null) {

				// Wrap long lines into multiple lines (max 75 chars)
				String[] wrT = WordUtils.wrap(strLine, charPerLine, "\n", true).split("\\r?\\n");

				// Loop through wrapped lines
				for (int j = 0; j < wrT.length; j++) {

					// Calculate Y position for current line
					ypoint = (int) pageHeight - (marginHeight + (i * lineGapHeight));

					// If page is full → create new page
					if (ypoint <= marginHeight) {

						contents.close();

						page = new PDPage(new PDRectangle(pageWidth, pageHeight));
						doc.addPage(page);

						contents = new PDPageContentStream(doc, page);
						contents.setFont(font, fontSize);
						contents.setNonStrokingColor(Color.black);

						// Reset line counter for new page
						i = 0;

						ypoint = (int) pageHeight - (marginHeight + (i * lineGapHeight));
					}

					i++; // Move to next line

					contents.beginText();

					// Set position where text will be written
					contents.newLineAtOffset(xpoint, ypoint);

					try {
						// Write text into PDF
						contents.showText(wrT[j]);

					} catch (Exception e) {

						// If unsupported characters found → handle manually
						log.error("Error in adding text in pdf " + wrT[j], e);

						// Replace unsupported characters with space
						for (int c = 0; c < wrT[j].length(); c++) {
							if (!WinAnsiEncoding.INSTANCE.contains(wrT[j].charAt(c))) {
								wrT[j] = wrT[j].replace(wrT[j].charAt(c), ' ');
							}
						}

						// Retry writing cleaned text
						contents.showText(wrT[j]);
					}

					contents.endText();
				}
			}

			// Close content stream after writing
			contents.close();

			// Save final PDF file
			doc.save(convPdfFile);

			// Close document
			doc.close();

		} finally {
			// Ensure reader is always closed
			if (br != null) {
				br.close();
			}
		}

		// Return final converted PDF file
		return convPdfFile;
	}

	// IMAGE -> PDF
	@Override
	public File convertImageToPdf(byte[] fileBytes, File tempDir, String documentName) throws IOException {

		// Extract file extension (example: "image.jpg" -> "jpg")
		String fileExtension = documentName.contains(".") ? documentName.substring(documentName.lastIndexOf('.') + 1)
				: "jpg";

		// Extract base name without extension (example: "image.jpg" -> "image")
		String baseName = documentName.contains(".") ? documentName.substring(0, documentName.lastIndexOf('.'))
				: documentName;

		// STEP 1: CREATE ORIGINAL IMAGE FILE (LEGACY BEHAVIOR)

		// This file stores the original uploaded image before conversion
		File orgFile;

		try (OutputStream os = new FileOutputStream(
				orgFile = new File(tempDir, baseName + "_original." + fileExtension))) {

			// Create file in temp directory
			if (!orgFile.createNewFile()) {
				log.error(orgFile.getName() + " already exists.");
			}

			// Write image bytes into file
			os.write(fileBytes);
		}

		// STEP 2: CREATE OUTPUT PDF FILE

		String pdfFileName;

		// If already PDF, keep same name
		if (fileExtension.equalsIgnoreCase("pdf")) {
			pdfFileName = documentName;
		} else {
			// Otherwise, convert name to .pdf
			pdfFileName = baseName + ".pdf";
		}

		// Create final PDF file
		File convPdfFile = new File(tempDir, pdfFileName);

		if (!convPdfFile.createNewFile()) {
			log.error(pdfFileName + " already exists.");
		}

		// STEP 3: CREATE PDF DOCUMENT FROM IMAGE

		// Create new PDF document
		PDDocument doc = new PDDocument();

		// Convert image bytes into a PDFBox image object
		PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, fileBytes, convPdfFile.getName());

		// STEP 4: ADD IMAGE TO PDF WITH PROPER SCALING (LEGACY LOGIC)

		// Get original image dimensions
		float imageHeight = pdImage.getHeight();
		float imageWidth = pdImage.getWidth();

		// Default page size is A4
		float finalPageWidth = PDRectangle.A4.getWidth();
		float finalPageHeight = PDRectangle.A4.getHeight();

		// Maintain aspect ratio of image
		if (imageHeight > imageWidth) {
			finalPageWidth = (imageWidth / imageHeight) * finalPageHeight;
		} else {
			finalPageHeight = (imageHeight / imageWidth) * finalPageWidth;
		}

		// If image is larger than page, scale it down using DPI logic
		if (imageHeight > finalPageHeight || imageWidth > finalPageWidth) {

			float finalPageWidthInch = finalPageWidth / 72.0f;
			float finalPageHeightInch = finalPageHeight / 72.0f;

			float imageDpi;

			if (imageHeight > imageWidth) {
				imageDpi = imageHeight / finalPageHeightInch;
			} else {
				imageDpi = imageWidth / finalPageWidthInch;
			}

			// Resize image to fit page
			imageWidth = (imageWidth / imageDpi) * 72.0f;
			imageHeight = (imageHeight / imageDpi) * 72.0f;
		}

		// Calculate position to center the image on page
		float startingXpoint = (finalPageWidth / 2 - imageWidth / 2);
		float startingYpoint = (finalPageHeight / 2 - imageHeight / 2);

		// Create PDF page with calculated size
		PDPage page = new PDPage(new PDRectangle(finalPageWidth, finalPageHeight));
		doc.addPage(page);

		// Draw image onto the PDF page
		PDPageContentStream contents = new PDPageContentStream(doc, page);
		contents.drawImage(pdImage, startingXpoint, startingYpoint, imageWidth, imageHeight);
		contents.close();

		// STEP 5: SAVE AND CLOSE PDF

		// Save the final PDF file
		doc.save(convPdfFile);

		// Close document to free resources
		doc.close();

		// Return generated PDF file
		return convPdfFile;
	}

	// TIFF -> PDF (DIRECT)
	@Override
	public File convertTiffToPdf(byte[] fileBytes, File outputFile) throws IOException {

		ImageInputStream is = null;
		ImageReader imageReader = null;

		try {

			// Create image input stream from byte array
			is = ImageIO.createImageInputStream(new ByteArrayInputStream(fileBytes));

			// Get TIFF reader
			imageReader = ImageIO.getImageReaders(is).next();
			imageReader.setInput(is);

			// Create PDF document
			PDDocument doc = new PDDocument();

			int totalPages = imageReader.getNumImages(true);

			// Loop through all TIFF pages
			for (int pageNum = 0; pageNum < totalPages; pageNum++) {

				BufferedImage bim = imageReader.read(pageNum);

				// Convert each image page into PDF image
				PDImageXObject pdImage = LosslessFactory.createFromImage(doc, bim);

				// Reuse SAME scaling logic from imageToPDF
				float imageHeight = pdImage.getHeight();
				float imageWidth = pdImage.getWidth();

				float finalPageWidth = PDRectangle.A4.getWidth();
				float finalPageHeight = PDRectangle.A4.getHeight();

				if (imageHeight > imageWidth) {
					finalPageWidth = (imageWidth / imageHeight) * finalPageHeight;
				} else {
					finalPageHeight = (imageHeight / imageWidth) * finalPageWidth;
				}

				if (imageHeight > finalPageHeight || imageWidth > finalPageWidth) {

					float finalPageWidthInch = finalPageWidth / 72.0f;
					float finalPageHeightInch = finalPageHeight / 72.0f;

					float imageDpi;

					if (imageHeight > imageWidth) {
						imageDpi = imageHeight / finalPageHeightInch;
					} else {
						imageDpi = imageWidth / finalPageWidthInch;
					}

					imageWidth = (imageWidth / imageDpi) * 72.0f;
					imageHeight = (imageHeight / imageDpi) * 72.0f;
				}

				float startingXpoint = (finalPageWidth / 2 - imageWidth / 2);
				float startingYpoint = (finalPageHeight / 2 - imageHeight / 2);

				PDPage page = new PDPage(new PDRectangle(finalPageWidth, finalPageHeight));
				doc.addPage(page);

				PDPageContentStream contents = new PDPageContentStream(doc, page);
				contents.drawImage(pdImage, startingXpoint, startingYpoint, imageWidth, imageHeight);
				contents.close();
			}

			// Save final PDF
			doc.save(outputFile);
			doc.close();

			return outputFile;

		} finally {

			if (is != null) {
				is.close();
			}

			if (imageReader != null) {
				imageReader.dispose();
			}
		}
	}

	// EMAIL -> PDF
	@Override
	public Map<String, List<String>> convertEmailToPdf(byte[] fileBytes, File tempDir, String documentName)
	        throws IOException {

	    // Temporary folder where extracted email attachments will be stored
	    File tempEmailAttachmentDest = null;

	    // Extract file extension (example: "mail.eml" -> "eml")
	    String fileExtension = documentName.contains(".")
	            ? documentName.substring(documentName.lastIndexOf('.') + 1)
	            : "eml";

	    // Extract base name (example: "mail.eml" -> "mail")
	    String baseName = documentName.contains(".")
	            ? documentName.substring(0, documentName.lastIndexOf('.'))
	            : documentName;

	    try {

	        // STEP 1: SAVE ORIGINAL EMAIL FILE

	        // Create file like: mail_original.eml
	        String originalFileName = baseName + "_original." + fileExtension;
	        File originalEmail = new File(tempDir, originalFileName);

	        if (!originalEmail.createNewFile()) {
	            log.error(originalFileName + " already exists!");
	        }

	        // Write email bytes into file
	        try (OutputStream os = new FileOutputStream(originalEmail)) {
	            os.write(fileBytes);
	        }

	        // STEP 2: CONVERT EMAIL BODY TO PDF

	        // Create output file for email body
	        String destinationFileName = baseName + "_body.pdf";
	        File destinationFile = new File(tempDir, destinationFileName);

	        if (!destinationFile.createNewFile()) {
	            log.error(destinationFileName + " file already exists!");
	        }

	        // Enable attachment extraction
	        boolean extractAttachment = true;

	        // Create temp folder to store extracted attachments
	        tempEmailAttachmentDest = new File(tempDir, "tempEmailAttachmentDest");
	        if (!tempEmailAttachmentDest.exists()) {
	            tempEmailAttachmentDest.mkdir();
	        }

	        // Helper class for command parameters (legacy logic)
	        CommandLineParameters cli = new CommandLineParameters();

	        // Extra parameters passed to converter
	        List<String> extParams = new ArrayList<>();
	        extParams.add("--page-size");
	        extParams.add(cli.getPageSize());

	        try {
	            // Convert email body -> PDF AND extract attachments
	            MimeMessageConverter.convertToPdf(
	                    originalEmail.getAbsolutePath(),
	                    destinationFile.getAbsolutePath(),
	                    cli.isHideHeaders(),
	                    extractAttachment,
	                    tempEmailAttachmentDest.getAbsolutePath(),
	                    extParams
	            );
	        } catch (Exception e) {
	            log.error("Error in email to pdf conversion for: " + documentName, e);
	            throw new IOException();
	        }

	        // STEP 3: PROCESS ATTACHMENTS

	        // Convert all attachments to PDF if possible
	        return convertAttachmentToPdf(tempEmailAttachmentDest, documentName, tempDir);

	    } finally {

	        // Cleanup: delete temporary attachment folder
	        if (tempEmailAttachmentDest != null && tempEmailAttachmentDest.exists()) {
	            FileUtils.deleteQuietly(tempEmailAttachmentDest);
	        }
	    }
	}

	private Map<String, List<String>> convertAttachmentToPdf(File tempEmailAttachmentDest,
	        String documentName, File rootDirectory) throws IOException {

	    // Counter to number attachments (attachment_1, attachment_2, etc.)
	    int attachmentIndex = 1;

	    // Stores details of files that could NOT be converted
	    Map<String, List<String>> unConvertedFileDetailsMap = new HashMap<>();

	    // Loop through all extracted attachment files
	    for (File file : tempEmailAttachmentDest.listFiles()) {

	        // Default PDF name for attachment
	        String attachmentFileName = documentName + "_attachment_" + attachmentIndex + ".pdf";

	        String fileName = file.getName(); // original file name
	        String fileExtension = FilenameUtils.getExtension(fileName);

	        try {

	            // Detect MIME type using FileFormatService
	            String mimeType = fileFormatService.checkIfValidFile(file);

	            // Create output PDF file
	            File attachmentPdfFile = new File(rootDirectory, attachmentFileName);

	            if (!attachmentPdfFile.createNewFile()) {
	                log.error(attachmentFileName + " already exists !");
	            }

	            // HANDLE DIFFERENT FILE TYPES

	            switch (mimeType) {

	                // If already PDF -> just copy
	                case "application/pdf":
	                    Files.copy(file, attachmentPdfFile);
	                    break;

	                // Image files -> convert using Image -> PDF logic
	                case "image/jpeg":
	                case "image/png":
	                case "image/gif":
	                case "image/bmp":
	                case "image/tiff":

	                    convertImageToPdf(
	                            FileUtils.readFileToByteArray(file),
	                            rootDirectory,
	                            fileName
	                    );
	                    break;

	                // Office files -> convert using Office -> PDF logic
	                case "application/x-tika-ooxml":
	                case "application/x-tika-msoffice":
	                case "application/msword":
	                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document":

	                    // Special case: MSG files not supported here
	                    if (fileExtension.equalsIgnoreCase("msg")) {
	                        throw new InvalidFileFormatException("File format " + mimeType + " not supported");
	                    }

	                    convertOfficeToPdf(FileUtils.readFileToByteArray(file), rootDirectory, fileName);
	                    break;

	                // Unsupported formats -> throw exception
	                default:
	                    throw new InvalidFileFormatException("File format " + mimeType + " not supported");
	            }

	        } catch (InvalidFileFormatException ex) {

	            // HANDLE UNSUPPORTED FILES

	            String mimeType = ex.getMessage();

	            log.error(mimeType + " not supported as attachment of: " + documentName);

	            // Save original file instead of converting
	            String attachmentFileNameRaw = documentName + "_attachment_" + attachmentIndex + "." + fileExtension;

	            File attachmentFile = new File(rootDirectory, attachmentFileNameRaw);

	            if (!attachmentFile.createNewFile()) {
	                log.error(attachmentFileNameRaw + " already exists.");
	            }

	            // Copy original file
	            Files.copy(file, attachmentFile);

	            // Store details in map
	            List<String> details = new ArrayList<>();
	            details.add(fileName);     // original name
	            details.add(mimeType);     // reason (unsupported type)

	            unConvertedFileDetailsMap.put(attachmentFileNameRaw, details);
	        }

	        // Move to next attachment
	        attachmentIndex++;
	    }

	    // Return all unconverted attachment details
	    return unConvertedFileDetailsMap;
	}

	// OFFICE -> PDF
	@Override
	public File convertOfficeToPdf(byte[] fileBytes, File tempDir, String documentName) throws IOException {

	    // STEP 1: EXTRACT FILE DETAILS

	    // Get file extension (example: "file.docx" -> "docx")
	    String fileExtension = documentName.contains(".")
	            ? documentName.substring(documentName.lastIndexOf('.') + 1)
	            : "doc";

	    // Get file name without extension (example: "file.docx" -> "file")
	    String baseName = documentName.contains(".")
	            ? documentName.substring(0, documentName.lastIndexOf('.'))
	            : documentName;


	    // STEP 2: CREATE ORIGINAL FILE (same as legacy behavior)

	    // This file stores the uploaded Office file before conversion
	    File orgFile;

	    // try-with-resources automatically closes the stream after writing
	    try (OutputStream os = new FileOutputStream(
	            orgFile = new File(tempDir, baseName + "_original." + fileExtension))) {

	        // Create file in temp directory
	        if (!orgFile.createNewFile()) {
	            log.error(orgFile.getName() + " already exists.");
	        }

	        // Write byte data into file
	        os.write(fileBytes);
	    }


	    // STEP 3: CREATE OUTPUT PDF FILE

	    String pdfFileName;

	    // If file is already PDF, keep same name
	    if (fileExtension.equalsIgnoreCase("pdf")) {
	        pdfFileName = documentName;
	    } else {
	        // Otherwise, change extension to .pdf
	        pdfFileName = baseName + ".pdf";
	    }

	    // Create output PDF file in temp directory
	    File officePdfFile = new File(tempDir, pdfFileName);

	    if (!officePdfFile.createNewFile()) {
	        log.error(pdfFileName + " already exists.");
	    }


	    // STEP 4: CALL EXTERNAL CONVERSION SERVICE

	    try {

	        // Get conversion service URL from configuration (not hardcoded)
	        String requestURL = configurationService.getProperty("request.url");

	        // Create HTTP client
	        HttpClient client = HttpClientBuilder.create().build();

	        // Build request URL with parameters
	        URIBuilder builder = new URIBuilder(requestURL);

	        // "src" = path of original Office file
	        builder.setParameter("src", orgFile.getAbsolutePath());

	        // "dest" = path where converted PDF should be saved
	        builder.setParameter("dest", officePdfFile.getAbsolutePath());

	        // Create HTTP GET request
	        HttpGet request = new HttpGet(builder.build());

	        // Send request to external service
	        HttpResponse response = client.execute(request);

	        log.info("Completed get request execution.");

	        // Check if conversion was successful (HTTP 200 OK)
	        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
	            throw new IOException("Error in office to pdf conversion");
	        }

	    } catch (Exception e) {

	        // Log error and throw exception if conversion fails
	        log.error("Error in office to pdf conversion", e);
	        throw new IOException("Error in office to pdf conversion");
	    }


	    // STEP 5: RETURN FINAL PDF FILE

	    return officePdfFile;
	}
	
}