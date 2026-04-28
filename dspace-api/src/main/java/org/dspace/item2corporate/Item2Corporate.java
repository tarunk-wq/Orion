package org.dspace.item2corporate;

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
 * This entity maps CHO (Corporate ID) to Item
 */
@Entity
@Table(name = "item2corporate")
public class Item2Corporate extends DSpaceObject {

    // CHO number is primary key (same pattern as PAN, PRAN)
    @Id
    @Column(name = "cho_no")
    private String choNo;

    // Many corporate entries can belong to one Item
    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    // Getters & Setters

    public String getChoNo() {
        return choNo;
    }

    public void setChoNo(String choNo) {
        this.choNo = choNo;
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
        return choNo;
    }

    @Override
    public int getType() {
        return Constants.ITEM;
    }
}