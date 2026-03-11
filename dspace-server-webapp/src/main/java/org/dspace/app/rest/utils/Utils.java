/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.utils;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.toList;
import static org.dspace.app.rest.utils.URLUtils.urlIsPrefixOf;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.exception.PaginationException;
import org.dspace.app.rest.exception.RepositoryNotFoundException;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.CommunityRest;
import org.dspace.app.rest.model.LinkRest;
import org.dspace.app.rest.model.LinksRest;
import org.dspace.app.rest.model.OrcidHistoryRest;
import org.dspace.app.rest.model.OrcidQueueRest;
import org.dspace.app.rest.model.ProcessRest;
import org.dspace.app.rest.model.PropertyRest;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.model.RestAddressableModel;
import org.dspace.app.rest.model.RestModel;
import org.dspace.app.rest.model.SupervisionOrderRest;
import org.dspace.app.rest.model.VersionHistoryRest;
import org.dspace.app.rest.model.VocabularyRest;
import org.dspace.app.rest.model.hateoas.EmbeddedPage;
import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.projection.CompositeProjection;
import org.dspace.app.rest.projection.DefaultProjection;
import org.dspace.app.rest.projection.EmbedRelsProjection;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.DSpaceRestRepository;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.dspace.app.rest.repository.ReloadableEntityObjectRepository;
import org.dspace.audittrail.AuditAction;
import org.dspace.audittrail.factory.AuditTrailServiceFactory;
import org.dspace.audittrail.service.AuditTrailService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.constants.enums.FolderPermission;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.dspace.userbitstream.UserBitstream;
import org.dspace.userbitstream.factory.UserBitstreamFactory;
import org.dspace.userbitstream.service.UserBitstreamService;
import org.dspace.util.UUIDUtils;
import org.dspace.versioning.VersionBitstream;
import org.dspace.versioning.factory.VersioningBitstreamServiceFactory;
import org.dspace.versioning.service.VersioningBitstreamService;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Link;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.opencsv.CSVReader;

