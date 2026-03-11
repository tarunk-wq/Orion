/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.dspace.audittrail.AuditAction;
import org.dspace.audittrail.factory.AuditTrailServiceFactory;
import org.dspace.audittrail.service.AuditTrailService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.bitstore.DSBitStoreService;
import org.dspace.userbitstream.UserBitstream;
import org.dspace.userbitstream.factory.UserBitstreamFactory;
import org.dspace.userbitstream.service.UserBitstreamService;
import org.dspace.utils.DSpace;
import org.dspace.versioning.VersionBitstream;
import org.dspace.versioning.factory.VersioningBitstreamServiceFactory;
import org.dspace.versioning.service.VersioningBitstreamService;
import org.springframework.beans.factory.annotation.Autowired;

import com.ibm.icu.text.SimpleDateFormat;
import com.lowagie.text.pdf.PdfReader;


/**
 * Miscellaneous utility methods
 *
 * @author Robert Tansley
 * @author Mark Diggory
 */
public class Util {
    // cache for source version result
    private static String sourceVersion = null;
    public static final String DC_SCHEMA = "dc";

    @Autowired
   	static ItemService itemService = ContentServiceFactory.getInstance().getItemService();

   @Autowired
   private static AuditTrailService auditTrailService;
   
   @Autowired(required = true)
   protected static BitstreamService bitstreamService;
   
   @Autowired(required = true)
   protected static UserBitstreamService userBitstreamService;
   
   @Autowired(required = true)
   private static VersioningBitstreamService versioningBitstreamService;

       
    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger();

    /**
     * Default constructor. Must be protected as org.dspace.xmlworkflow.WorkflowUtils extends it
     */
    protected Util() { }

    /**
     * Utility method to convert spaces in a string to HTML non-break space
     * elements.
     *
     * @param s string to change spaces in
     * @return the string passed in with spaces converted to HTML non-break
     * spaces
     */
    public static String nonBreakSpace(String s) {
        StringBuilder newString = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            if (ch == ' ') {
                newString.append("&nbsp;");
            } else {
                newString.append(ch);
            }
        }

