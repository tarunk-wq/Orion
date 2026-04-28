package org.dspace.item2agent;

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
 * This entity maps Agent ID to Item
 */
@Entity
@Table(name = "item2agent")
public class Item2Agent extends DSpaceObject {

    // Agent ID is primary key (same pattern as PAN, PRAN, ACK)
    @Id
    @Column(name = "agent_id")
    private String agentId;

    // Many agent entries can belong to one Item
    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    // Getters & Setters

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
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
        return agentId;
    }

    @Override
    public int getType() {
        return Constants.ITEM;
    }
}