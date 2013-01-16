/**
 * 
 */
package com.saplo.api.client.manager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.saplo.api.client.ResponseCodes;
import com.saplo.api.client.SaploClient;
import com.saplo.api.client.SaploClientException;
import com.saplo.api.client.entity.JSONRPCRequestObject;

/**
 * A manager class for Authorization methods
 * 
 * @author progre55
 *
 */
public class SaploAuthManager {

	private SaploClient client;
	
	public SaploAuthManager(SaploClient clientToUse) {
		this.client = clientToUse;
	}
	
	public String accessToken(String apiKey, String secretKey) throws SaploClientException {
		
		String accessToken;
		JSONObject params = new JSONObject();
		try {
		params.put("api_key", apiKey);
		params.put("secret_key", secretKey);
		} catch(JSONException je) {
			throw new SaploClientException(ResponseCodes.CODE_JSON_EXCEPTION, je);
		}
		
		JSONRPCRequestObject message = new JSONRPCRequestObject(client.getNextId(), "auth.accessToken", params);
		Object rawResult = client.sendAndReceiveAndParseResponse(message);
		
//		Object rawResult = client.parseResponse(responseMessage);
//		Object rawResult = client.sendAndReceiveAndParseResponse(message);
		if(rawResult instanceof JSONObject)
			accessToken = ((JSONObject) rawResult).opt("access_token").toString();
		else
			accessToken = rawResult.toString();
		
		return accessToken;
	}
	
	public boolean invalidateToken() throws SaploClientException {
		JSONArray params = new JSONArray();

		client.sendAndReceive(new JSONRPCRequestObject(client.getNextId(), "auth.invalidateToken", params));
		
		return true;
	}
}
