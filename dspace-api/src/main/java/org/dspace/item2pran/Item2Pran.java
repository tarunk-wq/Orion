package org.dspace.item2pran;

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
 * This entity represents mapping between PRAN and Item.
 * Table: item2pran
 */
@Entity
@Table(name = "item2pran")
public class Item2Pran extends DSpaceObject {

    // PRAN is used as primary key (same pattern as PAN, ACK)
    @Id
    @Column(name = "pran")
    private String pran;

    // Many PRAN entries can belong to one Item
    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    // Getters & Setters

    public String getPran() {
        return pran;
    }

    public void setPran(String pran) {
        this.pran = pran;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    // Required Overrides (DSpaceObject)

    // Used internally by DSpace (safe to return pran)
    @Override
    public String getName() {
        return pran;
    }

    // Type of object (keeping same as your other entities)
    @Override
    public int getType() {
        return Constants.ITEM;
    }
}