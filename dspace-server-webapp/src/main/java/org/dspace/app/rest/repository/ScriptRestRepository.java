/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.pdf.PdfReader;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.tools.ant.util.FileUtils;
import org.dspace.app.rest.converter.DSpaceRunnableParameterConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.ParameterValueRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.ScriptRest;
import org.dspace.app.rest.scripts.handler.impl.RestDSpaceRunnableHandler;
import org.dspace.app.rest.service.UserPermissionService;
import org.dspace.audittrail.AuditAction;
import org.dspace.audittrail.service.AuditTrailService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.constants.enums.FolderPermission;
import org.dspace.constants.enums.NodeType;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.service.ScriptService;
import org.dspace.util.UUIDUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * This is the REST repository dealing with the Script logic
 */
@Component(ScriptRest.CATEGORY + "." + ScriptRest.PLURAL_NAME)
public class ScriptRestRepository extends DSpaceRestRepository<ScriptRest, String> {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private ScriptService scriptService;

    @Autowired
    private DSpaceRunnableParameterConverter dSpaceRunnableParameterConverter;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private CollectionService cs;
    
    @Autowired
    private CommunityService communityService;
    
    @Autowired
    private AuditTrailService auditTrailService;
    
    @Autowired
    private UserPermissionService userPermissionService;
    
    @Override
    // authorization is verified inside the method
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public ScriptRest findOne(Context context, String name) {
        ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(name);
        if (scriptConfiguration != null) {
            if (scriptConfiguration.isAllowedToExecute(context, null)) {
                return converter.toRest(scriptConfiguration, utils.obtainProjection());
            } else {
                throw new AccessDeniedException("The current user was not authorized to access this script");
            }
        }
        return null;
    }

    @Override
    // authorization check is performed inside the script service
    @PreAuthorize("hasAuthority('AUTHENTICATED')")
    public Page<ScriptRest> findAll(Context context, Pageable pageable) {
        List<ScriptConfiguration> scriptConfigurations =
            scriptService.getScriptConfigurations(context);
        return converter.toRestPage(scriptConfigurations, pageable, utils.obtainProjection());
    }

    @Override
    public Class<ScriptRest> getDomainClass() {
        return ScriptRest.class;
    }

    /**
     * This method will take a String scriptname parameter and it'll try to resolve this to a script known by DSpace.
     * If a script is found, it'll start a process for this script with the given properties to this request
     * @param scriptName    The name of the script that will try to be resolved and started
     * @return A ProcessRest object representing the started process for this script
     * @throws SQLException If something goes wrong
     * @throws IOException  If something goes wrong
     */
    public ProcessRest startProcess(Context context, String scriptName, List<MultipartFile> files) throws SQLException,
        IOException, AuthorizeException, IllegalAccessException, InstantiationException {
        String properties = requestService.getCurrentRequest().getServletRequest().getParameter("properties");
        List<DSpaceCommandLineParameter> dSpaceCommandLineParameters =
            processPropertiesToDSpaceCommandLineParameters(properties);
        ScriptConfiguration scriptToExecute = scriptService.getScriptConfiguration(scriptName);

        if (scriptToExecute == null) {
            throw new ResourceNotFoundException("The script for name: " + scriptName + " wasn't found");
        }
        try {
        	 // Extracting collectionId from properties and then get uuid
            if(scriptName.equalsIgnoreCase("import")) {
            	DSpaceObject dspaceParent = null;
            	String dspaceParentId = dSpaceCommandLineParameters.get(2).getValue();        
            	String dspaceParentName = null;
            	
                dspaceParent = cs.find(context, UUIDUtils.fromString(dspaceParentId));
                
                if(dspaceParent == null) {
                	dspaceParent = communityService.find(context, UUIDUtils.fromString(dspaceParentId));
                }
                
                if(dspaceParent == null  || (dspaceParent.getType() != Constants.COLLECTION && dspaceParent.getType() != Constants.COMMUNITY)) {
                	throw new DSpaceBadRequestException("Upload folder cannot be null");
                }
                
                dspaceParentName = dspaceParent.getName();
                		
                if (context.getCurrentUser() == null) {
                    throw new AuthorizeException("Authorization token has expired!");
                }
                	
    			if (!scriptToExecute.isAllowedToExecute(context, dSpaceCommandLineParameters) &&
    				!userPermissionService.hasPermission(context, UUID.fromString(dspaceParentId), NodeType.LEAF.name(), FolderPermission.UPLOAD.name())) {
    				throw new AuthorizeException("Current user is not eligible to execute script with name: " + scriptName
    						 + " and the specified parameters " + StringUtils.join(dSpaceCommandLineParameters, ", "));
    			}
            	   
            	auditTrailService.logAction(context,"",AuditAction.BATCH_UPLOAD,files.get(0).getOriginalFilename(),dspaceParentName,scriptName);
            }else {
	            if (!scriptToExecute.isAllowedToExecute(context, dSpaceCommandLineParameters)) {
	                throw new AuthorizeException("Current user is not eligible to execute script with name: " + scriptName
	                        + " and the specified parameters " + StringUtils.join(dSpaceCommandLineParameters, ", "));
	            }
            }
        } catch (IllegalArgumentException e) {
            throw new DSpaceBadRequestException("Illegal argoument " + e.getMessage(), e);
        }
        RestDSpaceRunnableHandler restDSpaceRunnableHandler = new RestDSpaceRunnableHandler(
            context.getCurrentUser(), scriptToExecute.getName(), dSpaceCommandLineParameters,
            new HashSet<>(context.getSpecialGroups()));
        List<String> args = constructArgs(dSpaceCommandLineParameters);
        runDSpaceScript(files, context, scriptToExecute, restDSpaceRunnableHandler, args);
        return converter.toRest(restDSpaceRunnableHandler.getProcess(context), utils.obtainProjection());
    }

