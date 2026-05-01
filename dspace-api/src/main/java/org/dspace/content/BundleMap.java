package org.dspace.content;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.UniqueConstraint;

import org.dspace.core.Constants;

/**
 * Hibernate Entity for bundle_map table.
 *
 * includes: - bundle - parent bundle name - child bundle name
 */
@Entity
@Table(name = "bundle_map", uniqueConstraints = {
		// Prevent duplicate parent-child mapping
		@UniqueConstraint(columnNames = { "bundle", "parent_bundle_name", "child_bundle_name" }) })
public class BundleMap extends DSpaceObject {

	/**
	 * Root bundle name Example: A|B
	 */
	@Column(name = "bundle", nullable = false)
	private String bundle;

	/**
	 * Parent bundle name Example: A
	 */
	@Column(name = "parent_bundle_name", nullable = false)
	private String parentBundleName;

	/**
	 * Child bundle name Example: B
	 */
	@Column(name = "child_bundle_name", nullable = false)
	private String childBundleName;

	// Required constructor for DSpaceObject
	protected BundleMap() {
		super();
	}

	// Getters and Setters

	public String getBundle() {
		return bundle;
	}

	public void setBundle(String bundle) {
		this.bundle = bundle;
	}

	public String getParentBundleName() {
		return parentBundleName;
	}

	public void setParentBundleName(String parentBundleName) {
		this.parentBundleName = parentBundleName;
	}

	public String getChildBundleName() {
		return childBundleName;
	}

	public void setChildBundleName(String childBundleName) {
		this.childBundleName = childBundleName;
	}

	@Override
	public String getName() {
		return bundle;
	}

	@Override
	public int getType() {
		return Constants.ITEM;
	}
}