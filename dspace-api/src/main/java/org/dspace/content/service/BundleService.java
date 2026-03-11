/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service interface class for the Bundle object.
 * The implementation of this class is responsible for all business logic calls for the Bundle object and is
 * autowired by spring
 *
 * @author kevinvandevelde at atmire.com
 */
public interface BundleService extends DSpaceObjectService<Bundle>, DSpaceObjectLegacySupportService<Bundle> {

    /**
     * Create a new bundle, with a new ID and link it to the provided item
     *
     * @param context DSpace context object
     * @param item    DSpace item
     * @param name    bundle name
     * @return the newly created bundle
     * @throws AuthorizeException if authorization error
     * @throws SQLException       if database error
     */
    public Bundle create(Context context, Item item, String name) throws SQLException, AuthorizeException;

    public Bitstream getBitstreamByName(Bundle bundle, String name);

    /**
     * Add an existing bitstream to this bundle
     *
     * @param context   DSpace Context
     * @param bundle    the bitstream bundle
     * @param bitstream the bitstream to add
     * @throws AuthorizeException if authorization error
     * @throws SQLException       if database error
     */
    public void addBitstream(Context context, Bundle bundle, Bitstream bitstream)
        throws SQLException, AuthorizeException;


    /**
     * Remove a bitstream from this bundle - the bitstream is only deleted if
     * this was the last reference to it
     * <p>
     * If the bitstream in question is the primary bitstream recorded for the
     * bundle the primary bitstream field is unset in order to free the
     * bitstream from the foreign key constraint so that the
     * <code>cleanup</code> process can run normally.
     *
     * @param context   DSpace Context
     * @param bundle    the bitstream bundle
     * @param bitstream the bitstream to remove
     * @throws IOException        if IO error
     * @throws AuthorizeException if authorization error
     * @throws SQLException       if database error
     */
    public void removeBitstream(Context context, Bundle bundle, Bitstream bitstream) throws AuthorizeException,
        SQLException, IOException;


    /**
     * remove all policies on the bundle and its contents, and replace them with
     * the DEFAULT_BITSTREAM_READ policies belonging to the collection.
     *
     * @param context    DSpace Context
     * @param bundle     the bitstream bundle
     * @param collection Collection
     * @throws SQLException       if an SQL error or if no default policies found. It's a bit
     *                            draconian, but default policies must be enforced.
     * @throws AuthorizeException if authorization error
     */
    public void inheritCollectionDefaultPolicies(Context context, Bundle bundle, Collection collection)
        throws java.sql.SQLException, AuthorizeException;

    /**
     * remove all of the policies for the bundle and bitstream contents and replace
     * them with a new list of policies
     *
     * @param context     DSpace Context
     * @param bundle      the bitstream bundle
     * @param newpolicies -
     *                    this will be all of the new policies for the bundle and
     *                    bitstream contents
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void replaceAllBitstreamPolicies(Context context, Bundle bundle, List<ResourcePolicy> newpolicies)
        throws SQLException, AuthorizeException;

    public List<ResourcePolicy> getBitstreamPolicies(Context context, Bundle bundle) throws SQLException;

    public List<ResourcePolicy> getBundlePolicies(Context context, Bundle bundle) throws SQLException;

    /**
     * Moves a bitstream within a bundle from one place to another, shifting all other bitstreams in the process
     *
     * @param context               DSpace Context
     * @param bundle                The bitstream bundle
     * @param from                  The index of the bitstream to move
     * @param to                    The index to move the bitstream to
     * @throws AuthorizeException   when an SQL error has occurred (querying DSpace)
     * @throws SQLException         If the user can't make the changes
     */
    public void updateBitstreamOrder(Context context, Bundle bundle, int from, int to) throws AuthorizeException,
            SQLException;

    /**
     * Moves a bitstream from its current bundle to a new target bundle
     * @param context               DSpace Context
     * @param targetBundle          The target bundle where bitstream will be moved to
     * @param bitstream             The bitstream being moved
     * @throws SQLException         if database error
     * @throws AuthorizeException   if authorization error
     * @throws IOException          if IO error
     */
    public void  moveBitstreamToBundle(Context context, Bundle targetBundle, Bitstream bitstream) throws SQLException,
            AuthorizeException, IOException;