    private List<DSpaceCommandLineParameter> processPropertiesToDSpaceCommandLineParameters(String propertiesJson)
        throws IOException {
        List<ParameterValueRest> parameterValueRestList = new LinkedList<>();
        if (StringUtils.isNotBlank(propertiesJson)) {
            parameterValueRestList = Arrays.asList(mapper.readValue(propertiesJson, ParameterValueRest[].class));
        }

        List<DSpaceCommandLineParameter> dSpaceCommandLineParameters = new LinkedList<>();
        dSpaceCommandLineParameters.addAll(
            parameterValueRestList.stream().map(x -> dSpaceRunnableParameterConverter.toModel(x))
                                  .collect(Collectors.toList()));
        return dSpaceCommandLineParameters;
    }

    private List<String> constructArgs(List<DSpaceCommandLineParameter> dSpaceCommandLineParameters) {
        List<String> args = new ArrayList<>();
        for (DSpaceCommandLineParameter parameter : dSpaceCommandLineParameters) {
            args.add(parameter.getName());
            if (parameter.getValue() != null) {
                args.add(parameter.getValue());
            }
        }
        return args;
    }

    private void runDSpaceScript(List<MultipartFile> files, Context context, ScriptConfiguration scriptToExecute,
                                 RestDSpaceRunnableHandler restDSpaceRunnableHandler, List<String> args)
        throws IOException, SQLException, AuthorizeException, InstantiationException, IllegalAccessException {
        DSpaceRunnable dSpaceRunnable = scriptService.createDSpaceRunnableForScriptConfiguration(scriptToExecute);
        try {
            dSpaceRunnable.initialize(args.toArray(new String[0]), restDSpaceRunnableHandler, context.getCurrentUser());
            if (files != null && !files.isEmpty()) {
                checkFileNames(dSpaceRunnable, files);
                processFiles(context, restDSpaceRunnableHandler, files);
            }
            restDSpaceRunnableHandler.schedule(dSpaceRunnable);
        } catch (Exception e) {
            dSpaceRunnable.printHelp();
            try {
                restDSpaceRunnableHandler.handleException(
                    "Failed to parse the arguments given to the script with name: "
                        + scriptToExecute.getName() + " and args: " + args, e
                );
            } catch (Exception re) {
                // ignore re-thrown exception
            }
        }
    }

