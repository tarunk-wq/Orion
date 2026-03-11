package org.dspace.app.rest.model;

import java.util.List;
import java.util.UUID;

public class BatchDetailsRest extends DSpaceObjectRest {
	 public static final String NAME = "batchdetail";
	 public static final String PLURAL_NAME = "batchdetails";
	 public static final String CATEGORY = RestAddressableModel.BATCHDETAIL;
	
		private String batchName;
		
		private UUID[] itemIds;
		
		private String state;
		
		private Integer totalFiles;

		private Integer totalPages;
		
		private Integer fileSize;
		
		private Integer totalPdfs;
		
		private Integer mainPdfPageCount;
		
		private String Collectionname;
		
		private UUID collectionId;
		
		private String Communityname;
		
		private UUID communityId;
		
		private Integer itemIdslength;
		
	    private Integer workflowitemid; 

		private List<ItemRest> listOfItemsRest;
	    
	    private String rejectReason;
		
		public List<ItemRest> getListOfItemsRest() {
			return listOfItemsRest;
		}

		public void setListOfItemsRest(List<ItemRest> listOfItemsRest) {
			this.listOfItemsRest = listOfItemsRest;
		}
			
		public Integer getItemIdslength() {
			return itemIdslength;
		}

		public void setItemIdslength(Integer itemIdslength) {
			this.itemIdslength = itemIdslength;
		}

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

		public Integer getWorkflowitemid() {
			return workflowitemid;
		}
	
		public void setWorkflowitemid(Integer workflowitemid) {
			this.workflowitemid = workflowitemid;
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

		public UUID getCollectionId() {
			return collectionId;
		}

		public void setCollectionId(UUID collectionId) {
			this.collectionId = collectionId;
		}

		public String getCollectionname() {
			return Collectionname;
		}
	
		public void setCollectionname(String Collectionname) {
			this.Collectionname = Collectionname;
		}
	    
		public String getCommunityname() {
			return Communityname;
		}

		public void setCommunityname(String communityname) {
			Communityname = communityname;
		}

		public UUID getCommunityId() {
			return communityId;
		}

		public void setCommunityId(UUID communityId) {
			this.communityId = communityId;
		}

		public String getRejectReason() {
			return rejectReason;
		}

		public void setRejectReason(String rejectReason) {
			this.rejectReason = rejectReason;
		}

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
