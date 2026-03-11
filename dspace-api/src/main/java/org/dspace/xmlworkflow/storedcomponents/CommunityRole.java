/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xmlworkflow.storedcomponents;

import java.sql.SQLException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.dspace.content.Community;
import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.Group;

/**
 * Represents a workflow assignments database representation.
 * These assignments describe roles and the groups connected
 * to these roles for each community
 *
 * @author Abhijeet
 */
@Entity
@Table(name = "cwf_communityrole")
public class CommunityRole implements ReloadableEntity<Integer> {

    @Id
    @Column(name = "communityrole_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cwf_communityrole_seq")
    @SequenceGenerator(name = "cwf_communityrole_seq", sequenceName = "cwf_communityrole_seq", allocationSize = 1)
    private Integer id;

    @Column(name = "role_id", columnDefinition = "text")
    private String roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "community_id")
    private Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    protected CommunityRole() {

    }

    public void setRoleId(String id) {
        this.roleId = id;
    }

    public String getRoleId() {
        return roleId;
    }

    public Community getCommunity() {
		return community;
	}

	public void setCommunity(Community community) {
		this.community = community;
	}

	public void setGroup(Group group) {
        this.group = group;
    }

    public Group getGroup() throws SQLException {
        return group;
    }

    @Override
    public Integer getID() {
        return id;
    }
}
