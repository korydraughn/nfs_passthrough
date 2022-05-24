package org.irods.nfs;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApiResponse
{
    // clang-format off
	@JsonProperty("error_code") public int errorCode;
	@JsonProperty("message")    public String message;
    // clang-format on

    public ApiResponse()
    {
    }

    public ApiResponse(int errorCode, String message)
    {
        this.errorCode = errorCode;
        this.message = message;
    }
}