        return newString.toString();
    }

    /**
     * Encode a bitstream name for inclusion in a URL in an HTML document. This
     * differs from the usual URL-encoding, since we want pathname separators to
     * be passed through verbatim; this is required so that relative paths in
     * bitstream names and HTML references work correctly.
     * <P>
     * If the link to a bitstream is generated with the pathname separators
     * escaped (e.g. "%2F" instead of "/") then the Web user agent perceives it
     * to be one pathname element, and relative URI paths within that document
     * containing ".." elements will be handled incorrectly.
     * <P>
     *
     * @param stringIn input string to encode
     * @param encoding character encoding, e.g. UTF-8
     * @return the encoded string
     * @throws java.io.UnsupportedEncodingException if encoding error
     */
    public static String encodeBitstreamName(String stringIn, String encoding)
        throws java.io.UnsupportedEncodingException {
        // FIXME: This should be moved elsewhere, as it is used outside the UI
        if (stringIn == null) {
            return "";
        }

        StringBuilder out = new StringBuilder();

        final String[] pctEncoding = {"%00", "%01", "%02", "%03", "%04",
            "%05", "%06", "%07", "%08", "%09", "%0a", "%0b", "%0c", "%0d",
            "%0e", "%0f", "%10", "%11", "%12", "%13", "%14", "%15", "%16",
            "%17", "%18", "%19", "%1a", "%1b", "%1c", "%1d", "%1e", "%1f",
            "%20", "%21", "%22", "%23", "%24", "%25", "%26", "%27", "%28",
            "%29", "%2a", "%2b", "%2c", "%2d", "%2e", "%2f", "%30", "%31",
            "%32", "%33", "%34", "%35", "%36", "%37", "%38", "%39", "%3a",
            "%3b", "%3c", "%3d", "%3e", "%3f", "%40", "%41", "%42", "%43",
            "%44", "%45", "%46", "%47", "%48", "%49", "%4a", "%4b", "%4c",
            "%4d", "%4e", "%4f", "%50", "%51", "%52", "%53", "%54", "%55",
            "%56", "%57", "%58", "%59", "%5a", "%5b", "%5c", "%5d", "%5e",
            "%5f", "%60", "%61", "%62", "%63", "%64", "%65", "%66", "%67",
            "%68", "%69", "%6a", "%6b", "%6c", "%6d", "%6e", "%6f", "%70",
            "%71", "%72", "%73", "%74", "%75", "%76", "%77", "%78", "%79",
            "%7a", "%7b", "%7c", "%7d", "%7e", "%7f", "%80", "%81", "%82",
            "%83", "%84", "%85", "%86", "%87", "%88", "%89", "%8a", "%8b",
            "%8c", "%8d", "%8e", "%8f", "%90", "%91", "%92", "%93", "%94",
            "%95", "%96", "%97", "%98", "%99", "%9a", "%9b", "%9c", "%9d",
            "%9e", "%9f", "%a0", "%a1", "%a2", "%a3", "%a4", "%a5", "%a6",
            "%a7", "%a8", "%a9", "%aa", "%ab", "%ac", "%ad", "%ae", "%af",
            "%b0", "%b1", "%b2", "%b3", "%b4", "%b5", "%b6", "%b7", "%b8",
            "%b9", "%ba", "%bb", "%bc", "%bd", "%be", "%bf", "%c0", "%c1",
            "%c2", "%c3", "%c4", "%c5", "%c6", "%c7", "%c8", "%c9", "%ca",
            "%cb", "%cc", "%cd", "%ce", "%cf", "%d0", "%d1", "%d2", "%d3",
            "%d4", "%d5", "%d6", "%d7", "%d8", "%d9", "%da", "%db", "%dc",
            "%dd", "%de", "%df", "%e0", "%e1", "%e2", "%e3", "%e4", "%e5",
            "%e6", "%e7", "%e8", "%e9", "%ea", "%eb", "%ec", "%ed", "%ee",
            "%ef", "%f0", "%f1", "%f2", "%f3", "%f4", "%f5", "%f6", "%f7",
            "%f8", "%f9", "%fa", "%fb", "%fc", "%fd", "%fe", "%ff"};

        byte[] bytes = stringIn.getBytes(encoding);

        for (int i = 0; i < bytes.length; i++) {
            // Any unreserved char or "/" goes through unencoded
            if ((bytes[i] >= 'A' && bytes[i] <= 'Z')
                || (bytes[i] >= 'a' && bytes[i] <= 'z')
                || (bytes[i] >= '0' && bytes[i] <= '9') || bytes[i] == '-'
                || bytes[i] == '.' || bytes[i] == '_' || bytes[i] == '~'
                || bytes[i] == '/') {
                out.append((char) bytes[i]);
            } else if (bytes[i] >= 0) {
                // encode other chars (byte code < 128)
                out.append(pctEncoding[bytes[i]]);
            } else {
                // encode other chars (byte code > 127, so it appears as
                // negative in Java signed byte data type)
                out.append(pctEncoding[256 + bytes[i]]);
            }
        }
        log.debug("encoded \"" + stringIn + "\" to \"" + out.toString() + "\"");

        return out.toString();
    }

    /**
     * Version of encodeBitstreamName with one parameter, uses default encoding
     * <P>
     *
     * @param stringIn input string to encode
     * @return the encoded string
     * @throws java.io.UnsupportedEncodingException if encoding error
     */
    public static String encodeBitstreamName(String stringIn) throws java.io.UnsupportedEncodingException {
        return encodeBitstreamName(stringIn, Constants.DEFAULT_ENCODING);
    }

    /**
     * Formats the file size. Examples:
     *
     * - 50 = 50B
     * - 1024 = 1KB
     * - 1,024,000 = 1MB etc
     *
     * The numbers are formatted using java Locales
     *
     * @param in The number to convert
     * @return the file size as a String
     */
    public static String formatFileSize(double in) {
        // Work out the size of the file, and format appropriately
        // FIXME: When full i18n support is available, use the user's Locale
        // rather than the default Locale.
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.getDefault());
        DecimalFormat df = (DecimalFormat) nf;
        df.applyPattern("###,###.##");
        if (in < 1024) {
            df.applyPattern("0");
            return df.format(in) + " " + "B";
        } else if (in < 1024000) {
            in = in / 1024;
            return df.format(in) + " " + "kB";
        } else if (in < 1024000000) {
            in = in / 1024000;
            return df.format(in) + " " + "MB";
        } else {
            in = in / 1024000000;
            return df.format(in) + " " + "GB";
        }
    }

    /**
     * Obtain a parameter from the given request as an int. <code>-1</code> is
     * returned if the parameter is garbled or does not exist.
     *
     * @param request the HTTP request
     * @param param   the name of the parameter
     * @return the integer value of the parameter, or -1
     */
    public static int getIntParameter(HttpServletRequest request, String param) {
        String val = request.getParameter(param);

        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            // Problem with parameter
            return -1;
        }
    }

    /**
     * Obtain a parameter from the given request as a UUID. <code>null</code> is
     * returned if the parameter is garbled or does not exist.
     *
     * @param request the HTTP request
     * @param param   the name of the parameter
     * @return the integer value of the parameter, or -1
     */
    public static UUID getUUIDParameter(HttpServletRequest request, String param) {
        String val = request.getParameter(param);
        if (StringUtils.isEmpty(val)) {
            return null;
        }

        try {
            return UUID.fromString(val.trim());
        } catch (Exception e) {
            // at least log this error to make debugging easier
            // do not silently return null only.
            log.warn("Unable to recognize UUID from String \""
                         + val + "\". Will return null.", e);
            // Problem with parameter
            return null;
        }
    }

    /**
     * Obtain a List of UUID parameters from the given request as an UUID. null
     * is returned if parameter doesn't exist. <code>null</code> is returned in
     * position of the list if that particular value is garbled.
     *
     * @param request the HTTP request
     * @param param   the name of the parameter
     * @return list of UUID or null
     */
    public static List<UUID> getUUIDParameters(HttpServletRequest request,
                                               String param) {
        String[] request_values = request.getParameterValues(param);

        if (request_values == null) {
            return null;
        }

        List<UUID> return_values = new ArrayList<>(request_values.length);

        for (String s : request_values) {
            try {
                return_values.add(UUID.fromString(s.trim()));
            } catch (Exception e) {
                // Problem with parameter, stuff null in the list
                return_values.add(null);
            }
        }

        return return_values;
    }


    /**
     * Obtain an array of int parameters from the given request as an int. null
     * is returned if parameter doesn't exist. <code>-1</code> is returned in
     * array locations if that particular value is garbled.
     *
     * @param request the HTTP request
     * @param param   the name of the parameter
     * @return array of integers or null
     */
    public static int[] getIntParameters(HttpServletRequest request,
                                         String param) {
        String[] request_values = request.getParameterValues(param);

        if (request_values == null) {
            return null;
        }

        int[] return_values = new int[request_values.length];

        for (int x = 0; x < return_values.length; x++) {
            try {
                return_values[x] = Integer.parseInt(request_values[x]);
            } catch (Exception e) {
                // Problem with parameter, stuff -1 in this slot
                return_values[x] = -1;
            }
        }

        return return_values;
    }

    /**
     * Obtain a parameter from the given request as a boolean.
     * <code>false</code> is returned if the parameter is garbled or does not
     * exist.
     *
     * @param request the HTTP request
     * @param param   the name of the parameter
     * @return the integer value of the parameter, or -1
     */
    public static boolean getBoolParameter(HttpServletRequest request,
                                           String param) {
        return ((request.getParameter(param) != null) && request.getParameter(
            param).equals("true"));
    }

    /**
     * Get the button the user pressed on a submitted form. All buttons should
     * start with the text <code>submit</code> for this to work. A default
     * should be supplied, since often the browser will submit a form with no
     * submit button pressed if the user presses enter.
     *
     * @param request the HTTP request
     * @param def     the default button
     * @return the button pressed
     */
    public static String getSubmitButton(HttpServletRequest request, String def) {
        Enumeration e = request.getParameterNames();

        while (e.hasMoreElements()) {
            String parameterName = (String) e.nextElement();

            if (parameterName.startsWith("submit")) {
                return parameterName;
            }
        }

        return def;
    }

    /**
     * Gets Maven version string of the source that built this instance.
     *
     * @return string containing version, e.g. "1.5.2"; ends in "-SNAPSHOT" for development versions.
     */
    public static String getSourceVersion() {
        if (sourceVersion == null) {
            Properties constants = new Properties();

            InputStream cis = null;
            try {
                cis = Util.class.getResourceAsStream("/META-INF/maven/org.dspace/dspace-api/pom.properties");
                if (cis == null) {
                    // pom.properties will not exist when running tests
                    return "unknown";
                }
                constants.load(cis);
            } catch (Exception e) {
                log.error("Could not open dspace-api's pom.properties", e);
            } finally {
                if (cis != null) {
                    try {
                        cis.close();
                    } catch (IOException e) {
                        log.error("Unable to close input stream", e);
                    }
                }
            }

            sourceVersion = constants.getProperty("version", "none");
        }
        return sourceVersion;
    }

    /**
     * Get a list of all the respective "displayed-value(s)" from the given
     * "stored-value(s)" for a specific metadata field of a DSpace Item, by
     * reading submission-forms.xml
     *
     * @param item      The Dspace Item
     * @param values    A Metadatum[] array of the specific "stored-value(s)"
     * @param schema    A String with the schema name of the metadata field
     * @param element   A String with the element name of the metadata field
     * @param qualifier A String with the qualifier name of the metadata field
     * @param locale    locale
     * @return A list of the respective "displayed-values"
     * @throws SQLException            if database error
     * @throws DCInputsReaderException if reader error
     */

    public static List<String> getControlledVocabulariesDisplayValueLocalized(
        Item item, List<MetadataValue> values, String schema, String element,
        String qualifier, Locale locale) throws SQLException,
        DCInputsReaderException {
        List<String> toReturn = new ArrayList<>();
        DCInput myInputs = null;
        boolean myInputsFound = false;
        String formFileName = I18nUtil.getInputFormsFileName(locale);

        Collection collection = item.getOwningCollection();

        // Read the input form file for the specific collection
        DCInputsReader inputsReader = new DCInputsReader(formFileName);

        List<DCInputSet> inputSets = inputsReader.getInputsByCollection(collection);

        // Replace the values of Metadatum[] with the correct ones in case
        // of
        // controlled vocabularies
        String currentField = Utils.standardize(schema, element, qualifier, ".");

        for (DCInputSet inputSet : inputSets) {

            if (inputSet != null) {

                int fieldsNums = inputSet.getNumberFields();

                for (int p = 0; p < fieldsNums; p++) {

                    DCInput[][] inputs = inputSet.getFields();

                    if (inputs != null) {

                        for (int i = 0; i < inputs.length; i++) {
                            for (int j = 0; j < inputs[i].length; j++) {
                                String inputField = Utils
                                    .standardize(inputs[i][j].getSchema(), inputs[i][j].getElement(),
                                                 inputs[i][j].getQualifier(), ".");
                                if (currentField.equals(inputField)) {
                                    myInputs = inputs[i][j];
                                    myInputsFound = true;
                                    break;
                                }
                            }
                        }
                    }
                    if (myInputsFound) {
                        break;
                    }
                }
            }

            if (myInputsFound) {

                for (MetadataValue value : values) {

                    String pairsName = myInputs.getPairsType();
                    String stored_value = value.getValue();
                    String displayVal = myInputs.getDisplayString(pairsName, stored_value);

                    if (displayVal != null && !"".equals(displayVal)) {

                        toReturn.add(displayVal);
                    }

                }
            }
        }
        return toReturn;
    }

    /**
     * Split a list in an array of i sub-lists uniformly sized.
     *
     * @param <T> type of objects in the list.
     * @param idsList the list to split
     * @param i the number of sublists to return
     *
     * @return an array of sub-lists of fixed size
     */
    public static <T> List<T>[] splitList(List<T> idsList, int i) {
        int setmin = idsList.size() / i;
        List<T>[] result = new List[i];
        int offset = 0;
        for (int idx = 0; idx < i - 1; idx++) {
            result[idx] = idsList.subList(offset, offset + setmin);
            offset += setmin;
        }
        result[i - 1] = idsList.subList(offset, idsList.size());
        return result;
    }

    public static List<String> differenceInSubmissionFields(Collection fromCollection, Collection toCollection)
        throws DCInputsReaderException {
        DCInputsReader reader = new DCInputsReader();
        List<DCInputSet> from = reader.getInputsByCollection(fromCollection);
        List<DCInputSet> to = reader.getInputsByCollection(toCollection);

        Set<String> fromFieldName = new HashSet<>();
        Set<String> toFieldName = new HashSet<>();
        for (DCInputSet ff : from) {
            for (DCInput[] fdcrow : ff.getFields()) {
                for (DCInput fdc : fdcrow) {
                    fromFieldName.add(fdc.getFieldName());
                }
            }
        }
        for (DCInputSet tt : to) {
            for (DCInput[] tdcrow : tt.getFields()) {
                for (DCInput tdc : tdcrow) {
                    toFieldName.add(tdc.getFieldName());
                }
            }
        }

        return ListUtils.removeAll(fromFieldName, toFieldName);
    }
    
    public static String processBitstream(Context context, Item item, String inputFilePath, String outputFilePath) throws IOException {
		List<String> metadataList = Arrays.asList(DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("metadata.list"));
		List<String> metadataValues = new ArrayList<String>();
		List<String> userFriendlyNames = new ArrayList<String>();
		for (String metadata : metadataList) {
			String[] metadataArray = metadata.split("\\.");
			int metadataArrayLength = metadataArray.length;
			String element = metadataArray[1];
			String qualifier = (metadataArrayLength > 2 ? metadataArray[1] : null);
			List<MetadataValue> metadataValue = itemService.getMetadata(item, DC_SCHEMA, element, qualifier, Item.ANY);
			if (!metadataValue.isEmpty()) {
				String itemMetadataValue = processMetadataValue(metadataValue);
				String userFriendlyName = I18nUtil.getMessage(
						"metadata." + "dc" + "." + element + ((qualifier != null) ? "." + qualifier : ""), context);
				metadataValues.add(itemMetadataValue);
				userFriendlyNames.add(userFriendlyName);
			}
		}
		boolean isEmbedMetadataAndSavePDFA = embedMetadataAndSavePDFA(inputFilePath,outputFilePath,metadataValues,userFriendlyNames);
		if(isEmbedMetadataAndSavePDFA) {
			log.info("Embedding Successful in " + outputFilePath);
			return outputFilePath;
		} else {
			 log.error("Failed to embed metadata into PDF at " + inputFilePath);
			 return null;
		}
	}
    
    
    
    private static String processMetadataValue(List<MetadataValue> metadataValue) {
    	StringBuilder sb = new StringBuilder();
    	for(MetadataValue mv : metadataValue) {
    		sb.append(mv.getValue());
    		sb.append(" | ");
    	}
        sb.setLength(sb.length() - 3);
        log.info("MetadataValue:: " + sb.toString());
        return sb.toString();
	}

	private static boolean embedMetadataAndSavePDFA(String inputFilePath, String outputFilePath, List<String> metadataValues, List<String> userFriendlyNames) {
		String embedJarPath = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("embed.conformance");
		if (embedJarPath == null || !new File(embedJarPath).exists()) {
			log.error("Embed JAR path is invalid or does not exist: " + embedJarPath);
			return false;
		}
    	String arg0 = inputFilePath;
		String arg1 = outputFilePath;
		String arg2 = Util.class.getResource("/sRGB.profile").getPath();
		String arg3 = String.join(",", metadataValues);
		String arg4 = String.join(",", userFriendlyNames);
		boolean processExecuted = false;
    	String command= "java -jar \"" + embedJarPath + "\" \"" + arg0 + "\" \"" + arg1 + "\" \"" + arg2 + "\" \"" + arg3 + "\" \"" + arg4 + "\"";
        log.info("Command:: " + command);
    	try {
    		Process process = Runtime.getRuntime().exec(command);
            Thread errThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						IOUtils.copy(process.getErrorStream(), System.err);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				}
			});
			// wait till completion
			errThread.start();
			int exitCode = process.waitFor();
			processExecuted = true;
			log.info("Process exited with code "+ exitCode);
		} catch (Exception e) {
			log.error("Error running itext jar for signing",e);
		}
        return processExecuted;
    }

 	 public static boolean isInputStreamNotEmpty(BufferedInputStream bufferInputstream) {
         try {
        	 bufferInputstream.mark(1);
        	 int firstByte = bufferInputstream.read();
        	 if (firstByte == -1) {
        	     System.out.println("InputStream is empty, nothing to write.");
        	     return false;
        	 } else {
        	     // Reset the stream to include the first byte
        		 bufferInputstream.reset();
        	     return true;
        	 }	
         } catch (IOException e) {
             log.error("Error while reading bytes from inputstream",e);
             return false;
         }
     }
 	
 	 public  static int countMainPages(Context context, List<Bitstream> bitstreams, Item item, boolean isMainPage) {
 		int mainPdfPageCount = 0;
 		int pages = 0;
 		for(Bitstream bitstream : bitstreams) {
 			try {
 		        if(!bitstream.getFormat(context).getMIMEType().equalsIgnoreCase("application/pdf")) {
 		        	continue;
 		        }
 				int storenum = bitstream.getStoreNumber();
 		 		DSBitStoreService localStore = new DSpace().getServiceManager().getServicesByType(DSBitStoreService.class).get(storenum);
 		 		File file = localStore.getFile(bitstream);
 		 		
 		 		if(file == null) {
 		 			continue;
 		 		} 				
 				PDDocument document = null;
 				try {
 					document = Loader.loadPDF(file);
 					int filecount = document.getNumberOfPages();
 	 				pages += filecount;
 					mainPdfPageCount = Math.max(mainPdfPageCount, filecount);
 				} catch (Exception e) {
 			        PdfReader reader = null;
 					try {
 			            reader = new PdfReader(new FileInputStream(file));
 			            int filecount = reader.getNumberOfPages();
 		 				pages += filecount;
 			            mainPdfPageCount = Math.max(mainPdfPageCount, filecount);
 					} catch (Exception e1) {
 						log.info("Error in loading pdf for the item : " + item.getID());
 						log.error("Error in loading pdf document",e1);
 						return 0;
 					} finally {
 			            reader.close();
 					}
 				} finally {
 					document.close();
 				}
 			} catch (IOException | SQLException e) {
 				log.error("Error counting main pdf pages ", e);
 			}
 		}
 		if(isMainPage) {
 			return mainPdfPageCount;
 		} else {
 			return pages;
 		}
 	}
 	 
 	public static void setBitstreamProperties(Context context, Bitstream bitstream) throws IOException {
 			 InputStream stream = null;
 			 OutputStream outStream = null;
 			 File targetTempFile = null;
 			 long charCount = 0;
 			 long pageCount = 0;
 		 try {
 			 
 			 if(bitstreamService == null) {
 				 bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
 			 }
             stream = bitstreamService.retrieve(context, bitstream);
             byte[] inputArray = stream.readAllBytes();
             targetTempFile = File.createTempFile(bitstream.getID() + "_", ".pdf");
             outStream = new FileOutputStream(targetTempFile);
             outStream.write(inputArray);
             
             if (bitstream.getFormat(context).getMIMEType().toLowerCase().contains("pdf")) {
             	PDDocument document = Loader.loadPDF(targetTempFile);
             	pageCount = document.getNumberOfPages();
             	
             	PDFTextStripper stripper = new PDFTextStripper();
             	for (int pageNumber = 0; pageNumber < pageCount; pageNumber++) {
                     stripper.setStartPage(pageNumber + 1);
                     stripper.setEndPage(pageNumber + 1);

                     String text = stripper.getText(document);
                     charCount += text.length();
                 }
                 document.close();
                 
                long currentTimeInMillis = System.currentTimeMillis();
         		Date creationDate = new Date(currentTimeInMillis);
         		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         		String dateString = formatter.format(creationDate);
                 
                 bitstreamService.addMetadata(context, bitstream, MetadataSchemaEnum.DC.getName(), "charcount", null,  "en", String.valueOf(charCount));
                 bitstreamService.addMetadata(context, bitstream, MetadataSchemaEnum.DC.getName(), "pagecount", null,  "en", String.valueOf(pageCount));
                 bitstreamService.addMetadata(context, bitstream, MetadataSchemaEnum.DC.getName(), "date", "creation",  "en", dateString);
             }
 		} catch (Exception e) {
 			log.error("Error in setting up Page and Char Count for Bitstream", e);
 		}finally {
 			if(stream != null)
 				stream.close();
 			if(outStream != null)
 				outStream.close();
 			if(targetTempFile != null && targetTempFile.exists())
 				FileUtils.deleteQuietly(targetTempFile);
 		}
 	}
 	
 	public static String capitalizeEachWord(String input) {
 	    if (input == null || input.isEmpty()) {
 	        return input;
 	    }

 	    String[] words = input.split("\\s+"); // Split by whitespace
 	    StringBuilder capitalized = new StringBuilder();

 	    for (String word : words) {
 	        if (!word.isEmpty()) {
 	            capitalized.append(Character.toUpperCase(word.charAt(0))) // Capitalize first char
 	                      .append(word.substring(1).toLowerCase()) // Convert the rest to lowercase
 	                      .append(" "); // Add space
 	        }
 	    }

 	    return capitalized.toString().trim(); // Remove the trailing space
 	}
	
	public static void addAuditTrail(Context context, Item item, DSpaceObject parent, AuditAction action, Object... args) throws SQLException {
		if(auditTrailService == null) {
			auditTrailService = AuditTrailServiceFactory.getInstance().getAudittrailService();
		}   	
        auditTrailService.logAction(context, item.getHandle(), action, item.getName(),parent.getName(), "Parent ID : " + parent.getID().toString(), args);

    }

	
	 public static void removeItemMappings(Context context, Item item) {
	    	if(itemService == null) {
	    		itemService = ContentServiceFactory.getInstance().getItemService();
	    	}
	    	if(userBitstreamService == null) {
	    		userBitstreamService = UserBitstreamFactory.getInstance().getUserBitstreamService();
	    	}
	    	if(versioningBitstreamService == null) {
	    		versioningBitstreamService = VersioningBitstreamServiceFactory.getInstance().getVersioningBitstreamService();
	    	}
//	    	if(mostViewedItemService == null) {
//	    		mostViewedItemService = MostViewedItemFactory.getInstance().getMostViewedItemService();
//	    	}
//	    	if(appendBitstreamService == null) {
//	    		appendBitstreamService = AppendBitstreamFactory.getInstance().getAppendBitstreamService();
//	    	}
//	    	if(bitstreamStatisticsService == null) {
//	    		bitstreamStatisticsService = BitstreamStatisticsFactory.getInstance().getBitstreamStatisticsService();
//	    	}
//	    	if(syncMetadataItemService == null) {
//	    		syncMetadataItemService = SyncMetadataItemFactory.getInstance().getSyncMetadataItemService();
//	    	}
//	    	if(judgeDiaryService == null) {
//				judgeDiaryService = JudgeDiaryServiceFactory.getInstance().getJudgeDiaryService();
//	    	}
	    	
//	    	
	    	try {
				Item myitem = itemService.find(context, item.getID());
				List<UserBitstream> ubList = userBitstreamService.findByItemId(context, myitem);
				if(ubList !=null && !ubList.isEmpty()) {
					for(UserBitstream ub : ubList) {
						userBitstreamService.delete(context, ub);
					}
				} else {
					log.info("No User Bitstream for item " + myitem.getID());
				}
				
				List<Bundle> bundles = itemService.getBundles(myitem, Constants.DEFAULT_BUNDLE_NAME);
				if(!bundles.isEmpty()) {
					Bundle bundle = bundles.get(0);
					List<VersionBitstream> versionBitstreamList = versioningBitstreamService.findByBundle(context, bundle);
					for (VersionBitstream vb : versionBitstreamList) {
						vb.setBundle(null);
					    versioningBitstreamService.update(context, vb);
					}
				}
//				List<MostViewedItem> mostViewedItemList;
//					mostViewedItemList = mostViewedItemService.findByItemIdandCollection(context, item, item.getOwningCollection());
//				
//				if(mostViewedItemList.size()>0) {
//					MostViewedItem mostViewedItem = mostViewedItemList.get(0);
//					mostViewedItemService.delete(context, mostViewedItem);
//				} else {
//					mostViewedItemList = mostViewedItemService.findByItemId(context, item);
//					if(mostViewedItemList.size()>0) {
//						MostViewedItem mostViewedItem = mostViewedItemList.get(0);
//						mostViewedItemService.delete(context, mostViewedItem);
//					} else {
//						log.info("No Most Viewed Item for item " + myitem.getID());
//					}
//				}
//				List<AppendBitstream> appendBitList = appendBitstreamService.findByItemId(context, myitem);
//				if(appendBitList != null && !appendBitList.isEmpty()) {
//					for(AppendBitstream ab : appendBitList) {
//						appendBitstreamService.delete(context, ab);
//					}
//				}else {
//					log.info("No Append Bitstream for item " + myitem.getID());
//				}
//				
//				List<BitstreamStatistics> bitstreamStatisticsList = bitstreamStatisticsService.findByItemId(context, myitem);
//				if(bitstreamStatisticsList != null && !bitstreamStatisticsList.isEmpty()) {
//					for(BitstreamStatistics bs : bitstreamStatisticsList) {
//						bitstreamStatisticsService.delete(context, bs);
//					}
//				}else {
//					log.info("No Bitstream Statistics for the item " + myitem.getID());
//				}
//				
//				List<SyncMetadataItem> syncMetadataItemList = syncMetadataItemService.findByItem(context, myitem);
//				if(syncMetadataItemList != null && !syncMetadataItemList.isEmpty()) {
//					for (SyncMetadataItem smi : syncMetadataItemList) {
//						syncMetadataItemService.delete(context, smi);
//					}
//				} else {
//					log.info("No Sync Metadata Item for the item " + myitem.getID());
//				}
//				
//				List<JudgeDiary> judgeDiaryList = judgeDiaryService.findByItemID(context, myitem.getID());
//				if(judgeDiaryList != null && !judgeDiaryList.isEmpty()) {
//					for (JudgeDiary jd : judgeDiaryList) {
//						judgeDiaryService.delete(context, jd);
//					}
//				} else {
//					log.info("No Judge Diary Item for the item " + myitem.getID());
//				}
	    	} catch (SQLException | AuthorizeException | IOException e) {
				log.error("Error while deleting item : " + e);
				e.printStackTrace();
			}
	    }

}
