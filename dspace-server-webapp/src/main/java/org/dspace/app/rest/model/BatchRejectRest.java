package org.dspace.app.rest.model;

public class BatchRejectRest extends DSpaceObjectRest{
	public static final String NAME = "batchreject";
	 public static final String PLURAL_NAME = "batchrejects";
	 public static final String CATEGORY = RestAddressableModel.BATCHREJECT;
	 
	 private String reason;
	 
	 private String batchName;
	 
	 private Integer totalFiles;

	private Integer totalPages;
		
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	public String getBatchName() {
		return batchName;
	}
	public void setBatchName(String batchName) {
		this.batchName = batchName;
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
	public Integer getTotalPdfs() {
		return totalPdfs;
	}
	public void setTotalPdfs(Integer totalPdfs) {
		this.totalPdfs = totalPdfs;
	}
	private Integer totalPdfs;
	
	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return NAME;
	}
	@Override
	public String getCategory() {
		// TODO Auto-generated method stub
		return CATEGORY;
	}
	@Override
	public String getTypePlural() {
		// TODO Auto-generated method stub
		return PLURAL_NAME;
	}
}
