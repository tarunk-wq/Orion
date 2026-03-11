package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TempAccessResponse {

	private TempAccessDTO tempAccess;
	private String accessUrl;

	public TempAccessResponse() {
	}

	public TempAccessResponse(TempAccessDTO tempAccess, String accessUrl) {
		this.tempAccess = tempAccess;
		this.accessUrl = accessUrl;
	}

	public TempAccessDTO getTempAccess() {
		return tempAccess;
	}

	public void setTempAccess(TempAccessDTO tempAccess) {
		this.tempAccess = tempAccess;
	}

	public String getAccessUrl() {
		return accessUrl;
	}

	public void setAccessUrl(String accessUrl) {
		this.accessUrl = accessUrl;
	}
}