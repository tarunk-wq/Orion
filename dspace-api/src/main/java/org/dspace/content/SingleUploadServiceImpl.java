package org.dspace.content;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.dao.SourceTokenDAO;
import org.dspace.content.SourceToken;
import org.dspace.content.service.BundleMapService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.SingleUploadService;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * SingleUploadServiceImpl
 *
 * This class contains the business logic for the Single Upload API.
 *
 * The controller performs: - Authorization - Input validation
 *
 * This service performs: - bundle_map validation - Item creation - Bundle
 * hierarchy creation - Bitstream creation
 *
 * The design follows the same flow as UploadBitstream (old tech) but
 * implemented using Spring service architecture.
 */
public class SingleUploadServiceImpl implements SingleUploadService {

	// Service used to check bundle_map table.
	@Autowired
	private BundleMapService bundleMapService;

	// Service used for creating and updating Items.
	@Autowired
	private ItemService itemService;

	@Autowired
	private BitstreamService bitstreamService;
	
	@Autowired
	private WorkspaceItemService workspaceItemService;
	
	// Service used for creating Bundles.
	@Autowired
	private BundleService bundleService;

	// Service used for retrieving collections.
	@Autowired
	private CollectionService collectionService;
	
	@Autowired
	private SourceTokenDAO sourceTokenDAO;

	/**
	 * Temporary fixed collection UUID, i need to see if this is to be used layer
	 * atfer fixing findbundleitem method
	 *
	 * All new items created by the API will be placed in this collection will
	 * replace this later with configuration if needed
	 */
	//private static final UUID FIXED_COLLECTION_UUID = UUID.fromString(" "); commented out for now due to unsure collection logic

	/**
	 * Main method executed by the controller.
	 *
	 * This method performs the entire upload workflow.
	 *
	 * Steps performed: 1. Split mapping (Parent|Child) 2. Validate mapping exists
	 * in bundle_map table 3. Create Item with metadata 4. Check or create parent
	 * bundle 5. Check or create child bundle 6. Decode Base64 file 7. Create
	 * Bitstream and attach to child bundle
	 */
	@Override
	public void handleSingleUpload(Context context, String bundle, String mapping, String base64File, String title)
			throws SQLException, AuthorizeException, IOException {

		/**
		 * Split mapping string into parent and child bundle names Example: "A|B"
		 * parentName = A childName = B
		 */
		String[] parts = mapping.split("\\|");

		String parentName = parts[0];
		String childName = parts[1];

		/**
		 * Validate that this mapping exists in bundle_map table
		 *
		 * This ensures only predefined bundle hierarchies are allowed
		 */
		validateMapping(context, bundle, parentName, childName);

		// Create a new Item and add metadata (dc.title)
		Item item = createItemWithMetadata(context, title);

		// Resolve bundle hierarchy
		Bundle workingBundle = resolveWorkingBundle(context, bundle, item);

		// Decode the Base64 file and create a Bitstream
		// The Bitstream is then attached to the child bundle
		addBitstream(context, workingBundle, base64File);

		// Update Item after all changes are complete
		itemService.update(context, item);
	}

	// Validates whether the given mapping exists in the bundle_map table
	// If the mapping does not exist,the upload should be rejected

	private void validateMapping(Context context, String bundle, String parent, String child) throws SQLException {

		boolean exists = bundleMapService.isValidMapping(context, bundle, parent, child);

		if (!exists) {
			throw new IllegalArgumentException("Mapping does not exist in bundle_map table");
		}
	}

	// Creates a new Item inside a predefined collection and adds metadata to it
	// Currently only dc.title is added

	private Item createItemWithMetadata(Context context, String title) throws SQLException, AuthorizeException {

		// Find the collection where the item should be created
		Collection collection = null;

		if (collection == null) {
			throw new IllegalArgumentException("Collection not found");
		}

		// Create a workspace item first (required in new tech)
		WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);

		// Get the actual Item object from the workspace item
		Item item = workspaceItem.getItem();
		
