package org.dspace.exception;

public class HttpStatusException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private int statusCode;
	
	public HttpStatusException(String message) {
		super(message);
	}
	
	public HttpStatusException(String message, int statusCode) {
		super(message);
		this.statusCode = statusCode;
	}
	
	public int getStatusCode() {
		return statusCode;
	}
}

	
