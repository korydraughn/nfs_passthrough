package org.irods.nfs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiResponse {
	
	@JsonProperty("error_code") public int errorCode;
	@JsonProperty("message")    public String message;
	
	public ApiResponse() {}

	public ApiResponse(int errorCode, String message) {
		this.errorCode = errorCode;
		this.message = message;
	}

}