		// Add metadata: dc.title
		// Authority and confidence are not used for this field
		itemService.addMetadata(context, item, "dc", "title", null, null, title, null, -1, -1);

		// Save item changes
		itemService.update(context, item);

		return item;
	}

	/**
	 * This method determines the exact bundle where the uploaded file should
	 * finally be stored.
	 *
	 * The hierarchy is decided using the bundle_map table.
	 */
	private Bundle resolveWorkingBundle(Context context, String bundleName, Item item)
			throws SQLException, AuthorizeException {

		//Find the mapping in bundle_map table
		List<BundleMap> mappings = bundleMapService.findByBundle(context, bundleName);

		//For now use the first mapping
		BundleMap mapping = mappings.get(0);
		
		if (mappings == null || mappings.isEmpty()) {
		    throw new IllegalArgumentException("No bundle mapping found for " + bundleName);
		}

		//Extract bundle names and hierarchy
		String parentBundleName = mapping.getParentBundleName();
		String childBundleName = mapping.getChildBundleName();

		//Find or create the parent bundle inside Item
		Bundle parentBundle = null;

		//Search existing bundles attached to the item
		for (Bundle bundle : item.getBundles()) {

			if (bundle.getName().equalsIgnoreCase(parentBundleName)) {
				parentBundle = bundle;
				break;
			}
		}

		//If parent bundle does not exist, create it
		if (parentBundle == null) {

			parentBundle = bundleService.create(context, item, parentBundleName);
		}

		//Find or create the child bundle
		Bundle childBundle = null;

		// Check if child bundle already exists under parent
		for (Bundle subBundle : parentBundle.getSubBundles()) {

			if (subBundle.getName().equalsIgnoreCase(childBundleName)) {
				childBundle = subBundle;
				break;
			}
		}

		//If child bundle does not exist, create it
		if (childBundle == null) {

			childBundle = bundleService.create(context, null, childBundleName);

			//Attach the child bundle under parent bundle
			parentBundle.getSubBundles().add(childBundle);

			//Save changes
			bundleService.update(context, parentBundle);
		}

		//Final bundle where the file will be uploaded
		return childBundle;
	}

	// Decodes Base64 file and creates a Bitstream.
	// The Bitstream is stored inside the child bundle.
	private Bitstream addBitstream(Context context, Bundle childBundle, String base64File)
			throws IOException, SQLException, AuthorizeException {

		// Decode Base64 string into binary data.
		byte[] decodedBytes = Base64.getDecoder().decode(base64File);

		InputStream inputStream = new ByteArrayInputStream(decodedBytes);

		// Create Bitstream inside the child bundle.
		Bitstream bitstream = bitstreamService.create(context, childBundle, inputStream);

		// Assign a temporary file name.
		bitstream.setName(context, "uploaded_file");

		// Save Bitstream changes.
		bitstreamService.update(context, bitstream);

		// Update bundle.
		bundleService.update(context, childBundle);

		return bitstream;
	}
	
	/**
	 * Validate that the source exists in sourcetoken table
	 */
	@Override
	public boolean validateSource(Context context, String source) throws SQLException {

	    // Check if source parameter is empty
	    if (source == null || source.trim().isEmpty()) {
	        return false;
	    }

	    // Query DB for the source
	    SourceToken tokenRow = sourceTokenDAO.findBySource(context, source);

	    // If row exists -> source is valid
	    return tokenRow != null;
	}
	
	/**
	 * Validate token for a given source
	 */
	@Override
	public boolean validateToken(Context context, String source, String token) throws SQLException {

	    // Fetch token row from DB
	    SourceToken tokenRow = sourceTokenDAO.findBySource(context, source);

	    if (tokenRow == null) {
	        return false;
	    }

	    // Check if source is active
	    if (!tokenRow.isActive()) {
	        return false;
	    }

	    // Compare supplied token with stored token
	    return token != null && token.equals(tokenRow.getToken()); //check if token is sent then check if api request token matches db token
	}
}