    private void processFiles(Context context, RestDSpaceRunnableHandler restDSpaceRunnableHandler,
                              List<MultipartFile> files)
        throws Exception {
    	
        for (MultipartFile file : files) {
    		String tempDirPath = getTempWorkDir();
        	File tempDir = new File(tempDirPath + System.getProperty("file.separator") + "zip");
        	if (tempDir.exists()) {
        		FileUtils.delete(tempDir);
        	}
        	tempDir.mkdirs();
            File tempZipFile = new File(tempDir + System.getProperty("file.separator") + file.getOriginalFilename());
            file.transferTo(tempZipFile);
            
    		List<Long> allCounts = new ArrayList<>(Arrays.asList(0L, 0L, 0L, 0L, 0L));
    		
    		if (!"metadata-import".equalsIgnoreCase(restDSpaceRunnableHandler.getProcess(context).getName())) {
                String workDir = unzip(tempZipFile, tempDirPath);
    			allCounts = getFolderInfo(workDir);
    		}

            restDSpaceRunnableHandler
                .writeFilestream(context, tempZipFile.getName(), new FileInputStream(tempZipFile), "inputfile", allCounts);
        }
    }
    
    public static String getTempWorkDir() {
        return System.getProperty("java.io.tmpdir") + System.getProperty("file.separator")  + "temp_" + new Date().getTime();
    }
    
    public String unzip(File zipfile, String destDir) throws IOException {
 		// 2
 		// does the zip file exist and can we write to the temp directory
 		if (!zipfile.canRead()) {
 			log.error("Zip file '" + zipfile.getAbsolutePath() + "' does not exist, or is not readable.");
 		}

 		String destinationDir = destDir;
 		if (destDir == null) {
 			destinationDir = getTempWorkDir();
 		}

 		File tempdir = new File(destinationDir);
 		if (tempdir.exists()) {
 			FileUtils.delete(tempdir);
 		}
 		
 		if (!tempdir.exists() && !tempdir.mkdirs()) {
 			log.error("Unable to create temporary directory: " + tempdir.getAbsolutePath());
 		}
 		
 		String zipFileName = zipfile.getName();
 		String sourcedir = destinationDir + System.getProperty("file.separator") + zipFileName;
 		String zipDir = destinationDir + System.getProperty("file.separator") + zipFileName
 				+ System.getProperty("file.separator");

 		// 3
 		String sourceDirForZip = sourcedir;
 		ZipFile zf = new ZipFile(zipfile);
 		ZipEntry entry;
 		try {
			Enumeration<? extends ZipEntry> entries = zf.entries();
			while (entries.hasMoreElements()) {
				entry = entries.nextElement();
				if (entry.isDirectory()) {
					if (!new File(zipDir + entry.getName()).mkdirs()) {
						log.error("Unable to create contents directory: " + zipDir + entry.getName());
					}
				} else {
					System.out.println("Extracting file: " + entry.getName());
					log.info("Extracting file: " + entry.getName());

					int index = entry.getName().lastIndexOf('/');
					if (index == -1) {
						// Was it created on Windows instead?
						index = entry.getName().lastIndexOf('\\');
					}
					if (index > 0) {
						File dir = new File(zipDir + entry.getName().substring(0, index));
						if (!dir.exists() && !dir.mkdirs()) {
							log.error("Unable to create directory: " + dir.getAbsolutePath());
						}

						// Entries could have too many directories, and we need to adjust the sourcedir
						// file1.zip (SimpleArchiveFormat / item1 / contents|dublin_core|...
						// SimpleArchiveFormat / item2 / contents|dublin_core|...
						// or
						// file2.zip (item1 / contents|dublin_core|...
						// item2 / contents|dublin_core|...

						// regex supports either windows or *nix file paths
						String[] entryChunks = entry.getName().split("/|\\\\");
						if (entryChunks.length > 2) {
							if (sourceDirForZip == sourcedir) {
								sourceDirForZip = sourcedir + "/" + entryChunks[0];
							}
						}
					}
					byte[] buffer = new byte[1024];
					int len;
					InputStream in = zf.getInputStream(entry);
					BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(zipDir + entry.getName()));
					while ((len = in.read(buffer)) >= 0) {
						out.write(buffer, 0, len);
					}
					in.close();
					out.close();
				}
			}
			
	 		// Close zip file
	 		zf.close();
		} catch (Exception e) {
			log.error("Error in unzipping zip:: ", e);
		}

