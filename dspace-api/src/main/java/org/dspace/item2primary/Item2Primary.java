package org.dspace.item2primary;

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
 * Maps Primary ID + Type to Item
 */
@Entity
@Table(name = "item2primary")
public class Item2Primary extends DSpaceObject {

    @Id
    @Column(name = "primary_id")
    private String primaryId;

    @Column(name = "primary_type")
    private String primaryType;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    // Getters & Setters

    public String getPrimaryId() {
        return primaryId;
    }

    public void setPrimaryId(String primaryId) {
        this.primaryId = primaryId;
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public void setPrimaryType(String primaryType) {
        this.primaryType = primaryType;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    @Override
    public String getName() {
        return primaryId;
    }

    @Override
    public int getType() {
        return Constants.ITEM;
    }
}