package org.dspace.content;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
@Entity
@Table(name = "item2agent_agency_pan")
public class Item2AgentAgencyPan extends DSpaceObject {

    @Id
    @Column(name = "pan")
    private String pan;

    @Column(name = "item_id")
    private Integer itemId;

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
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