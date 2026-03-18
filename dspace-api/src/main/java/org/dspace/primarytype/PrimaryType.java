package org.dspace.primarytype;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;

@Entity
@Table(name = "primarytype")
public class PrimaryType extends DSpaceObject {

    @Id
    @Column(name = "primary_type_name")
    private String primaryTypeName;

    public String getPrimaryTypeName() {
        return primaryTypeName;
    }

    public void setPrimaryTypeName(String primaryTypeName) {
        this.primaryTypeName = primaryTypeName;
    }

    @Override
    public String getName() {
        return primaryTypeName;
    }

    @Override
    public int getType() {
        return Constants.ITEM;
    }
}