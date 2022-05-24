package org.irods.nfs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiRequest {
	
	 @JsonProperty("op_code") public int opCode;
	 @JsonProperty("payload") public String payload;
}
