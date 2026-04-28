package org.dspace.item2ackno;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.dspace.content.Item;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;

@Entity
@Table(name = "item2ackno")
public class Item2AckNo extends DSpaceObject {

    @Id
    @Column(name = "ack_no")
    private String ackNo;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    // Getters & Setters

    public String getAckNo() {
        return ackNo;
    }

    public void setAckNo(String ackNo) {
        this.ackNo = ackNo;
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
        return ackNo;
    }

    @Override
    public int getType() {
        return Constants.ITEM;
    }
}