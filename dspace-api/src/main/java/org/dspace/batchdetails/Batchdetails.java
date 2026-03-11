package org.dspace.batchdetails;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.dspace.batchhistory.BatchHistory;
import org.dspace.content.CacheableDSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "batchdetails")
public class Batchdetails extends CacheableDSpaceObject implements DSpaceObjectLegacySupport{

	@Column(name = "batch_name")
	private String batchName;
	
    @Column(name = "item_ids", columnDefinition = "uuid[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
	private UUID[] itemIds;
	
	@Column(name="state")
	private String state;
	
	@ManyToOne
    @JoinColumn(name = "workflowitem_id", nullable = true)
	@NotFound(action = NotFoundAction.IGNORE)
	private XmlWorkflowItem workflowId;

	@ManyToOne
	@JoinColumn(name="history_id")
	private BatchHistory historyid;

	@Column(name="total_files")
	private Integer totalFiles;

	@Column(name="total_pages")
	private Integer totalPages;
	
	@Column(name="file_size")
	private Integer fileSize;
	
	@Column(name="total_pdfs")
	private Integer totalPdfs;
	
	@Column(name="mainpdf_pagecount")
	private Integer mainPdfPageCount;

	public String getBatchName() {
		return batchName;
	}

	public void setBatchName(String batchName) {
		this.batchName = batchName;
	}

	public UUID[] getItemIds() {
		return itemIds;
	}

	public void setItemIds(UUID[] itemIds) {
		this.itemIds = itemIds;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public XmlWorkflowItem getWorkflowId() {
		return workflowId;
	}

	public void setWorkflowId(XmlWorkflowItem workflowId) {
		this.workflowId = workflowId;
	}

	public BatchHistory getHistoryid() {
		return historyid;
	}

	public void setHistoryid(BatchHistory historyid) {
		this.historyid = historyid;
	}
	
	public Integer getTotalFiles() {
		return totalFiles;
	}

	public void setTotalFiles(Integer totalFiles) {
		this.totalFiles = totalFiles;
	}

	public Integer getTotalPages() {
		return totalPages;
	}

	public void setTotalPages(Integer totalPages) {
		this.totalPages = totalPages;
	}

	public Integer getFileSize() {
		return fileSize;
	}

	public void setFileSize(Integer fileSize) {
		this.fileSize = fileSize;
	}

	public Integer getTotalPdfs() {
		return totalPdfs;
	}

	public void setTotalPdfs(Integer totalPdfs) {
		this.totalPdfs = totalPdfs;
	}

	public Integer getMainPdfPageCount() {
		return mainPdfPageCount;
	}

	public void setMainPdfPageCount(Integer mainPdfPageCount) {
		this.mainPdfPageCount = mainPdfPageCount;
	}

	@Override
	public Integer getLegacyId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getType() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

}