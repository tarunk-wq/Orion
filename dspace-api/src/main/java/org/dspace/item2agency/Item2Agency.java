package org.dspace.item2agency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.dspace.content.Item;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;

/**
 * This entity maps Agency ID to Item
 */
@Entity
@Table(name = "item2agency")
public class Item2Agency extends DSpaceObject {

    // Agency ID is primary key (same pattern as others)
    @Id
    @Column(name = "agency_id")
    private String agencyId;

    // Many agency entries can belong to one Item
    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    // Getters & Setters

    public String getAgencyId() {
        return agencyId;
    }

    public void setAgencyId(String agencyId) {
        this.agencyId = agencyId;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    // Required Overrides

    @Override
    public String getName() {
        return agencyId;
    }

    @Override
    public int getType() {
        return Constants.ITEM;
    }
}