    /**
     * Changes bitstream order according to the array
     *
     * @param context      DSpace Context
     * @param bundle       the bitstream bundle
     * @param bitstreamIds the identifiers in the order they are to be set
     * @throws SQLException       when an SQL error has occurred (querying DSpace)
     * @throws AuthorizeException If the user can't make the changes
     */
    public void setOrder(Context context, Bundle bundle, UUID bitstreamIds[]) throws AuthorizeException, SQLException;

    int countTotal(Context context) throws SQLException;
    
    /**
     * Create a sub-bundle under a parent bundle.
     *
     * This method follows the same delegation pattern used in
     * CommunityService#createSubcommunity.
     * It delegates to the overloaded methods for actual creation logic.
     *
     * @param context        current DSpace context
     * @param parentBundle   parent bundle under which child will be created
     * @return newly created child bundle
     * @throws SQLException         if a database error occurs
     * @throws AuthorizeException   if authorization fails
     */
	Bundle createSubBundle(Context context, Bundle parentBundle) throws SQLException, AuthorizeException;

    /**
     * Create a sub-bundle under a parent bundle with a specific name.
     *
     * This overload allows specifying the bundle name and delegates
     * to the main creation method that handles full logic.
     *
     * @param context        current DSpace context
     * @param parentBundle   parent bundle under which child will be created
     * @param name           name of the new child bundle
     * @return newly created child bundle
     * @throws SQLException         if a database error occurs
     * @throws AuthorizeException   if authorization fails
     */
	Bundle createSubBundle(Context context, Bundle parentBundle, String name) throws SQLException, AuthorizeException;
    
    /**
     * Create a sub-bundle under a parent bundle with a specific name and UUID.
     *
     *
     * @param context        current DSpace context
     * @param parentBundle   parent bundle
     * @param name           name of the new bundle
     * @param uuid           optional UUID (if provided)
     * @return newly created child bundle
     * @throws SQLException         if a database error occurs
     * @throws AuthorizeException   if authorization fails
     */
	Bundle createSubBundle(Context context, Bundle parentBundle, String name, UUID uuid)
			throws SQLException, AuthorizeException;
    
    /**
     * Link an existing child bundle to a parent bundle.
     *
     * This method establishes the bidirectional parent-child
     * relationship and registers the corresponding event.
     * Mirrors CommunityService#addSubcommunity behavior.
     *
     * @param context        current DSpace context
     * @param parentBundle   parent bundle
     * @param childBundle    child bundle to link
     * @throws SQLException         if a database error occurs
     * @throws AuthorizeException   if authorization fails
     */
	void addSubBundle(Context context, Bundle parentBundle, Bundle childBundle) throws SQLException, AuthorizeException;
    /**
     * Get all parent bundles of the given bundle.
     * This includes direct and indirect parents.
     *
     * @param context DSpace context
     * @param bundle child bundle
     * @return list of parent bundles
     * @throws SQLException if database error occurs
     */
	List<Bundle> getAllParentBundles(Context context, Bundle bundle) throws SQLException;

    /**
     * Remove a child bundle from a parent bundle hierarchy.
     *
     * This method unlinks the parent-child relationship between two bundles.
     * It does NOT delete the bundle entity itself — only the hierarchy link.
     * Mirrors the structure of CommunityService#removeSubcommunity,
     * but adapted for bundle hierarchy semantics.
     *
     * @param context       current DSpace context
     * @param parentBundle  parent bundle
     * @param childBundle   child bundle to unlink
     * @throws SQLException         if a database error occurs
     * @throws AuthorizeException   if authorization fails
     */
	void removeSubBundle(Context context, Bundle parentBundle, Bundle childBundle)
			throws SQLException, AuthorizeException, IOException;

    /**
     * Internal method to remove the bundle from database and clean up hierarchy.
     *
     * @param context current DSpace context
     * @param bundle bundle to delete
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization fails
     */
	void rawDelete(Context context, Bundle bundle) throws SQLException, AuthorizeException, IOException;


    /**
     * Get direct child bundles of the given bundle.
     *
     * @param context DSpace context
     * @param bundle parent bundle
     * @return list of child bundles
     * @throws SQLException if database error occurs
     */
	List<Bundle> getSubBundles(Context context, Bundle bundle) throws SQLException;


}
