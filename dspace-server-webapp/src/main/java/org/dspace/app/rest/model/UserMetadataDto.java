package org.dspace.app.rest.model;

public class UserMetadataDto {
   private Integer id;
   private String name;
   private Integer type;
   private String value;
   private String systemFieldName;
   private Boolean isUniqueMetadata;
   
   public Boolean getIsUniqueMetadata() {
	   return isUniqueMetadata;
   }
   
   public void setIsUniqueMetadata (Boolean isUniqueMetadata) {
	   this.isUniqueMetadata = isUniqueMetadata;
   }
   
   public Integer getId() {
      return this.id;
   }

   public void setId(Integer id) {
      this.id = id;
   }

   public String getValue() {
      return this.value;
   }

   public void setValue(String value) {
      this.value = value;
   }

   public UserMetadataDto() {
   }

   public UserMetadataDto(int id, int type, String name, String value, String systemFieldName) {
      this.id = id;
      this.value = value;
	  this.name = name;
      this.type = type;
      this.systemFieldName = systemFieldName;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public int getType() {
      return this.type;
   }

   public void setType(int type) {
      this.type = type;
   }

   public String getSystemFieldName() {
	  return systemFieldName;
   }
	
   public void setSystemFieldName(String systemFieldName) {
	  this.systemFieldName = systemFieldName;
   }
   
   
}
