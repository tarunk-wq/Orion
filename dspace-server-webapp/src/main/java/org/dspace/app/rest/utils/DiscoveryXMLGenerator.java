package org.dspace.app.rest.utils;

import java.io.File;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.Logger;
import org.dspace.usermetadata.UserMetadataFields;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DiscoveryXMLGenerator {
	
	private final static Logger logger = org.apache.logging.log4j.LogManager.getLogger(DiscoveryXMLGenerator.class);
	
	private static final String BEANS_TAG = "beans";
	private static final String BEAN_TAG = "bean";
	private static final String PROPERTY_TAG = "property";
	
	private static final String SORT_ID_PREFIX = "sort-";
	private static final String SEARCH_FILTER_ID_PREFIX = "searchFilter-";
	private static final String DISCOVERABLE = "discoverable";
	private static final String WITHDRAWN = "withdrawn";
	private static final String DESC_ID_SUFFIX = "-desc";

	public boolean generateXMLFile(String communityHandle, ArrayList<UserMetadataFields> metadataList, File outputFile) {
		
		boolean retVal = false;
		
		String[] handleVals = communityHandle.split("/");
		String uniqueHandlePart = handleVals[1];
				
		try {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			
			// Create the root element.
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement(BEANS_TAG);
			rootElement.setAttribute("xmlns", "http://www.springframework.org/schema/beans");
			rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
			rootElement.setAttribute("xmlns:context", "http://www.springframework.org/schema/context");
			rootElement.setAttribute("xmlns:util", "http://www.springframework.org/schema/util");
			rootElement.setAttribute("xsi:schemaLocation", "http://www.springframework.org/schema/beans "
					+ "http://www.springframework.org/schema/beans/spring-beans-3.0.xsd "
				    + "http://www.springframework.org/schema/context "
				    + "http://www.springframework.org/schema/context/spring-context-3.0.xsd "
				    + "http://www.springframework.org/schema/util "
				    + "http://www.springframework.org/schema/util/spring-util-3.0.xsd");
			rootElement.setAttribute("default-autowire-candidates", "*Service,*DAO,javax.sql.DataSource");
			doc.appendChild(rootElement);
			
			addDiscoveryConfigurationService(doc, rootElement, communityHandle, uniqueHandlePart);
			addDiscoveryConfiguration(doc, rootElement, uniqueHandlePart, metadataList);
			addWorkspaceDiscoveryConfiguration(doc, rootElement, uniqueHandlePart, metadataList);
			
			addsearchFilterWorkspaceConfiguration(doc, rootElement, DISCOVERABLE);
			addsearchFilterWorkspaceConfiguration(doc, rootElement, WITHDRAWN);
			
			for(UserMetadataFields field : metadataList) {
				addsearchFilterConfiguration(doc, rootElement, field);
				addSortConfiguration(doc, rootElement, field);
				addSortDescConfiguration(doc, rootElement, field);
			}
			
			// Write the content into xml file.
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(outputFile);
			
			transformer.transform(source, result);
			
			logger.info("XML file generated successfully");
			
			retVal = true;
		}catch(Exception e) {
			logger.error("Error in generating dicovery xml file.", e);
		}
		return retVal;
	}
	
	private String getElementOrQualifier(String systemFieldName) {
		
		return systemFieldName.substring(systemFieldName.lastIndexOf(".") + 1);
	}
	
	private void addDiscoveryConfigurationService(Document doc, Element rootElement, String handle, String uniqueHandlePart) {
		
		Element beanElement = doc.createElement(BEAN_TAG);
		beanElement.setAttribute("id", "org.dspace.discovery.configuration.DiscoveryConfigurationService-" + uniqueHandlePart);
		beanElement.setAttribute("class", "org.dspace.discovery.configuration.DiscoveryConfigurationService");
		
		Element propertyElement = doc.createElement(PROPERTY_TAG);
		propertyElement.setAttribute("name", "map");
		
		Element mapElement = doc.createElement("map");		
		Element entryElement = doc.createElement("entry");
		entryElement.setAttribute("key", handle);
		entryElement.setAttribute("value-ref", "discoveryConfiguration-"+uniqueHandlePart);
		mapElement.appendChild(entryElement);
		Element entryElement1 = doc.createElement("entry");
		entryElement1.setAttribute("key", "workspace." + handle);
		entryElement1.setAttribute("value-ref", "workspaceDiscoveryConfiguration-"+uniqueHandlePart);
		mapElement.appendChild(entryElement1);
		
		propertyElement.appendChild(mapElement);
		beanElement.appendChild(propertyElement);
		
		rootElement.appendChild(beanElement);
		
		Comment comment = doc.createComment("Config Map for User Defined Collections");
		rootElement.insertBefore(comment, beanElement);
	}
	
	private void addDiscoveryConfiguration(Document doc, Element rootElement, String uniqueHandlePart, ArrayList<UserMetadataFields> metadataList) {
		
		Element beanElement = doc.createElement(BEAN_TAG);
		beanElement.setAttribute("id", "discoveryConfiguration-"+uniqueHandlePart);
		beanElement.setAttribute("class", "org.dspace.discovery.configuration.DiscoveryConfiguration");
		beanElement.setAttribute("scope", "prototype");
		
		Element idPropertyElement = doc.createElement(PROPERTY_TAG);
		idPropertyElement.setAttribute("name", "id");
		idPropertyElement.setAttribute("value", "discoveryConfiguration-" + uniqueHandlePart);
		
		beanElement.appendChild(idPropertyElement);
		
		//<property name="sidebarFacets">\
		addSidebarFacetsConfiguration(doc, beanElement, metadataList);
		
		//<property name="searchFilters">
		addSearchFiltersConfiguration(doc, beanElement, metadataList);
		
		//<property name="searchSortConfiguration">
		addSearchSortConfiguration(doc, beanElement, metadataList);
		
		//<property name="defaultFilterQueries">
		addDefaultFilterQueries(doc, beanElement);
		
		//<property name="hitHighlightingConfiguration">
		addHitHighlightingConfiguration(doc, beanElement);
		
		//<property name="spellCheckEnabled" value="true"/>
		addSpellCheckConfiguration(doc, beanElement);
		
		rootElement.appendChild(beanElement);
		
		Comment comment = doc.createComment("Config for Advanced Search");
		rootElement.insertBefore(comment, beanElement);
	}
	
	private void addWorkspaceDiscoveryConfiguration(Document doc, Element rootElement, String uniqueHandlePart, ArrayList<UserMetadataFields> metadataList) {
		
		Element beanElement = doc.createElement(BEAN_TAG);
		beanElement.setAttribute("id", "workspaceDiscoveryConfiguration-"+uniqueHandlePart);
		beanElement.setAttribute("class", "org.dspace.discovery.configuration.DiscoveryConfiguration");
		beanElement.setAttribute("scope", "prototype");
		
		Element idPropertyElement = doc.createElement(PROPERTY_TAG);
		idPropertyElement.setAttribute("name", "id");
		idPropertyElement.setAttribute("value", "workspaceDiscoveryConfiguration-" + uniqueHandlePart);
		
		beanElement.appendChild(idPropertyElement);
		
		//<property name="sidebarFacets">\
		addSidebarFacetsWorkspaceConfiguration(doc, beanElement, metadataList);
		
		//<property name="searchFilters">
		addSearchFiltersWorkspaceConfiguration(doc, beanElement, metadataList);
		
		//<property name="searchSortConfiguration">
		addSearchSortConfiguration(doc, beanElement, metadataList);
		
		//<property name="defaultFilterQueries">
		addDefaultFilteWorkspacerQueries(doc, beanElement);
		
		//<property name="hitHighlightingConfiguration">
		addHitHighlightingConfiguration(doc, beanElement);
		
		//<property name="spellCheckEnabled" value="true"/>
		addSpellCheckConfiguration(doc, beanElement);
		
		rootElement.appendChild(beanElement);
		
		Comment comment = doc.createComment("Config for Administrative Search");
		rootElement.insertBefore(comment, beanElement);
	}
	
	private void addSearchFiltersConfiguration(Document doc, Element parentElement, ArrayList<UserMetadataFields> metadataList) {
		//<property name="searchFilters">
		Element searchFiltersPropertyElement = doc.createElement(PROPERTY_TAG);
		searchFiltersPropertyElement.setAttribute("name", "searchFilters");
		Element searchFilterslistElement = doc.createElement("list");
		
		for(UserMetadataFields field : metadataList) {
			Element refElement = doc.createElement("ref");
			refElement.setAttribute("bean", SEARCH_FILTER_ID_PREFIX + getElementOrQualifier(field.getSystemFieldName()));
			searchFilterslistElement.appendChild(refElement);
		}
		
		searchFiltersPropertyElement.appendChild(searchFilterslistElement);
		parentElement.appendChild(searchFiltersPropertyElement);
		
		Comment comment = doc.createComment("Add Search Filters for Advanced Search");
		parentElement.insertBefore(comment, searchFiltersPropertyElement);
	}
	
	private void addSearchFiltersWorkspaceConfiguration(Document doc, Element parentElement, ArrayList<UserMetadataFields> metadataList) {
		//<property name="searchFilters">
		Element searchFiltersPropertyElement = doc.createElement(PROPERTY_TAG);
		searchFiltersPropertyElement.setAttribute("name", "searchFilters");
		Element searchFilterslistElement = doc.createElement("list");
		
		Element refElement1 = doc.createElement("ref");
		refElement1.setAttribute("bean", SEARCH_FILTER_ID_PREFIX + DISCOVERABLE);
		searchFilterslistElement.appendChild(refElement1);
		
		Element refElement2 = doc.createElement("ref");
		refElement2.setAttribute("bean", SEARCH_FILTER_ID_PREFIX + WITHDRAWN);
		searchFilterslistElement.appendChild(refElement2);
		
		for(UserMetadataFields field : metadataList) {
			Element refElement = doc.createElement("ref");
			refElement.setAttribute("bean", SEARCH_FILTER_ID_PREFIX + getElementOrQualifier(field.getSystemFieldName()));
			searchFilterslistElement.appendChild(refElement);
		}
		
		searchFiltersPropertyElement.appendChild(searchFilterslistElement);
		parentElement.appendChild(searchFiltersPropertyElement);
		
		Comment comment = doc.createComment("Add Search Filter for Administrative Search");
		parentElement.insertBefore(comment, searchFiltersPropertyElement);
	}
	
	private void addSidebarFacetsConfiguration(Document doc, Element parentElement, ArrayList<UserMetadataFields> metadataList){
		//<property name="sidebarFacets">
//		Element sidebarFacetPropertyElement = doc.createElement(PROPERTY_TAG);
//		sidebarFacetPropertyElement.setAttribute("name", "sidebarFacets");
//		Element listElement = doc.createElement("list");
//		
//		sidebarFacetPropertyElement.appendChild(listElement);
//		parentElement.appendChild(sidebarFacetPropertyElement);
//		
//		Comment comment = doc.createComment("Comment");
//		parentElement.insertBefore(comment, sidebarFacetPropertyElement);
		
		
		Element sidebarFacetPropertyElement = doc.createElement(PROPERTY_TAG);
		sidebarFacetPropertyElement.setAttribute("name", "sidebarFacets");
		Element listElement = doc.createElement("list");
		
		for(UserMetadataFields field : metadataList) {
			Element refElement = doc.createElement("ref");
			refElement.setAttribute("bean", SEARCH_FILTER_ID_PREFIX + getElementOrQualifier(field.getSystemFieldName()));
			listElement.appendChild(refElement);
		}
		
		sidebarFacetPropertyElement.appendChild(listElement);
		parentElement.appendChild(sidebarFacetPropertyElement);
		
		Comment comment = doc.createComment("Add Sidebar Facets For Advanced Search");
		parentElement.insertBefore(comment, sidebarFacetPropertyElement);
	}
	
	private void addSidebarFacetsWorkspaceConfiguration(Document doc, Element parentElement, ArrayList<UserMetadataFields> metadataList){
		
		Element sidebarFacetPropertyElement = doc.createElement(PROPERTY_TAG);
		sidebarFacetPropertyElement.setAttribute("name", "sidebarFacets");
		Element listElement = doc.createElement("list");
		
		Element refElement1 = doc.createElement("ref");
		refElement1.setAttribute("bean", SEARCH_FILTER_ID_PREFIX + DISCOVERABLE);
		listElement.appendChild(refElement1);
		
		Element refElement2 = doc.createElement("ref");
		refElement2.setAttribute("bean", SEARCH_FILTER_ID_PREFIX + WITHDRAWN);
		listElement.appendChild(refElement2);
		
		for(UserMetadataFields field : metadataList) {
			Element refElement = doc.createElement("ref");
			refElement.setAttribute("bean", SEARCH_FILTER_ID_PREFIX + getElementOrQualifier(field.getSystemFieldName()));
			listElement.appendChild(refElement);
		}
		
		sidebarFacetPropertyElement.appendChild(listElement);
		parentElement.appendChild(sidebarFacetPropertyElement);
		
		Comment comment = doc.createComment("Add Sidebar Facets for Administrative Search");
		parentElement.insertBefore(comment, sidebarFacetPropertyElement);
	}
	
	private void addSearchSortConfiguration(Document doc, Element parentElement, ArrayList<UserMetadataFields> metadataList) {
		//<property name="searchSortConfiguration">
		Element sortConfigurationPropertyElement = doc.createElement(PROPERTY_TAG);
		sortConfigurationPropertyElement.setAttribute("name", "searchSortConfiguration");
		
		Element beanElement = doc.createElement(BEAN_TAG);
		beanElement.setAttribute("class", "org.dspace.discovery.configuration.DiscoverySortConfiguration");
		
//		<property name="defaultSortOrder" value="desc"/>;
//		Element sortFieldsOrderElement = doc.createElement(PROPERTY_TAG);
//		sortFieldsOrderElement.setAttribute("name", "defaultSortOrder");
//		sortFieldsOrderElement.setAttribute("value", "desc");
//		beanElement.appendChild(sortFieldsOrderElement);
		
		//<property name="sortFields">
		Element sortFieldsPropertyElement = doc.createElement(PROPERTY_TAG);
		sortFieldsPropertyElement.setAttribute("name", "sortFields");
		Element sortFieldslistElement = doc.createElement("list");
//		ArrayList<UserMetadataFields> metaList1 = new ArrayList<UserMetadataFields>();
//		if(metaList1.size() > 0)
//		{
			for(UserMetadataFields field : metadataList) {
				Element refElement = doc.createElement("ref");
				refElement.setAttribute("bean", SORT_ID_PREFIX + getElementOrQualifier(field.getSystemFieldName()));
				sortFieldslistElement.appendChild(refElement);
			}
			
			for(UserMetadataFields field : metadataList) {
				Element refElement = doc.createElement("ref");
				refElement.setAttribute("bean", SORT_ID_PREFIX + getElementOrQualifier(field.getSystemFieldName()) + DESC_ID_SUFFIX);
				sortFieldslistElement.appendChild(refElement);
			}
//		}
		
		sortFieldsPropertyElement.appendChild(sortFieldslistElement);
		beanElement.appendChild(sortFieldsPropertyElement);
		
		sortConfigurationPropertyElement.appendChild(beanElement);
		parentElement.appendChild(sortConfigurationPropertyElement);
		
		Comment comment = doc.createComment("Add Sorting Options for Search");
		parentElement.insertBefore(comment, sortConfigurationPropertyElement);
	}
	
	private void addHitHighlightingConfiguration(Document doc, Element parentElement) {
		//<property name="hitHighlightingConfiguration">
		Element hitHighlightingConfigurationPropertyElement = doc.createElement(PROPERTY_TAG);
		hitHighlightingConfigurationPropertyElement.setAttribute("name", "hitHighlightingConfiguration");
		
		Element beanElement = doc.createElement(BEAN_TAG);
		beanElement.setAttribute("class", "org.dspace.discovery.configuration.DiscoveryHitHighlightingConfiguration");
		
		//<property name="metadataFields">
		Element metadataFieldsPropertyElement = doc.createElement(PROPERTY_TAG);
		metadataFieldsPropertyElement.setAttribute("name", "metadataFields");
		Element listElement = doc.createElement("list");
		
		//<bean class="org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration">
		Element hitHighlightFieldBeanElement = doc.createElement(BEAN_TAG);
		hitHighlightFieldBeanElement.setAttribute("class", "org.dspace.discovery.configuration.DiscoveryHitHighlightFieldConfiguration");
		
		/*
		<property name="field" value="fulltext"/>
        <property name="maxSize" value="250"/>
        <property name="snippets" value="2"/>
		*/
		Element fulltextPropertyElement = doc.createElement(PROPERTY_TAG);
		fulltextPropertyElement.setAttribute("name", "field");
		fulltextPropertyElement.setAttribute("value", "fulltext");

		Element maxSizePropertyElement = doc.createElement(PROPERTY_TAG);
		maxSizePropertyElement.setAttribute("name", "maxSize");
		maxSizePropertyElement.setAttribute("value", "250");

		Element snippetsPropertyElement = doc.createElement(PROPERTY_TAG);
		snippetsPropertyElement.setAttribute("name", "snippets");
		snippetsPropertyElement.setAttribute("value", "2");

		
		hitHighlightFieldBeanElement.appendChild(fulltextPropertyElement);
		hitHighlightFieldBeanElement.appendChild(maxSizePropertyElement);
		hitHighlightFieldBeanElement.appendChild(snippetsPropertyElement);
		listElement.appendChild(hitHighlightFieldBeanElement);
		
		metadataFieldsPropertyElement.appendChild(listElement);
		beanElement.appendChild(metadataFieldsPropertyElement);
		
		hitHighlightingConfigurationPropertyElement.appendChild(beanElement);
		parentElement.appendChild(hitHighlightingConfigurationPropertyElement);
		
		Comment comment = doc.createComment("Add Search Highlight Config");
		parentElement.insertBefore(comment, hitHighlightingConfigurationPropertyElement);
	}
	
	private void addSpellCheckConfiguration(Document doc, Element parentElement) {
		Element spellCheckEnabledPropertyElement = doc.createElement(PROPERTY_TAG);
		spellCheckEnabledPropertyElement.setAttribute("name", "spellCheckEnabled");
		spellCheckEnabledPropertyElement.setAttribute("value", "true");
		parentElement.appendChild(spellCheckEnabledPropertyElement);
		
		Comment comment = doc.createComment("Add Spell Check Config");
		parentElement.insertBefore(comment, spellCheckEnabledPropertyElement);
	}
	
	private void addsearchFilterConfiguration(Document doc, Element rootElement, UserMetadataFields metadataField) {
		
		Element beanElement = doc.createElement(BEAN_TAG);
		beanElement.setAttribute("id", SEARCH_FILTER_ID_PREFIX + getElementOrQualifier(metadataField.getSystemFieldName()));
//		beanElement.setAttribute("class", "org.dspace.discovery.configuration.DiscoverySearchFilter");
		beanElement.setAttribute("class", "org.dspace.discovery.configuration.DiscoverySearchFilterFacet");
		
		//<property name="indexFieldName" value="title"/>
		Element indexFieldPropertyElement = doc.createElement(PROPERTY_TAG);
		indexFieldPropertyElement.setAttribute("name", "indexFieldName");
		indexFieldPropertyElement.setAttribute("value", getElementOrQualifier(metadataField.getSystemFieldName()));
		beanElement.appendChild(indexFieldPropertyElement);
		
		/*<property name="metadataFields">
            <list>
                <value>dc.title</value>
            </list>
         </property>
		*/
		Element metadataFieldPropertyElement = doc.createElement(PROPERTY_TAG);
		metadataFieldPropertyElement.setAttribute("name", "metadataFields");
		
		Element listElement = doc.createElement("list");		
		Element valueElement = doc.createElement("value");
		valueElement.setTextContent(metadataField.getSystemFieldName());
		listElement.appendChild(valueElement);
		
		metadataFieldPropertyElement.appendChild(listElement);
		beanElement.appendChild(metadataFieldPropertyElement);
		
		if(metadataField.getFieldType() == 2) {
			Element datePropertyElement = doc.createElement(PROPERTY_TAG);
			datePropertyElement.setAttribute("name", "type");
			datePropertyElement.setAttribute("value", "date");
			beanElement.appendChild(datePropertyElement);
		}
		if(metadataField.getFieldType() == 1) {
			Element datePropertyElement = doc.createElement(PROPERTY_TAG);
			datePropertyElement.setAttribute("name", "type");
			datePropertyElement.setAttribute("value", "integer");
			beanElement.appendChild(datePropertyElement);
		}
		/*<property name="facetLimit" value="5"/>
        <property name="sortOrderSidebar" value="COUNT"/>
        <property name="sortOrderFilterPage" value="COUNT"/>
        <property name="isOpenByDefault" value="true"/>
        <property name="pageSize" value="10"/>
        <property name="exposeMinAndMaxValue" value="true"/>*/
		Element facetPropertyElement = doc.createElement(PROPERTY_TAG);
		facetPropertyElement.setAttribute("name", "facetLimit");
		facetPropertyElement.setAttribute("value", "-1");
		beanElement.appendChild(facetPropertyElement);
		Element sortSidebarPropertyElement = doc.createElement(PROPERTY_TAG);
		sortSidebarPropertyElement.setAttribute("name", "sortOrderSidebar");
		sortSidebarPropertyElement.setAttribute("value", "COUNT");
		beanElement.appendChild(sortSidebarPropertyElement);
		Element sortFilterPropertyElement = doc.createElement(PROPERTY_TAG);
		sortFilterPropertyElement.setAttribute("name", "sortOrderFilterPage");
		sortFilterPropertyElement.setAttribute("value", "COUNT");
		beanElement.appendChild(sortFilterPropertyElement);
		Element openPropertyElement = doc.createElement(PROPERTY_TAG);
		openPropertyElement.setAttribute("name", "isOpenByDefault");
		openPropertyElement.setAttribute("value", "true");
		beanElement.appendChild(openPropertyElement);
		Element pagePropertyElement = doc.createElement(PROPERTY_TAG);
		pagePropertyElement.setAttribute("name", "pageSize");
		pagePropertyElement.setAttribute("value", "10");
		beanElement.appendChild(pagePropertyElement);
		Element exposeminmaxPropertyElement = doc.createElement(PROPERTY_TAG);
		exposeminmaxPropertyElement.setAttribute("name", "exposeMinAndMaxValue");
		exposeminmaxPropertyElement.setAttribute("value", "true");
		beanElement.appendChild(exposeminmaxPropertyElement);
		
		rootElement.appendChild(beanElement);
		
		Comment comment = doc.createComment("Search Filters for Advanced Search");
		rootElement.insertBefore(comment, beanElement);
	}
	
private void addsearchFilterWorkspaceConfiguration(Document doc, Element rootElement, String value) {
		
		Element beanElement = doc.createElement(BEAN_TAG);
		beanElement.setAttribute("id", SEARCH_FILTER_ID_PREFIX + value);
//		beanElement.setAttribute("class", "org.dspace.discovery.configuration.DiscoverySearchFilter");
		beanElement.setAttribute("class", "org.dspace.discovery.configuration.DiscoverySearchFilterFacet");
		
		//<property name="indexFieldName" value="title"/>
		Element indexFieldPropertyElement = doc.createElement(PROPERTY_TAG);
		indexFieldPropertyElement.setAttribute("name", "indexFieldName");
		indexFieldPropertyElement.setAttribute("value", value);
		beanElement.appendChild(indexFieldPropertyElement);
		
		/*<property name="metadataFields">
            <list>
            </list>
         </property>
		*/
		Element metadataFieldPropertyElement = doc.createElement(PROPERTY_TAG);
		metadataFieldPropertyElement.setAttribute("name", "metadataFields");
		
		Element listElement = doc.createElement("list");		
		
		metadataFieldPropertyElement.appendChild(listElement);
		beanElement.appendChild(metadataFieldPropertyElement);
		
		
		Element datePropertyElement = doc.createElement(PROPERTY_TAG);
		datePropertyElement.setAttribute("name", "type");
		datePropertyElement.setAttribute("value", "standard");
		beanElement.appendChild(datePropertyElement);
		
		/*<property name="isOpenByDefault" value="true"/>
        <property name="pageSize" value="10"/>*/
		Element openPropertyElement = doc.createElement(PROPERTY_TAG);
		openPropertyElement.setAttribute("name", "isOpenByDefault");
		openPropertyElement.setAttribute("value", "true");
		beanElement.appendChild(openPropertyElement);
		Element pagePropertyElement = doc.createElement(PROPERTY_TAG);
		pagePropertyElement.setAttribute("name", "pageSize");
		pagePropertyElement.setAttribute("value", "10");
		beanElement.appendChild(pagePropertyElement);
		
		rootElement.appendChild(beanElement);
		
		Comment comment = doc.createComment("SearchFilters For Administrative Search");
		rootElement.insertBefore(comment, beanElement);
	}
	
	private void addSortConfiguration(Document doc, Element rootElement, UserMetadataFields metadataField) {
		
		Element beanElement = doc.createElement(BEAN_TAG);
		beanElement.setAttribute("id", SORT_ID_PREFIX + getElementOrQualifier(metadataField.getSystemFieldName()));
		beanElement.setAttribute("class", "org.dspace.discovery.configuration.DiscoverySortFieldConfiguration");
		//element.setTextContent(fieldValue);
		
		Element propertyElement = doc.createElement(PROPERTY_TAG);
		propertyElement.setAttribute("name", "metadataField");
		propertyElement.setAttribute("value", metadataField.getSystemFieldName());
		beanElement.appendChild(propertyElement);
		
		if(metadataField.getFieldType() == 2) {
			Element datePropertyElement = doc.createElement(PROPERTY_TAG);
			datePropertyElement.setAttribute("name", "type");
			datePropertyElement.setAttribute("value", "date");
			
			beanElement.appendChild(datePropertyElement);
		}else if(metadataField.getFieldType() == 1) {
			Element intPropertyElement = doc.createElement(PROPERTY_TAG);
			intPropertyElement.setAttribute("name", "type");
			intPropertyElement.setAttribute("value", "integer");
			
			beanElement.appendChild(intPropertyElement);
		}
		
		Element sortFieldsOrderElement = doc.createElement(PROPERTY_TAG);
		sortFieldsOrderElement.setAttribute("name", "defaultSortOrder");
		sortFieldsOrderElement.setAttribute("value", "asc");
		beanElement.appendChild(sortFieldsOrderElement);
		
		rootElement.appendChild(beanElement);
		
		Comment comment = doc.createComment("Add Sort Ascending Config");
		rootElement.insertBefore(comment, beanElement);
	}
	
private void addSortDescConfiguration(Document doc, Element rootElement, UserMetadataFields metadataField) {
		
		Element beanElement = doc.createElement(BEAN_TAG);
		beanElement.setAttribute("id", SORT_ID_PREFIX + getElementOrQualifier(metadataField.getSystemFieldName()) + DESC_ID_SUFFIX);
		beanElement.setAttribute("class", "org.dspace.discovery.configuration.DiscoverySortFieldConfiguration");
		//element.setTextContent(fieldValue);
		
		Element propertyElement = doc.createElement(PROPERTY_TAG);
		propertyElement.setAttribute("name", "metadataField");
		propertyElement.setAttribute("value", metadataField.getSystemFieldName());
		beanElement.appendChild(propertyElement);
		
		if(metadataField.getFieldType() == 2) {
			Element datePropertyElement = doc.createElement(PROPERTY_TAG);
			datePropertyElement.setAttribute("name", "type");
			datePropertyElement.setAttribute("value", "date");
			
			beanElement.appendChild(datePropertyElement);
		}else if(metadataField.getFieldType() == 1) {
			Element intPropertyElement = doc.createElement(PROPERTY_TAG);
			intPropertyElement.setAttribute("name", "type");
			intPropertyElement.setAttribute("value", "integer");
			
			beanElement.appendChild(intPropertyElement);
		}
		
		Element sortFieldsOrderElement = doc.createElement(PROPERTY_TAG);
		sortFieldsOrderElement.setAttribute("name", "defaultSortOrder");
		sortFieldsOrderElement.setAttribute("value", "desc");
		beanElement.appendChild(sortFieldsOrderElement);
		
		rootElement.appendChild(beanElement);
		
		Comment comment = doc.createComment("Add Sort Descending Config");
		rootElement.insertBefore(comment, beanElement);
	}

private void addDefaultFilterQueries(Document doc, Element parentElement) {
	Element defaultFilterQueriesElement = doc.createElement(PROPERTY_TAG);
	defaultFilterQueriesElement.setAttribute("name", "defaultFilterQueries");
	Element listElement = doc.createElement("list");
	
	Element firstValueElement = doc.createElement("value");
    firstValueElement.setTextContent("(search.resourcetype:Item AND latestVersion:true) OR search.resourcetype:Collection OR search.resourcetype:Community");

    // Create the second value element
    Element secondValueElement = doc.createElement("value");
    secondValueElement.setTextContent("-withdrawn:true AND -discoverable:false");

    // Append both value elements to the list
    listElement.appendChild(firstValueElement);
    listElement.appendChild(secondValueElement);

    // Append the list to the defaultFilterQueries property element
    defaultFilterQueriesElement.appendChild(listElement);
	parentElement.appendChild(defaultFilterQueriesElement);
	Comment comment = doc.createComment("Default FIlter Queries for What to show on Advanced Search");
	parentElement.insertBefore(comment, defaultFilterQueriesElement);
}

private void addDefaultFilteWorkspacerQueries(Document doc, Element parentElement) {
	Element defaultFilterQueriesElement = doc.createElement(PROPERTY_TAG);
	defaultFilterQueriesElement.setAttribute("name", "defaultFilterQueries");
	Element listElement = doc.createElement("list");
	
	Element valueElement = doc.createElement("value");

	valueElement.setTextContent("(search.resourcetype:Item AND latestVersion:true) OR search.resourcetype:WorkspaceItem OR search.resourcetype:XmlWorkflowItem");

	// Append the value element to the list
	listElement.appendChild(valueElement);
	
	// Append the list to the defaultFilterQueries property element
	defaultFilterQueriesElement.appendChild(listElement);	
	parentElement.appendChild(defaultFilterQueriesElement);
	Comment comment = doc.createComment("Default FIlter Queries for What to show on Administrative Search");
	parentElement.insertBefore(comment, defaultFilterQueriesElement);
}
}
