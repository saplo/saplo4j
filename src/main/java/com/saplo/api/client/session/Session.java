package com.saplo.api.client.session;

import java.io.IOException;

import com.saplo.api.client.SaploClientException;
import com.saplo.api.client.entity.JSONRPCRequestObject;
import com.saplo.api.client.entity.JSONRPCResponseObject;

/**
 * Transport session. May have state associated with it.
 * */
public interface Session {
	/**
	 * Send JSON message and receive the result
	 * 
	 * @param message - A JSON message to send
	 * @return the JSON result message
	 * @throws SaploClientException 
	 */
	JSONRPCResponseObject sendAndReceive(JSONRPCRequestObject message) throws SaploClientException;

	/**
	 * Set a params value to the session (usually the jsessionid 
	 * or access_token param)
	 */
	void setParams(String params);

	/**
	 * Close the session and release the resources if necessary
	 */
	void close() throws IOException;
}