/**
 * Collection of utility methods
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Component
public class Utils {

    private static final Logger log = LogManager.getLogger(Utils.class);

    /**
     * The default page size, if unspecified in the request.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * The maximum number of embed levels to allow.
     */
    private static final int EMBED_MAX_LEVELS = 10;

    private final static String ANNOTAION_BUNDLE = "ANNOTAION";

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    RequestService requestService;

    @Autowired
    @Qualifier("defaultConversionService")
    ConversionService conversionService;

    @Autowired(required = true)
    private List<DSpaceObjectService<? extends DSpaceObject>> dSpaceObjectServices;

    @Autowired
    private static BitstreamFormatService bitstreamFormatService;

    // Must be loaded @Lazy, as ConverterService also autowires Utils
    @Lazy
    @Autowired
    private ConverterService converter;

    @Autowired
    private static ConfigurationService configurationService;
    
    @Autowired(required = true)
    private static AuditTrailService auditTrailService;
    
    @Autowired(required = true)
	private static ItemService itemService;
    
    @Autowired
    private static GroupService groupService;

    @Autowired
    private static AuthorizeService authorizeService;
    
    @Autowired(required = true)
    private static VersioningBitstreamService versioningBitstreamService;
    
    @Autowired(required = true)
	protected static UserBitstreamService userBitstreamService;
    
    @Autowired(required = true)
	private static BitstreamService bitstreamService;
    
    @Autowired(required = true)
	private static BundleService bundleService;
    
    @Autowired(required = true)
	protected static EPersonService epersonService;
    
    @Autowired(required = true)
   	private static BitstreamStorageService bitstreamStorageService = StorageServiceFactory.getInstance().getBitstreamStorageService();

    @Autowired
    private static CollectionService cs;

    /** Cache to support fast lookups of LinkRest method annotation information. */
    private final Map<Method, Optional<LinkRest>> linkAnnotationForMethod = new HashMap<>();

    public <T> Page<T> getPage(List<T> fullContents, @Nullable Pageable optionalPageable) {
        Pageable pageable = getPageable(optionalPageable);
        int total = fullContents.size();
        if (pageable.getOffset() > total) {
            throw new PaginationException(total);
        } else {
            List<T> pageContent = getListSlice(fullContents, pageable);
            return new PageImpl<>(pageContent, pageable, total);
        }
    }

    /**
     * Returns list of objects for the current page.
     * @param fullList the complete list of objects
     * @param optionalPageable
     * @return list of page objects
     * @param <T>
     */
    public <T> List<T> getPageObjectList(List<T> fullList, @Nullable Pageable optionalPageable) {
        Pageable pageable = getPageable(optionalPageable);
        int total = fullList.size();
        if (pageable.getOffset() > total) {
            throw new PaginationException(total);
        } else {
            return getListSlice(fullList, pageable);
        }
    }

    /**
     * Returns the list elements required for the page
     * @param fullList the complete list of objects
     * @param pageable
     * @return list of page objects
     * @param <T>
     */
    private <T> List<T> getListSlice(List<T> fullList, Pageable pageable) {
        int total = fullList.size();
        List<T> pageContent = null;
        if (pageable.getOffset() + pageable.getPageSize() > total) {
            pageContent = fullList.subList(Math.toIntExact(pageable.getOffset()), total);
        } else {
            pageContent = fullList.subList(Math.toIntExact(pageable.getOffset()),
                Math.toIntExact(pageable.getOffset()) + pageable.getPageSize());
        }
        return pageContent;
    }

    /**
     * Convenience method to get a default pageable instance if needed.
     *
     * @param optionalPageable the existing pageable instance, may be null.
     * @return the existing instance if it is not null, a default pageable instance otherwise.
     */
    public Pageable getPageable(@Nullable Pageable optionalPageable) {
        return optionalPageable != null ? optionalPageable : PageRequest.of(0, DEFAULT_PAGE_SIZE);
    }

    /**
     * Create a HAL Link to a single resource
     * @param data the resource itself
     * @param rel name of the link relation to create
     * @return created Link object
     */
    public Link linkToSingleResource(RestAddressableModel data, String rel) {
        // Create link using Spring HATEOAS link builder
        return linkTo(data.getController(), data.getCategory(), data.getTypePlural()).slash(getIdentifierForLink(data))
                                                                                     .withRel(rel);
    }

    /**
     * Create a HAL Link to a subresource of given resource. This method assumes the name & link to the subresource
     * are both the same string value. See other linkToSubResource method if they are different.
     * @param data main resource
     * @param rel name/subpath of the subresource (assumed to be the same)
     * @return created Link object
     */
    public Link linkToSubResource(RestAddressableModel data, String rel) {
        return linkToSubResource(data, rel, rel);
    }

    /**
     * Create a HAL Link to a subresource of given resource using given path name and link name
     * @param data main resource
     * @param rel name of the subresource link relation to create
     * @param path subpath for the subresource
     * @return created Link object
     */
    public Link linkToSubResource(RestAddressableModel data, String rel, String path) {
        // Create link using Spring HATEOAS link builder
        return linkTo(data.getController(), data.getCategory(), data.getTypePlural()).slash(getIdentifierForLink(data))
                                                                                     .slash(path)
                                                                                     .withRel(rel);
    }

    /**
     * Returns an identifier for a given resource, to be used in a Link.
     * @param data resource to identify
     * @return identifier, which is either an ID (if exists) or string representation of the object.
     */
    private Serializable getIdentifierForLink(RestAddressableModel data) {
        // If the resource is identifiable by an ID, use it. Otherwise use toString() to represent it.
        Serializable identifier = data.toString();
        if (data instanceof BaseObjectRest) {
            identifier = ((BaseObjectRest) data).getId();
        }
        return identifier;
    }

    /**
     * Retrieve the {@link DSpaceRestRepository} for the specified category and model in the plural form as used in the endpoints.
     * If the model is available in its singular form use {@link #getResourceRepositoryByCategoryAndModel(String, String)}
     *
     * @param apiCategory
     * @param modelPlural
     * @return the requested repository.
     * @throws RepositoryNotFoundException passed through.
     */
    public DSpaceRestRepository getResourceRepository(String apiCategory, String modelPlural)
            throws RepositoryNotFoundException {
        return getResourceRepositoryByCategoryAndModel(apiCategory, modelPlural);
    }

    /**
     * Retrieve the {@link DSpaceRestRepository} for the specified category and model. The model is in the singular form
     * as returned by the {@link RestAddressableModel#getType()} method
     *
     * @param apiCategory
     * @param modelPlural
     * @return the requested repository.
     * @throws RepositoryNotFoundException if no such repository can be found.
     */
    public DSpaceRestRepository getResourceRepositoryByCategoryAndModel(String apiCategory, String modelPlural)
            throws RepositoryNotFoundException {
        try {
            return applicationContext.getBean(apiCategory + "." + modelPlural, DSpaceRestRepository.class);
        } catch (NoSuchBeanDefinitionException e) {
            throw new RepositoryNotFoundException(apiCategory, modelPlural);
        }
    }

    /**
     * Find the names of all {@link DSpaceRestRepository} implementations.
     * @return the names of all repository types.
     */
    public String[] getRepositories() {
        return applicationContext.getBeanNamesForType(DSpaceRestRepository.class);
    }

    public static String makeSingular(String modelPlural) {
        // The old dspace res package includes the evo inflection library which
        // has a plural() function but no singular function
        if (modelPlural.equals("communities")) {
            return CommunityRest.NAME;
        }
        if (modelPlural.equals("authorities")) {
            return VocabularyRest.NAME;
        }
        if (modelPlural.equals("resourcepolicies")) {
            return ResourcePolicyRest.NAME;
        }
        if (StringUtils.equals(modelPlural, "processes")) {
            return ProcessRest.NAME;
        }
        if (StringUtils.equals(modelPlural, "versionhistories")) {
            return VersionHistoryRest.NAME;
        }
        if (StringUtils.equals(modelPlural, "properties")) {
            return PropertyRest.NAME;
        }
        if (StringUtils.equals(modelPlural, "vocabularies")) {
            return VocabularyRest.NAME;
        }
        if (StringUtils.equals(modelPlural, OrcidQueueRest.PLURAL_NAME)) {
            return OrcidQueueRest.NAME;
        }
        if (StringUtils.equals(modelPlural, "orcidhistories")) {
            return OrcidHistoryRest.NAME;
        }
        if (StringUtils.equals(modelPlural, "supervisionorders")) {
            return SupervisionOrderRest.NAME;
        }
        return modelPlural.replaceAll("s$", "");
    }

    /**
     * Retrieve the LinkRestRepository associated with a specific link from the
     * apiCategory and model specified in the parameters.
     *
     * @param apiCategory the apiCategory
     * @param modelPlural the model name in its plural form
     * @param rel         the name of the relation
     * @return
     */
    public LinkRestRepository getLinkResourceRepository(String apiCategory, String modelPlural, String rel) {
        try {
            return applicationContext.getBean(apiCategory + "." + modelPlural + "." + rel, LinkRestRepository.class);
        } catch (NoSuchBeanDefinitionException e) {
            throw new RepositoryNotFoundException(apiCategory, modelPlural);
        }
    }

    /**
     * @param rel
     * @param restClass
     * @return the LinkRest annotation corresponding to the specified rel in the rest class, or null if not found.
     */
    public LinkRest getClassLevelLinkRest(String rel, Class<? extends RestAddressableModel> restClass) {
        Optional<LinkRest> optionalLinkRest = getLinkRests(restClass).stream().filter((linkRest) ->
                rel.equals(linkRest.name())).findFirst();
        return optionalLinkRest.isPresent() ? optionalLinkRest.get() : null;
    }

    /**
     * Build the canonical representation of a metadata key in DSpace.  I.e.
     * {@code <schema>.<element>[.<qualifier>]}
     *
     * @param schema
     * @param element
     * @param qualifier
     * @return
     */
    public String getMetadataKey(String schema, String element, String qualifier) {
        return org.dspace.core.Utils.standardize(schema, element, qualifier, ".");
    }

    /**
     * Create a temporary file from a multipart file upload
     *
     * @param multipartFile
     *            the multipartFile representing the uploaded file. Please note that it is a complex object including
     *            additional information other than the binary like the original file name and the MIME type
     * @param prefixTempName
     *            the prefix to use to generate the filename of the temporary file
     * @param suffixTempName
     *            the suffix to use to generate the filename of the temporary file
     * @return the temporary file on the server
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static File getFile(MultipartFile multipartFile, String prefixTempName, String suffixTempName)
            throws IOException, FileNotFoundException {
        // TODO after change item-submission into
        ConfigurationService configurationService
                = DSpaceServicesFactory.getInstance().getConfigurationService();
        String tempDir = (configurationService.hasProperty("upload.temp.dir"))
                ? configurationService.getProperty("upload.temp.dir")
                : System.getProperty("java.io.tmpdir");
        File uploadDir = new File(tempDir);
        if (!uploadDir.exists()) {
            if (!uploadDir.mkdir()) {
                uploadDir = null;
            }
        }
        File file = File.createTempFile(prefixTempName + "-" + suffixTempName, ".temp", uploadDir);
        try (InputStream io = new BufferedInputStream(multipartFile.getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));) {
            org.dspace.core.Utils.bufferedCopy(io, out);
        }
        return file;
    }

    /**
     * Return the filename part from a multipartFile upload that could eventually contains the full path on the client

     *
     * @param multipartFile
     *            the file uploaded
     * @return the filename part of the file on the client filesystem
     * @throws IOException
     * @throws FileNotFoundException
     */
    public static String getFileName(MultipartFile multipartFile)
            throws IOException, FileNotFoundException {
        String originalFilename = multipartFile.getOriginalFilename();
        if (originalFilename != null) {
            // split by \ or / as we don't know the client OS (Win, Linux)
            String[] parts = originalFilename.split("[\\/]");
            return parts[parts.length - 1];
        } else {
            return multipartFile.getName();
        }
    }

    /**
     * This method will construct a List of BitstreamFormats out of a request.
     * It will call the {@link Utils#getStringListFromRequest(HttpServletRequest)} method to retrieve a list of links
     * out of the request.
     * The method will iterate over this list of links and parse the links to retrieve the integer ID from it.
     * It will then retrieve the BitstreamFormat corresponding to this ID.
     * If one is found, this BitstreamFormat is added to the List of BitstreamFormats that we will return.
     *
     * @param request   The request out of which we'll create the List of BitstreamFormats
     * @param context   The relevant DSpace context
     * @return          The resulting list of BitstreamFormats that we parsed out of the request
     */
    public List<BitstreamFormat> constructBitstreamFormatList(HttpServletRequest request, Context context) {

    	 bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
        return getStringListFromRequest(request).stream()
                .map(link -> {
                    if (link.endsWith("/")) {
                        link = link.substring(0, link.length() - 1);
                    }
                    return link.substring(link.lastIndexOf('/') + 1);
                })
                .map(id -> {
                    try {
                        return bitstreamFormatService.find(context, parseInt(id));
                    } catch (SQLException | NumberFormatException e) {
                        log.error("Could not find bitstream format for id: " + id, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(toList());
    }

    /**
     * This method will construct a List of DSpaceObjects by executing the method
     * {@link Utils#readFromRequest(HttpServletRequest)} and fetching the List of Strings from the request.
     * The method will iterate over this list of Strings and parse the String to retrieve the UUID from it.
     * It will then look through all the DSpaceObjectServices to try and match this UUID to a DSpaceObject.
     * If one is found, this DSpaceObject is added to the List of DSpaceObjects that we will return.
     * @param context   The relevant DSpace context
     * @param list      The interesting UUIDs.
     * @return          The resulting list of DSpaceObjects that we parsed out of the request
     */
    public List<DSpaceObject> constructDSpaceObjectList(Context context, List<String> list) {
        List<DSpaceObject> dSpaceObjects = new LinkedList<>();
        for (String string : list) {
            if (string.endsWith("/")) {
                string = string.substring(0, string.length() - 1);
            }
            String uuid = string.substring(string.lastIndexOf('/') + 1);
            try {
                for (DSpaceObjectService dSpaceObjectService : dSpaceObjectServices) {
                    DSpaceObject dSpaceObject = dSpaceObjectService.find(context, UUIDUtils.fromString(uuid));
                    if (dSpaceObject != null) {
                        dSpaceObjects.add(dSpaceObject);
                        break;
                    }
                }
            } catch (SQLException e) {
                log.error("Could not find DSpaceObject for UUID: " + uuid, e);
            }

        }
        return dSpaceObjects;
    }

    /**
     * This method reads lines from the request's InputStream and will add this to a list of Strings.
     * @param request       The request from which the InputStream will be fetched
     * @return              A list of String constructed from the request's InputStream
     * @throws IOException  If something goes wrong
     */
    private List<String> readFromRequest(HttpServletRequest request) throws IOException {
        List<String> list = new LinkedList<>();
        Scanner scanner = new Scanner(request.getInputStream());

        try {

            while (scanner.hasNextLine()) {

                String line = scanner.nextLine();
                if (org.springframework.util.StringUtils.hasText(line)) {
                    list.add(decodeUrl(line));
                }
            }

        } finally {
            scanner.close();
        }
        return list;
    }

    private String decodeUrl(String url) {
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.warn("The following url could not be decoded: " + url);
        }
        return StringUtils.EMPTY;
    }

    /**
     * This method will retrieve a list of DSpaceObjects from the Request by reading in the Request's InputStream
     * with a Scanner and searching the InputStream for UUIDs which will then be resolved to a DSpaceObject.
     * These will all be added to a list and returned by this method.
     * @param request       The request of which the InputStream will be used
     * @return              The list of DSpaceObjects that we could find in the InputStream
     * @throws IOException  If something goes wrong
     */
    public List<String> getStringListFromRequest(HttpServletRequest request) {
        List<String> list = null;
        try {
            list = readFromRequest(request);
        } catch (IOException e) {
            log.error("Something went wrong with reading in the inputstream from the request", e);
        }
        return list;
    }

    public <T extends HALResource> T toResource(RestModel restObject) {
        return converter.toResource(restObject);
    }

    /**
     * Gets the alphanumerically sorted union of multiple string arrays.
     *
     * @param arrays the string arrays.
     * @return the sorted union of them, with no duplicate values.
     */
    public String[] getSortedUnion(String[]... arrays) {
        Set<String> set = new TreeSet<>();
        for (String[] array : arrays) {
            for (String string : array) {
                set.add(string);
            }
        }
        return set.toArray(arrays[0]);
    }

    /**
     * Gets the method with the given name in the given class.
     *
     * @param clazz the class.
     * @param name the method name.
     * @return the first method found with the given name.
     * @throws IllegalArgumentException if no such method is found.
     */
    public Method requireMethod(Class clazz, String name) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        throw new IllegalArgumentException("No such method in " + clazz + ": " + name);
    }

    /**
     * Gets the effective projection requested by the current servlet request, or {@link DefaultProjection} if none
     * is specified.
     * <p>
     * Any number of individual {@code Projections} that are spring-registered {@link Component}s may be specified
     * by name via the {@code projection} parameter. If multiple projections are specified, they will be wrapped in a
     * {@link CompositeProjection} and applied in order as described there.
     * </p><p>
     * In addition, any number of embeds may be specified by rel name via the {@code embed} parameter.
     * When provided, these act as an "allow list" of embeds that may be included in the response, as described
     * and implemented by {@link EmbedRelsProjection}.
     * </p>
     *
     * @return the requested or default projection, never {@code null}.
     * @throws IllegalArgumentException if the request specifies an unknown projection name.
     */
    public Projection obtainProjection() {
        ServletRequest servletRequest = requestService.getCurrentRequest().getServletRequest();
        List<String> projectionNames = getValues(servletRequest, "projection");
        Set<String> embedRels = new HashSet<>(getValues(servletRequest, "embed"));

        List<Projection> projections = new ArrayList<>();

        for (String projectionName : projectionNames) {
            projections.add(converter.getProjection(projectionName));
        }


        if (!embedRels.isEmpty()) {
            Set<String> embedSizes = new HashSet<>(getValues(servletRequest, "embed.size"));
            projections.add(new EmbedRelsProjection(embedRels, embedSizes));
        }

        if (projections.isEmpty()) {
            return Projection.DEFAULT;
        } else if (projections.size() == 1) {
            return projections.get(0);
        } else {
            return new CompositeProjection(projections);
        }
    }

    /**
     * Gets zero or more values for the given servlet request parameter.
     * <p>
     * This convenience method reads multiple values that have been specified as request parameter in multiple ways:
     * via * {@code ?paramName=value1&paramName=value2}, via {@code ?paramName=value1,value2},
     * or a combination.
     * </p><p>
     * It provides the values in the order they were given in the request, and automatically de-dupes them.
     * </p>
     *
     * @param servletRequest the servlet request.
     * @param parameterName the parameter name.
     * @return the ordered, de-duped values, possibly empty, never {@code null}.
     */
    private List<String> getValues(ServletRequest servletRequest, String parameterName) {
        String[] rawValues = servletRequest.getParameterValues(parameterName);
        List<String> values = new ArrayList<>();
        if (rawValues != null) {
            for (String rawValue : rawValues) {
                for (String value : rawValue.split(",")) {
                    String trimmedValue = value.trim();
                    if (trimmedValue.length() > 0 && !values.contains(trimmedValue)) {
                        values.add(trimmedValue);
                    }
                }
            }
        }
        return values;
    }

    /**
     * Adds embeds or links for all class-level LinkRel annotations for which embeds or links are allowed.
     *
     * @param halResource the resource.
     * @param oldLinks    previously traversed links
     */
    public void embedOrLinkClassLevelRels(HALResource<RestAddressableModel> halResource, Link... oldLinks) {
        Projection projection = halResource.getContent().getProjection();
        getLinkRests(halResource.getContent().getClass()).stream().forEach((linkRest) -> {
            Link link = linkToSubResource(halResource.getContent(), linkRest.name());
            if (projection.allowEmbedding(halResource, linkRest, oldLinks)) {
                embedRelFromRepository(halResource, linkRest.name(), link, linkRest, oldLinks);
                halResource.add(link); // unconditionally link if embedding was allowed
            } else if (projection.allowLinking(halResource, linkRest)) {
                halResource.add(link);
            }
        });
    }

    private List<LinkRest> getLinkRests(Class<? extends RestAddressableModel> restClass) {
        List<LinkRest> list = new ArrayList<>();
        LinksRest linksAnnotation = restClass.getDeclaredAnnotation(LinksRest.class);
        if (linksAnnotation != null) {
            list.addAll(Arrays.asList(linksAnnotation.links()));
        }
        return list;
    }

    /**
     * Embeds a rel whose value comes from a {@link LinkRestRepository}, if the maximum embed level has not
     * been exceeded yet.
     * <p>
     * The embed will be skipped if 1) the link repository reports that it is not embeddable or 2) the returned
     * value is null and the LinkRest annotation has embedOptional = true.
     * </p><p>
     * Implementation note: The caller is responsible for ensuring that the projection allows the embed
     * before calling this method.
     * </p>
     *
     * @param resource the resource from which the embed will be made.
     * @param rel the name of the rel.
     * @param link the link.
     * @param linkRest the LinkRest annotation (must have method defined).
     * @throws RepositoryNotFoundException if the link repository could not be found.
     * @throws IllegalArgumentException if the method specified by the LinkRest could not be found in the
     * link repository.
     * @throws RuntimeException if any other problem occurs when trying to invoke the method.
     */
    void embedRelFromRepository(HALResource<? extends RestAddressableModel> resource,
                                        String rel, Link link, LinkRest linkRest) {
        embedRelFromRepository(resource, rel, link, linkRest, new Link[] {});
    }

    /**
     * Embeds a rel whose value comes from a {@link LinkRestRepository}, if the maximum embed level has not
     * been exceeded yet.
     * <p>
     * The embed will be skipped if 1) the link repository reports that it is not embeddable or 2) the returned
     * value is null and the LinkRest annotation has embedOptional = true.
     * </p><p>
     * Implementation note: The caller is responsible for ensuring that the projection allows the embed
     * before calling this method.
     * </p>
     *
     * @param resource the resource from which the embed will be made.
     * @param rel the name of the rel.
     * @param link the link.
     * @param linkRest the LinkRest annotation (must have method defined).
     * @param oldLinks    The previously traversed links
     * @throws RepositoryNotFoundException if the link repository could not be found.
     * @throws IllegalArgumentException if the method specified by the LinkRest could not be found in the
     * link repository.
     * @throws RuntimeException if any other problem occurs when trying to invoke the method.
     */
    void embedRelFromRepository(HALResource<? extends RestAddressableModel> resource,
                                        String rel, Link link, LinkRest linkRest, Link... oldLinks) {
        if (resource.getContent().getEmbedLevel() == EMBED_MAX_LEVELS) {
            return;
        }
        Projection projection = resource.getContent().getProjection();
        LinkRestRepository linkRepository = getLinkResourceRepository(resource.getContent().getCategory(),
                resource.getContent().getTypePlural(), rel);
        if (linkRepository.isEmbeddableRelation(resource.getContent(), rel)) {
            Method method = requireMethod(linkRepository.getClass(), linkRest.method());
            Object contentId = getContentIdForLinkMethod(resource.getContent(), method);
            try {
                Object linkedObject = method.invoke(linkRepository, null, contentId,
                                                    projection.getPagingOptions(rel, resource, oldLinks), projection);
                resource.embedResource(rel, wrapForEmbedding(resource, linkedObject, link, oldLinks));
            } catch (InvocationTargetException e) {
                // This will be thrown from the LinkRepository if a Resource has been requested that'll try to embed
                // something that we don't have READ rights to. It'll then throw an AccessDeniedException from that
                // linkRepository and we want to catch it here since we don't want our entire request to fail if a
                // subresource of the requested resource is not available to be embedded. Instead we'll log it here
                if (e.getTargetException() instanceof AccessDeniedException) {
                    log.warn("Tried fetching resource: " + linkRest.name() + " for DSpaceObject with ID: " + contentId);
                } else if (e.getTargetException() instanceof RuntimeException) {
                    throw (RuntimeException) e.getTargetException();
                } else {
                    throw new RuntimeException(e);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Adds embeds (if the maximum embed level has not been exceeded yet) for all properties annotated with
     * {@code @LinkRel} or whose return types are {@link RestAddressableModel} subclasses.
     * @param resource the resource to be so augmented.
     */
    public void embedMethodLevelRels(HALResource<? extends RestAddressableModel> resource) {
        if (resource.getContent().getEmbedLevel() == EMBED_MAX_LEVELS) {
            return;
        }
        try {
            for (PropertyDescriptor pd : Introspector.getBeanInfo(
                    resource.getContent().getClass()).getPropertyDescriptors()) {
                Method readMethod = pd.getReadMethod();
                String propertyName = pd.getName();
                if (readMethod != null && !"class".equals(propertyName)) {
                    embedMethodLevelRel(resource, readMethod, propertyName);
                }
            }
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the LinkRest annotation for the given method, if any.
     *
     * @param readMethod the method.
     * @return the annotation, or {@code null} if not found.
     */
    public @Nullable LinkRest findLinkAnnotation(Method readMethod) {
        Optional<LinkRest> optional = linkAnnotationForMethod.get(readMethod);
        if (optional == null) {
            LinkRest linkRest = AnnotationUtils.findAnnotation(readMethod, LinkRest.class);
            optional = linkRest != null ? Optional.of(linkRest) : Optional.empty();
            linkAnnotationForMethod.put(readMethod, optional);
        }
        return optional.isPresent() ? optional.get() : null;
    }

    /**
     * Adds an embed for the given property read method. If the @LinkRel annotation is present and
     * specifies a method name, the value will come from invoking that method in the appropriate link
     * rest repository. Otherwise, the value will come from invoking the method directly on the wrapped
     * rest object.
     *
     * @param resource the resource from which the embed will be made.
     * @param readMethod the property read method.
     * @param propertyName the property name, which will be used as the rel/embed name unless the @LinkRel
     *                     annotation is present and specifies a different name.
     */
    private void embedMethodLevelRel(HALResource<? extends RestAddressableModel> resource,
                                     Method readMethod,
                                     String propertyName) {
        String rel = propertyName;
        LinkRest linkRest = findLinkAnnotation(readMethod);
        try {
            if (linkRest != null) {
                if (!resource.getContent().getProjection().allowEmbedding(resource, linkRest)) {
                    return; // projection disallows this optional method-level embed
                }
                if (StringUtils.isNotBlank(linkRest.name())) {
                    rel = linkRest.name();
                }
                Link link = linkToSubResource(resource.getContent(), rel);
                if (StringUtils.isBlank(linkRest.method())) {
                    Object linkedObject = readMethod.invoke(resource.getContent());
                    resource.embedResource(rel, wrapForEmbedding(resource, linkedObject, link));
                } else {
                    embedRelFromRepository(resource, rel, link, linkRest);
                }
            } else if (RestAddressableModel.class.isAssignableFrom(readMethod.getReturnType())) {
                RestAddressableModel linkedObject = (RestAddressableModel) readMethod.invoke(resource.getContent());
                resource.embedResource(rel, linkedObject == null ? null :
                        wrapForEmbedding(resource, linkedObject, linkToSubResource(resource.getContent(), rel)));
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Wraps the given linked object (retrieved from a link repository or link method on the rest item)
     * in an object that is appropriate for embedding, if needed. Does not perform the actual embed; the
     * caller is responsible for that.
     *
     * @param resource the resource from which the embed will be made.
     * @param linkedObject the linked object.
     * @param link the link, which is used if the linked object is a list or page, to determine the self link
     *             and embed property name to use for the subresource.
     * @return the wrapped object, which will have an "embed level" one greater than the given parent resource.
     */
    private Object wrapForEmbedding(HALResource<? extends RestAddressableModel> resource,
                                    Object linkedObject, Link link) {
        return wrapForEmbedding(resource, linkedObject, link, new Link[] {});
    }

    /**
     * Wraps the given linked object (retrieved from a link repository or link method on the rest item)
     * in an object that is appropriate for embedding, if needed. Does not perform the actual embed; the
     * caller is responsible for that.
     *
     * @param resource the resource from which the embed will be made.
     * @param linkedObject the linked object.
     * @param link the link, which is used if the linked object is a list or page, to determine the self link
     *             and embed property name to use for the subresource.
     * @param oldLinks    The previously traversed links
     * @return the wrapped object, which will have an "embed level" one greater than the given parent resource.
     */
    private Object wrapForEmbedding(HALResource<? extends RestAddressableModel> resource,
                                    Object linkedObject, Link link, Link... oldLinks) {
        int childEmbedLevel = resource.getContent().getEmbedLevel() + 1;
        //Add the latest link to the list
        Link[] newList = Arrays.copyOf(oldLinks, oldLinks.length + 1);
        newList[oldLinks.length] = link;
        if (linkedObject instanceof RestAddressableModel) {
            RestAddressableModel restObject = (RestAddressableModel) linkedObject;
            restObject.setEmbedLevel(childEmbedLevel);
            return converter.toResource(restObject, newList);
        } else if (linkedObject instanceof Page) {
            // The first page has already been constructed by a link repository and we only need to wrap it
            Page<RestAddressableModel> page = (Page<RestAddressableModel>) linkedObject;
            return new EmbeddedPage(link.getHref(), page.map((restObject) -> {
                restObject.setEmbedLevel(childEmbedLevel);
                return converter.toResource(restObject, newList);
            }), null, link.getRel().value());
        } else if (linkedObject instanceof List) {
            // The full list has been retrieved and we need to provide the first page for embedding
            List<RestAddressableModel> list = (List<RestAddressableModel>) linkedObject;
            if (list.size() > 0) {
                PageImpl<RestAddressableModel> page = new PageImpl(
                        list.subList(0, list.size() > DEFAULT_PAGE_SIZE ? DEFAULT_PAGE_SIZE : list.size()),
                        PageRequest.of(0, DEFAULT_PAGE_SIZE), list.size());
                return new EmbeddedPage(link.getHref(),
                        page.map((restObject) -> {
                            restObject.setEmbedLevel(childEmbedLevel);
                            return converter.toResource(restObject, newList);
                        }),
                        list, link.getRel().value());
            } else {
                PageImpl<RestAddressableModel> page = new PageImpl(list);
                return new EmbeddedPage(link.getHref(), page, list, link.getRel().value());
            }
        } else {
            return linkedObject;
        }
    }

    /**
     * Gets an object representing the id of the wrapped object, whose runtime time matches the second
     * (id) argument of the given link method. This is necessary because it is possible for the rest
     * object's id to be a string while the domain object's id may be a uuid or numeric type.
     *
     * @param linkMethod the link method.
     * @return the id, which may be a UUID, Integer, or Long.
     */
    private Object getContentIdForLinkMethod(RestAddressableModel restObject, Method linkMethod) {
        Object contentId = ((BaseObjectRest) restObject).getId();
        Class requiredIdType = linkMethod.getParameterTypes()[1];
        if (!requiredIdType.isAssignableFrom(contentId.getClass())) {
            if (requiredIdType.equals(UUID.class)) {
                contentId = UUID.fromString(contentId.toString());
            } else if (requiredIdType.equals(Integer.class)) {
                contentId = Integer.parseInt(contentId.toString());
            } else if (requiredIdType.equals(Long.class)) {
                contentId = Long.parseLong(contentId.toString());
            } else {
                throw new IllegalArgumentException("Cannot cast " + restObject.getClass()
                        + " id type " + contentId.getClass() + " to id type required by "
                        + linkMethod.getDeclaringClass() + "#" + linkMethod.getName() + ": " + requiredIdType);
            }
        }
        return contentId;
    }

    /**
     * Convert the input string in the primary key class according to the repository interface
     *
     * @param repository
     * @param pkStr
     * @return
     */
    public Serializable castToPKClass(ReloadableEntityObjectRepository repository, String pkStr) {
        return (Serializable) conversionService.convert(pkStr, repository.getPKClass());
    }

    /**
     * Return the dspace api model object corresponding to the provided, not null, rest object. This only works when the
     * rest object is supported by a {@link DSpaceRestRepository} that also implement the
     * {@link ReloadableEntityObjectRepository} interface. If this is not the case the method will throw an
     * IllegalArgumentException
     *
     * @param context
     *            the DSpace Context
     * @param restObj
     *            the not null rest object. If null the method will throws an {@link IllegalArgumentException}
     * @return the dspace api model object corresponding to the provided, not null, rest object
     * @throws IllegalArgumentException
     *             if the restObj is not supported by a {@link DSpaceRestRepository} that also implement the
     *             {@link ReloadableEntityObjectRepository} interface
     * @throws SQLException
     *             if a database error occur
     */
    public Object getDSpaceAPIObjectFromRest(Context context, BaseObjectRest restObj)
            throws IllegalArgumentException, SQLException {
        DSpaceRestRepository repository = getResourceRepositoryByCategoryAndModel(restObj.getCategory(),
                restObj.getTypePlural());
        Serializable pk = castToPKClass((ReloadableEntityObjectRepository) repository, restObj.getId().toString());
        return ((ReloadableEntityObjectRepository) repository).findDomainObjectByPk(context, pk);
    }

    /**
    * Get the rest object associated with the specified URI
    *
    * @param context the DSpace context
    * @param uri     the uri of a {@link BaseObjectRest}
    * @return the {@link BaseObjectRest} identified by the provided uri
    * @throws SQLException             if a database error occur
    * @throws IllegalArgumentException if the uri is not valid
    */
    public BaseObjectRest getBaseObjectRestFromUri(Context context, String uri) throws SQLException {
    	if(configurationService == null) {
    		configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    	}
        String dspaceUrl = configurationService.getProperty("dspace.server.url");
        String dspaceSSRUrl = configurationService.getProperty("dspace.server.ssr.url", dspaceUrl);

        // Convert strings to URL objects.
        // Do this early to check that inputs are well-formed.
        URL dspaceUrlObject;
        URL dspaceUrlSSRObject = null;
        URL requestUrlObject;
        try {
            dspaceUrlObject = new URL(dspaceUrl);
            requestUrlObject = new URL(uri);
            if (StringUtils.isNoneBlank(dspaceSSRUrl)) {
                dspaceUrlSSRObject = new URL(dspaceSSRUrl);
            }
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(
                    String.format("Configuration '%s' or request '%s' is malformed", dspaceUrl, uri));
        }

        // Check whether the URI could be valid.
        if (!urlIsPrefixOf(dspaceUrl, uri) && !urlIsPrefixOf(dspaceSSRUrl, uri)) {
            throw new IllegalArgumentException("the supplied uri is not ours: " + uri);
        }

        // Extract from the URI the category, model and id components.
        // They start after the dspaceUrl/api/{apiCategory}/{apiModel}/{id}
        int dspacePathLength = StringUtils.split(dspaceUrlObject.getPath(), '/').length;
        String[] requestPath = StringUtils.split(requestUrlObject.getPath(), '/');
        String[] uriParts = Arrays.copyOfRange(requestPath, dspacePathLength,
                requestPath.length);

        int dspaceSSRPathLength = StringUtils.split(dspaceUrlSSRObject.getPath(), '/').length;
        String[] uriSSRParts = Arrays.copyOfRange(requestPath, dspaceSSRPathLength,
            requestPath.length);

        if ("api".equalsIgnoreCase(uriParts[0])) {
            uriParts = Arrays.copyOfRange(uriParts, 1, uriParts.length);
        }
        if (uriParts.length != 3 && uriSSRParts.length != 3) {
            throw new IllegalArgumentException("the supplied uri lacks required path elements: " + uri);
        }

        return findBaseObjectRest(context, uriParts[0], uriParts[1], uriParts[2]);
    }

    /**
     * Gets the rest object of of specified type (i.e. "core.item", "core.collection", "workflow.workflowitem",...)
     * having given uuid.
     *
     * @param context the DSpace context
     * @param type Object type
     * @param uuid Object uuid
     * @return the {@link BaseObjectRest} identified by the provided uuid
     */
    public BaseObjectRest getBaseObjectRestFromTypeAndUUID(Context context, String type, String uuid) {

        if (StringUtils.isBlank(type)) {
            throw new IllegalArgumentException("Type is missing");
        }

        String[] split = type.split("\\.");
        if (split.length != 2) {
            throw new IllegalArgumentException("Provided type is not valid: " + type);
        }
        return findBaseObjectRest(context, split[0], split[1], uuid);
    }

    private BaseObjectRest findBaseObjectRest(Context context, String apiCategory, String model, String uuid) {

        DSpaceRestRepository repository;
        try {
            repository = getResourceRepository(apiCategory, model);
            if (!(repository instanceof ReloadableEntityObjectRepository)) {
                throw new IllegalArgumentException("the supplied category and model are not" +
                    " for the right type of repository: " +
                    String.format("%s.%s", apiCategory, model));
            }
        } catch (RepositoryNotFoundException e) {
            throw new IllegalArgumentException("the repository for the category '" + apiCategory + "' and model '"
                + model + "' was not found", e);
        }

        Serializable pk;
        try {
            // cast the string id in the uriParts to the real pk class
            pk = castToPKClass((ReloadableEntityObjectRepository) repository, uuid);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "the supplied uuid could not be cast to a Primary Key class: " + uuid, e);
        }
        try {
            // disable the security as we only need to retrieve the object to further process the authorization
            context.turnOffAuthorisationSystem();
            return (BaseObjectRest) repository.findOne(context, pk);
        } finally {
            context.restoreAuthSystemState();
        }
    }
    
    public static Community[] extractDepAndSub(Community folder) throws SQLException {
		Community current  = folder;
		Community subDepartment = folder;
		
		while (current != null) {
			subDepartment = current;
			current = current.getParentCommunities().get(0);
			if (current.getParentCommunities().size() == 0) {
				return new Community[] {current, subDepartment};
			}
		}
		throw new SQLException("Cannot find department and subdepartment for folder: " + folder.getID().toString());
	}
    
    public static String extractPath(Community community) {
        // List to hold community names from leaf to root
        List<String> pathParts = new ArrayList<>();

        // Traverse up the community hierarchy
        Community currentCommunity = community;
        while (currentCommunity != null) {
            pathParts.add(currentCommunity.getName());

            // Move to parent if exists
            if (!currentCommunity.getParentCommunities().isEmpty()) {
                currentCommunity = currentCommunity.getParentCommunities().get(0);
            } else {
                currentCommunity = null;
            }
        }

        // Reverse to get path from top-level to leaf
        Collections.reverse(pathParts);

        // Join with "/" separator
        return String.join("/", pathParts);
    }
    
    public static void handlePdfFromString(Context context,HttpServletRequest request, HttpServletResponse response, StringBuilder pdfContent, String fileName) throws SQLException {
		// Creating a temp folder with time stamp(for uniqueness)
    	String filePath = System.getProperty("java.io.tmpdir") + File.separator + new Date().getTime();
    	File tempFolder = new File(filePath);
		if (!tempFolder.exists()) {
			tempFolder.mkdirs();
		}
		
		// Creating a temp file with provided fileName
		File tmpFile = new File(filePath, fileName);
    	try {
			Document document = new Document();
			PdfWriter.getInstance(document, new FileOutputStream(tmpFile));
			document.setPageSize(new Rectangle(841.9f,595.3f));
			document.open();
			
			document.add(new Paragraph(pdfContent.toString()));			
			document.close();
		} catch (Exception e) {
			log.error("Error while creating pdf from String content", e);
		}

		//Send pdf
		try {
			FileInputStream stream = new FileInputStream(tmpFile);
			
			response.setContentType("application/pdf");
			response.setHeader("Content-Length", String.valueOf(tmpFile.length()));
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".pdf");
            IOUtils.copy(stream, response.getOutputStream());
			stream.close();
			response.getOutputStream().flush();
			FileUtils.deleteDirectory(tempFolder);
		}
		catch(Exception e){
			log.error("Error while sending PDF file.", e);
		}
		finally {
			// adding entry in audittrail
			if(auditTrailService == null) {
				auditTrailService = AuditTrailServiceFactory.getInstance().getAudittrailService();
			}
			auditTrailService.logAction(context, "", AuditAction.DOWNLOAD_PDF_REPORT, fileName);
			if(tempFolder != null && tempFolder.exists()) {
				tempFolder.delete();
			}
		}
	}
    
    public static void handlePdf(Context context,HttpServletRequest request, HttpServletResponse response, List<String[]> metadataList, String fileName)throws SQLException{
		// Creating a temp folder with timestamp(for uniqueness)
    	String filePath = System.getProperty("java.io.tmpdir") + File.separator + new Date().getTime();
    	File tempFolder = new File(filePath);
		if (!tempFolder.exists()) {
			tempFolder.mkdirs();
		}
		
		// Creating a temp file with provided fileName
		File tmpFile = new File(filePath, fileName);
    	try {
			Document document = new Document();
			PdfWriter.getInstance(document, new FileOutputStream(tmpFile));
			document.setPageSize(new Rectangle(841.9f,595.3f));
			document.open();
			
			// iterating through metadata
			for (String[] strings : metadataList) {
				PdfPCell cell = new PdfPCell();
				cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
				PdfPTable table = new PdfPTable(strings.length);
				table.setWidthPercentage(100);
				if(strings.length == 1) {
					cell.setPaddingTop(15f);
					cell.setPaddingBottom(15f);
					cell.setPaddingLeft(7f);
					cell.setHorizontalAlignment(PdfPCell.ALIGN_LEFT);
				}
				for (String st : strings) {
					//if the data starts with http:// or https:// insert it as a link
					if (st == null || st.trim().isEmpty()) {
						st = "";
					}
					if (st.startsWith("http://") || st.startsWith("https://")) {
						Chunk chunk = new Chunk(st);
						chunk.setAnchor(st);
						cell.setPhrase(new Phrase(chunk));
					} else {
						cell.setPhrase(new Phrase(st));						
					}
					table.addCell(cell);
				}
				document.add(table);
			}			
			document.close();
		} catch (Exception e) {
			log.error("Error while creating pdf", e);
		}

		//Send pdf
		try {
			FileInputStream stream = new FileInputStream(tmpFile);
			
			response.setContentType("application/pdf");
			response.setHeader("Content-Length", String.valueOf(tmpFile.length()));
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".pdf");
            IOUtils.copy(stream, response.getOutputStream());
			stream.close();
			response.getOutputStream().flush();
			FileUtils.deleteDirectory(tempFolder);
		}
		catch(Exception e){
			log.error("Error while sending PDF file.", e);
		}
		finally {
			if(auditTrailService == null) {
				auditTrailService = AuditTrailServiceFactory.getInstance().getAudittrailService();
			}
			auditTrailService.logAction(context, "", AuditAction.DOWNLOAD_PDF_REPORT, fileName);
			if(tempFolder != null && tempFolder.exists()) {
				tempFolder.delete();
			}
		}
	}
    
    public static void handlePdfFromMapContent( Map<String, String> metadataMap, String filePath) {
		// Creating a temp file with provided fileName
		File tmpFile = new File(filePath);
    	try {
			Document document = new Document();
			PdfWriter.getInstance(document, new FileOutputStream(tmpFile));
			document.setPageSize(new Rectangle(841.9f,595.3f));
			document.open();
			
			// iterating through metadata
			PdfPTable table = new PdfPTable(2);
			table.setWidthPercentage(100);
			for (String key : metadataMap.keySet()) {
				PdfPCell cell = new PdfPCell();
				cell.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
				cell.setPhrase(new Phrase(key));	
				table.addCell(cell);
				
				PdfPCell cell2 = new PdfPCell();
				cell2.setHorizontalAlignment(PdfPCell.ALIGN_CENTER);
				cell2.setPhrase(new Phrase(metadataMap.get(key)));	
				table.addCell(cell2);
			}
			
			document.add(table);	
			document.close();
		} catch (Exception e) {
			log.error("Error while creating pdf", e);
		}
	}
    
    public static void handleCSV(Context context,HttpServletRequest request, HttpServletResponse response, List<String[]> metadataList, String fileName) throws SQLException {
		// Creating a temp folder with timestamp(for uniqueness)
    	String filePath = System.getProperty("java.io.tmpdir") + File.separator + new Date().getTime();
    	File tempFolder = new File(filePath);
		if (!tempFolder.exists()) {
			tempFolder.mkdirs();
		}
		
		// Creating a temp file with provided fileName
		File tmpFile = new File(filePath, fileName);
    	try {
    		StringBuilder reportString = new StringBuilder();
    		for (String[] metadataArray : metadataList) {
    			for (int count = 0; count < metadataArray.length; count++) {
    				String str = metadataArray[count];
    				if (count < metadataArray.length - 1) {
    					reportString.append(str).append(",");
    				} else {
    					reportString.append(str).append("\n");
    				}
    			}
    		}
    		
    		FileWriter writeToTmpFile = new FileWriter(tmpFile);
			writeToTmpFile.write(reportString.toString());
			writeToTmpFile.close();
    	} catch (Exception e) {
			log.error("Error while creating CSV", e);
		}

		//Send pdf
		try {
			FileInputStream stream = new FileInputStream(tmpFile);
			
			response.setContentType("text/csv");
			response.setHeader("Content-Length", String.valueOf(tmpFile.length()));
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".csv");
            IOUtils.copy(stream, response.getOutputStream());
			stream.close();
			response.getOutputStream().flush();
			
			FileUtils.deleteDirectory(tempFolder);
		}
		catch(Exception e){
			log.error("Error while sending CSV file.", e);
		}
		finally {
			// adding entry in audittrail
			if(auditTrailService == null) {
				auditTrailService = AuditTrailServiceFactory.getInstance().getAudittrailService();
			}
			auditTrailService.logAction(context,"", AuditAction.DOWNLOAD_CSV_REPORT, fileName);
    
			if(tempFolder != null && tempFolder.exists()) {
				tempFolder.delete();
			}
		}
	}
    
    public static void addAuditTrail(Context context, Item item, DSpaceObject dso, AuditAction action,Object... args) throws SQLException {

    	if(configurationService == null) {
    		configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    	}
		if(auditTrailService == null) {
			auditTrailService = AuditTrailServiceFactory.getInstance().getAudittrailService();
		}
		if(itemService == null) {
			itemService = ContentServiceFactory.getInstance().getItemService();
		}   	
    	
    	if(item.getHandle() != null && !item.getHandle().isEmpty()) {
    		auditTrailService.logAction(context, item.getHandle(), action, args);
    	} else if (item.getID() != null) {
    		auditTrailService.logAction(context, String.valueOf(item.getID()), action, args);
    	}

    }
    
    public static String getTaskLevelByCollectionUser(Context context, Group group) {
    	String level = "L1";
    	try {
    		String groupName = group.getName().toLowerCase();
			if (groupName.contains("finaleditor")) {
			    return "L3";
			} else if (groupName.contains("editor")) {
			    return "L2";
			}
		} catch (Exception e) {
			log.error("Error in getting levels of the task",e);
		}
		return level;
    }
    
	public static boolean checkUserPermissionInFolderHierarchy(Context context, Collection collection, String permission) throws SQLException {
	    EPerson currentUser = context.getCurrentUser();

	    if (authorizeService == null) {
			authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
		}
	    if (groupService == null) {
			groupService = EPersonServiceFactory.getInstance().getGroupService();
		}
	    
	    // Admin override
	    if (authorizeService.isAdmin(context)) {
	        return true;
	    }

	    // Collection-level groups
	    String collectionPermissionGroupName = String.join("_", collection.getName(), collection.getID().toString(), permission);
	    String collectionAdminGroupName = String.join("_", collection.getName(), collection.getID().toString(), FolderPermission.ADMIN.name());

	    Group collectionPermissionGroup = groupService.findByName(context, collectionPermissionGroupName);
	    Group collectionAdminGroup = groupService.findByName(context, collectionAdminGroupName);

	    boolean hasPermission =
	        (collectionPermissionGroup != null && groupService.isMember(context, currentUser, collectionPermissionGroup)) ||
	        (collectionAdminGroup != null && groupService.isMember(context, currentUser, collectionAdminGroup));

	    if (hasPermission) {
	        return true;
	    }

	    // Check communities (parent-level)
	    for (Community comm : collection.getCommunities()) {
	        String commPermissionGroupName = String.join("_", comm.getName(), comm.getID().toString(), permission);
	        String commAdminGroupName = String.join("_", comm.getName(), comm.getID().toString(), FolderPermission.ADMIN.name());

	        Group commPermissionGroup = groupService.findByName(context, commPermissionGroupName);
	        Group commAdminGroup = groupService.findByName(context, commAdminGroupName);

	        boolean hasCommPermission =
	            (commPermissionGroup != null && groupService.isMember(context, currentUser, commPermissionGroup)) ||
	            (commAdminGroup != null && groupService.isMember(context, currentUser, commAdminGroup));

	        if (hasCommPermission) {
	            return true;
	        }
	    }

	    return false;
	}
	
	public static String convertDateToString(Date parameter) {
		try {
			DateFormat formatter;
			formatter = new SimpleDateFormat("dd-MM-yyyy");
			String dateAsString = formatter.format(parameter);
			return dateAsString;
		} catch (IllegalArgumentException e) {
			log.error("Error in timestamp to string conversion " + parameter, e);
			return null;
		}
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
			
    	} catch (Exception e) {
			log.error("Error while deleting item : " + e);
		}
    }
	
	public static UUID createAnnotatedBitstream(Context context, UUID itemid ,InputStream inputStream, EPerson eperson, Bitstream oldBitstream) throws SQLException, AuthorizeException, IOException {
	   Item item = itemService.find(context, itemid);
	   Bitstream bs = null;
	   Bundle annotationBundle = null;
	   List<Bundle> bundles = itemService.getBundles(item, ANNOTAION_BUNDLE);
	   if(bundles.size() == 0) {
		   annotationBundle = bundleService.create(context, item, ANNOTAION_BUNDLE);
		   bundleService.update(context, annotationBundle);
	   }else {
		   annotationBundle = bundles.get(0);
	   }
	   bs = bitstreamService.create(context, annotationBundle, inputStream); 
	   bs.setName(context, oldBitstream.getName());
	   BitstreamFormat bf = bitstreamFormatService.guessFormat(context, bs);
       bitstreamService.setFormat(context, bs, bf);
       bitstreamService.update(context, bs);
       
       //Entry in User Bitstream Table
       userBitstreamService.insert(context, eperson, oldBitstream, bs, new Date(), item);
       context.commit();
	   return bs.getID();
   }


   public static UUID updateExistingAnnotatedBitstream(Context context, InputStream inputStream,EPerson eperson, Bitstream oldBitstream) throws SQLException, IOException {
	   UserBitstream userBitstream =  userBitstreamService.findByEperson(context, eperson, oldBitstream).get(0);
	   UUID annotatedBitstreamId = userBitstream.getVersionedBistream().getID();
	   Bitstream annotatedBitstream = bitstreamService.find(context, annotatedBitstreamId);
	   //replace versioned bitsteam content
	   bitstreamStorageService.store(context, annotatedBitstream, inputStream);
	   return annotatedBitstreamId;
   }
	
	public static boolean checkIfAnnotatedBitstreamExist(Context context, EPerson eperson, Bitstream oldBitstream)
			throws SQLException {
		initializeServices();

		List<UserBitstream> userBitstreamList = userBitstreamService.findByEperson(context, eperson, oldBitstream);
		return userBitstreamList.size() > 0 ? true : false;
	}
	
	private static void initializeServices() {
	    if (userBitstreamService == null) {
	        userBitstreamService = UserBitstreamFactory.getInstance().getUserBitstreamService();
	    }
	    if (itemService == null) {
	        itemService = ContentServiceFactory.getInstance().getItemService();
	    }
	    if (bitstreamService == null) {
	        bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
	    }
	    if (bundleService == null) {
	        bundleService = ContentServiceFactory.getInstance().getBundleService();
	    }
	    if (bitstreamFormatService == null) {
	        bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
	    }
	    if (epersonService == null) {
	        epersonService = EPersonServiceFactory.getInstance().getEPersonService();
	    }
	}
	
	public static List<String[]> parseCSVFileInList(File csvFile, boolean skipHeader) {
		FileReader fileReader = null;
		CSVReader reader = null;
		List<String[]> rows = new ArrayList<String[]>();

		if (!csvFile.exists()) {
			return rows;
		}

		try {
			fileReader = new FileReader(csvFile);
			reader = new CSVReader(fileReader);
			String[] line;
			if (skipHeader) {
				line = reader.readNext();
			}
			while ((line = reader.readNext()) != null) {
				rows.add(line);
			}
			fileReader.close();
			reader.close();
		} catch (Exception e) {
			log.error("Error while parsing CSV file", e);
		}
		return rows;
	}
	
	// get eperson's all permission for the specific item, as per Community_Colllection_Group.
	public static String getEpersonItemPermissions(Context context, Item item) throws SQLException {
		String epersonPermission = "";
		try {
			if (context == null || context.getCurrentUser() == null) {
				return epersonPermission;
			}
			// Find the path to top level community
			String pathToTopLevelCommunity = "";
			if (item.getOwningCollection() != null) {
				pathToTopLevelCommunity = getCollectionToTopLevelCommunityPath(item.getOwningCollection());
			} else if (item.getOwningCommunity() != null) {
				pathToTopLevelCommunity = getChildToParentCommunityPath(item.getOwningCommunity());
			}
			String[] paths = pathToTopLevelCommunity.split("_");

			// Creating the group-name and finding the list of the groups
			paths[paths.length - 1] = paths[paths.length - 1].replaceAll("\\s", "-");
			String groupNamePrefix = "";
			for (int i = paths.length - 1; i >= 0; --i) {
				groupNamePrefix += paths[i] + "_";
			}
			groupNamePrefix = groupNamePrefix.toLowerCase();
			
			// finding groups related to 
			List<Group> groupList = context.getCurrentUser().getGroups();
			StringBuilder localEpersonGroupsAuth = new StringBuilder();
			if (groupList != null && !groupList.isEmpty()) {
				List<String> authList = new ArrayList<String>();
				for (Group group : groupList) {
					String groupName = group.getName().toLowerCase();
					String authName = "";
					if (groupName.equalsIgnoreCase("Administrator") || groupName.contains("reviewer") || groupName.contains("edit")) {
						authName = "all";
					}
					else if(groupName.contains("read") || groupName.contains("view"))
					{
						authName= "read";
					}
					if (groupName.indexOf(groupNamePrefix) != -1) {
						authName = groupName.replace(groupNamePrefix, "");
					}
					if (!authName.isEmpty() && !authList.contains(authName)) {
						if (authName.equalsIgnoreCase("Admin")) {
							authName = "all";
						}
						localEpersonGroupsAuth.append(authName).append(",");
					}
				}
			}
			
			// stringify all eperson groups belongs to eperson
			epersonPermission = localEpersonGroupsAuth.toString();
			if (!epersonPermission.isEmpty()) {
				epersonPermission = epersonPermission.substring(0, epersonPermission.length() - 1);
			}
		} catch (SQLException e) {
			log.error("Error in fetching all group Access related to the item for current user", e);
		}
		return epersonPermission;
	}
	
	public static boolean checkIfUploadUser(Context context) {
    	boolean isPermitted = false;
    	List<Group> groups = context.getCurrentUser().getGroups();
    	for(Group g : groups) {
            String uploadGroupName = g.getName();
          	if (Utils.isPermitted(context, uploadGroupName, Constants.GroupPermission.UPLOAD.value) || 
          		Utils.isPermitted(context, uploadGroupName, Constants.GroupPermission.SUBMIT.value)) {
          		isPermitted = true;
          		break;
          	}
          }
		return isPermitted;
	}
	
	public static boolean isPermitted(Context context, String groupName, String permission) {
		try {
			if (groupService == null) {
				groupService = EPersonServiceFactory.getInstance().getGroupService();
			}
			if (authorizeService == null) {
				authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
			}
			
			String adminGroupName = groupName + Constants.GroupPermission.ADMIN.value;
			String permissionGroupName = null;
			if(groupName.contains("_")) {
				groupName = groupName.substring(0, groupName.lastIndexOf("_"));
				permissionGroupName = String.join("_", groupName,permission);
			}else {
				permissionGroupName = groupName + permission;
			}
			return (groupService.isMember(context, adminGroupName) || groupService.isMember(context, permissionGroupName) || authorizeService.isAdmin(context));
		} catch (SQLException e) {
			log.error("Error in checking if Eperson is Admin or group-member");
		}
		return false;
	}
	
	public static boolean isCollectionAdmin(Context context, String collectionGroupName) {
		try {
			if (groupService == null) {
				groupService = EPersonServiceFactory.getInstance().getGroupService();
			}
			if (authorizeService == null) {
				authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
			}
			String adminGroupName = collectionGroupName + Constants.GroupPermission.ADMIN.value;
			return (groupService.isMember(context, adminGroupName) || authorizeService.isAdmin(context));
		} catch (SQLException e) {
			log.error("Error in checking if Eperson is Admin or group-member");
		}
		return false;
	}
	
	public static String getChildToParentCommunityPath(Community childCommunity) throws SQLException {
		String path = childCommunity.getName() + "_";
		// This loop will exclude the child node from path
		while (childCommunity.getParentCommunities() != null && childCommunity.getParentCommunities().size() > 0) {

			if (childCommunity.getParentCommunities().size() > 0) {
				path += childCommunity.getParentCommunities().get(0).getName() + "_";
				childCommunity = childCommunity.getParentCommunities().get(0);
			}
		}
		return path;
	}
	
	public static String getCollectionToTopLevelCommunityPath(Collection collection) throws SQLException {
		String path = "";
		cs = ContentServiceFactory.getInstance().getCollectionService();
		if (collection != null) {
			Context context = (ContextUtil.obtainCurrentRequestContext() != null) ? ContextUtil.obtainCurrentRequestContext() : new Context();
			collection = cs.find(context, collection.getID());
			path = collection.getName() + "_";
			if (collection.getCommunities() != null) {
				Community itemOwningCommunity = collection.getCommunities().get(0);
				path += getChildToParentCommunityPath(itemOwningCommunity);
			}
		}
		
		
		return path;
	}
	
	public static String getTopToBottomHierarchy(Collection owningCollection) throws SQLException {
		String path = "";
		String pathToTopLevelCommunity = getCollectionToTopLevelCommunityPath(owningCollection);
		String[] paths = pathToTopLevelCommunity.split("_");

		for(int i = paths.length - 1;i >= 0; --i) {
			path += paths[i] +"/";
		}
		return path;
	}
	
	public static String getCollectionGroupName(Collection owningCollection) throws SQLException {
		String path = getTopToBottomHierarchy(owningCollection);
		return path.replace("/", "_");
	}
}
