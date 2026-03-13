package org.dspace.content.dto;

import java.util.Map;

/**
 * SingleUploadRequest
 *
 * Represents the JSON body sent to the Single Upload API.
 *
 * Payload example:
 *
 * {
 *   "bundle": "A|B",
 *   "file": "base64encodedstring",
 *   "metadata": {
 *       "dc.title": "Example Title"
 *   }
 * }
 *
 * Additional fields replicate UploadBitstream API logic.
 */
public class SingleUploadRequest {

    /**
     * Bundle hierarchy string
     * Example: "A|B"
     */
    private String bundle;

    /**
     * Base64 encoded file
     */
    private String file;

    /**
     * Metadata map (currently dc.title)
     */
    private Map<String, String> metadata;

    //  UploadBitstream legacy fields 

    private String corporateAckNo;
    private String corporateSubAckNo;
    private String corporateContributionAckNo;

    private String retailSubAckNo;
    private String agencyAckNo;
    private String agentAckNo;
    private String individualAgentAckNo;

    private String corporateName;
    private String subscriberName;
    private String agencyName;
    private String agentName;

    private String aadhaar;
    private String email;
    private String contactNo;

    private String pan;
    private String agencyOrAgentPAN;
    private String pran;

    private String choNo;
    private String cboNo;
    private String cboName;

    private String agencyID;
    private String agentID;

    private String primary;
    private String primaryType;

    private String documentType;
    private String documentName;
    private String fileType;
    private String legacyType;

    private String createdBy;
    private String createdDate;
    private String requirementId;

    private boolean maskedDoc;

    //  Getters & Setters 

    public String getBundle() {
        return bundle;
    }

    public void setBundle(String bundle) {
        this.bundle = bundle;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public String getCorporateAckNo() {
        return corporateAckNo;
    }

    public void setCorporateAckNo(String corporateAckNo) {
        this.corporateAckNo = corporateAckNo;
    }

    public String getCorporateSubAckNo() {
        return corporateSubAckNo;
    }

    public void setCorporateSubAckNo(String corporateSubAckNo) {
        this.corporateSubAckNo = corporateSubAckNo;
    }

    public String getCorporateContributionAckNo() {
        return corporateContributionAckNo;
    }

    public void setCorporateContributionAckNo(String corporateContributionAckNo) {
        this.corporateContributionAckNo = corporateContributionAckNo;
    }

    public String getRetailSubAckNo() {
        return retailSubAckNo;
    }

    public void setRetailSubAckNo(String retailSubAckNo) {
        this.retailSubAckNo = retailSubAckNo;
    }

    public String getAgencyAckNo() {
        return agencyAckNo;
    }

    public void setAgencyAckNo(String agencyAckNo) {
        this.agencyAckNo = agencyAckNo;
    }

    public String getAgentAckNo() {
        return agentAckNo;
    }

    public void setAgentAckNo(String agentAckNo) {
        this.agentAckNo = agentAckNo;
    }

    public String getIndividualAgentAckNo() {
        return individualAgentAckNo;
    }

    public void setIndividualAgentAckNo(String individualAgentAckNo) {
        this.individualAgentAckNo = individualAgentAckNo;
    }

    public String getCorporateName() {
        return corporateName;
    }

    public void setCorporateName(String corporateName) {
        this.corporateName = corporateName;
    }

    public String getSubscriberName() {
        return subscriberName;
    }

    public void setSubscriberName(String subscriberName) {
        this.subscriberName = subscriberName;
    }

    public String getAgencyName() {
        return agencyName;
    }

    public void setAgencyName(String agencyName) {
        this.agencyName = agencyName;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAadhaar() {
        return aadhaar;
    }

    public void setAadhaar(String aadhaar) {
        this.aadhaar = aadhaar;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
    }

    public String getAgencyOrAgentPAN() {
        return agencyOrAgentPAN;
    }

    public void setAgencyOrAgentPAN(String agencyOrAgentPAN) {
        this.agencyOrAgentPAN = agencyOrAgentPAN;
    }

    public String getPran() {
        return pran;
    }

    public void setPran(String pran) {
        this.pran = pran;
    }

    public String getChoNo() {
        return choNo;
    }

    public void setChoNo(String choNo) {
        this.choNo = choNo;
    }

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

    public String getAgencyID() {
        return agencyID;
    }

    public void setAgencyID(String agencyID) {
        this.agencyID = agencyID;
    }

    public String getAgentID() {
        return agentID;
    }

    public void setAgentID(String agentID) {
        this.agentID = agentID;
    }

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public String getPrimaryType() {
        return primaryType;
    }

    public void setPrimaryType(String primaryType) {
        this.primaryType = primaryType;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getLegacyType() {
        return legacyType;
    }

    public void setLegacyType(String legacyType) {
        this.legacyType = legacyType;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    public boolean isMaskedDoc() {
        return maskedDoc;
    }

    public void setMaskedDoc(boolean maskedDoc) {
        this.maskedDoc = maskedDoc;
    }
}