 		if (sourceDirForZip != sourcedir) {
 			sourcedir = sourceDirForZip;
 			System.out.println("Set sourceDir using path inside of Zip: " + sourcedir);
 			log.info("Set sourceDir using path inside of Zip: " + sourcedir);
 		}

 		return sourcedir;
 	}

    /**
     * This method checks if the files referenced in the options are actually present for the request
     * If this isn't the case, we'll abort the script now instead of creating issues later on
     * @param dSpaceRunnable   The script that we'll attempt to run
     * @param files             The list of files in the request
     */
    private void checkFileNames(DSpaceRunnable dSpaceRunnable, List<MultipartFile> files) {
        List<String> fileNames = new LinkedList<>();
        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename();
            if (fileNames.contains(fileName)) {
                throw new UnprocessableEntityException("There are two files with the same name: " + fileName);
            } else {
                fileNames.add(fileName);
            }
        }

        List<String> fileNamesFromOptions = dSpaceRunnable.getFileNamesFromInputStreamOptions();
        if (!fileNames.containsAll(fileNamesFromOptions)) {
            throw new UnprocessableEntityException("Files given in properties aren't all present in the request");
        }
    }

 // Read the total files and total number of pages
    private List<Long> getFolderInfo(String dataDir) throws Exception {
   	 log.info("Document in Folder --> ");
		File data = new File(dataDir);
		long fileCount = 0;
		long batchSize = 0;
		long totalPdfs = 0;
		long mainPdfPageCountBatch = 0;
		long pageCount = 0;
		String bitstreamName = null; 
		
		List<Long> result = new ArrayList<Long>(Arrays.asList(0l, 0l, 0l, 0l, 0l));
		try {
	    	for (File caseFolder : data.listFiles()) {  
	    		if (!caseFolder.isDirectory()) {
	                log.warn("Skipping non-folder: " + caseFolder.getName());
	                continue;
	            }
	    		fileCount++;
	            log.info("Processing folder: " + caseFolder);
	    		long mainPageCountItem = 0;
				for (File file : caseFolder.listFiles()) {
					String fileName = file.getName();
					bitstreamName = fileName;
		    		batchSize += ((file.length()/1024));
		    		if (fileName.toLowerCase().endsWith(".pdf")) {    	  
		    			totalPdfs++;
		    			log.info("File Name:-->"+ fileName);
		    			PDDocument document = null;
	 		   			try {
	 		   				document = Loader.loadPDF(file);
	 		   				long filePageCount = document.getNumberOfPages();
	 		    			pageCount += filePageCount;
	 		    			mainPageCountItem = Math.max(mainPageCountItem, filePageCount);
	 		   			} catch (Exception e) {
	 		   		        PdfReader reader = null;
	 		   				try {
	 		   		            reader = new PdfReader(new FileInputStream(file));
	 		   		            long filePageCount = reader.getNumberOfPages();
	 	 		    			pageCount += filePageCount;
	 	 		    			mainPageCountItem = Math.max(mainPageCountItem, filePageCount);
	 		   				} catch (Exception e1) {
	 		   					log.error("Error in loading pdf document",e1);
	 		   				} finally {
	 		   		            reader.close();
	 		   				}
	 		   			} finally {
	 		   				document.close();
	 		   			}
		    		}
				}
				mainPdfPageCountBatch += mainPageCountItem;
	    	}
	    	
	    	result.set(0, fileCount);
	    	result.set(1, batchSize);
	    	result.set(2, totalPdfs);
	    	result.set(3, mainPdfPageCountBatch);
	    	result.set(4, pageCount);
		} catch(Exception e) {
			log.error("Error reading folder information ",e);
			throw new Exception("Following PDF is corrupted. Please check! : " + bitstreamName);
		}
		return result;
    }

}
