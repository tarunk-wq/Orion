package org.dspace.item2agentagencypan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.dspace.content.Item;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
@Entity
@Table(name = "item2agent_agency_pan")
public class Item2AgentAgencyPan extends DSpaceObject {

    @Id
    @Column(name = "pan")
    private String pan;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    @Override
    public String getName() {
        return pan;
    }

    @Override
    public int getType() {
        return Constants.ITEM;
    }
}