package org.dspace.cbo2cho;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;

/**
 * Entity for cbo2cho table
 * Maps CBO → CHO relationship
 */
@Entity
@Table(name = "cbo2cho")
public class Cbo2Cho extends DSpaceObject {

    // Using cbo_no as primary key (same pattern as PAN, PRAN, etc.)
    @Id
    @Column(name = "cbo_no")
    private String cboNo;

    @Column(name = "cbo_name")
    private String cboName;

    @Column(name = "cho_no")
    private String choNo;

    // Getters & Setters

    public String getCboNo() {
        return cboNo;
    }

    public void setCboNo(String cboNo) {
        this.cboNo = cboNo;
    }

    public String getCboName() {
        return cboName;
    }

    public void setCboName(String cboName) {
        this.cboName = cboName;
    }

    public String getChoNo() {
        return choNo;
    }

    public void setChoNo(String choNo) {
        this.choNo = choNo;
    }

    // Required overrides

    @Override
    public String getName() {
        return cboNo;
    }

    @Override
    public int getType() {
        return Constants.ITEM;
    }
}