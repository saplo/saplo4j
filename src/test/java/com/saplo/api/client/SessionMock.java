/**
 * 
 */
package com.saplo.api.client;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import com.saplo.api.client.entity.JSONRPCRequestObject;
import com.saplo.api.client.entity.JSONRPCResponseObject;
import com.saplo.api.client.manager.SaploAccountManager;
import com.saplo.api.client.manager.SaploAuthManager;
//import static org.mockito.Mockito.when;

/**
 * @author progre55
 *
 */
public class SessionMock {

	@Test
	public void someFirstTest() throws JSONException, HttpException, IOException, SaploClientException {
		
		SaploClient client = mock(SaploClient.class);
		
		when(client.sendAndReceive(any(JSONRPCRequestObject.class))).thenReturn(new JSONRPCResponseObject(new JSONObject("{\"result\":{\"access_token\":\"AT8413675556692900563\"}}")));
		when(client.parseResponse(any(JSONRPCResponseObject.class))).thenReturn(new JSONObject("{\"access_token\":\"AT8413675556692900563\"}"));
		
		SaploAuthManager authMgr = new SaploAuthManager(client);
		
		String token = authMgr.accessToken("", "");
		System.out.println(token);
	}
	
	
	// TODO srsly need to write some tests when I have time
	
	@Test
	public void testReconnection() throws SaploClientException, JSONException {
		
		SaploClient client = new SaploClient.Builder("apikey", "secretkey").accessToken("AT8413675556692900563").build();
		SaploClient spyClient = spy(client);
		SaploAccountManager saploAccountManager = new SaploAccountManager(spyClient);		
		SaploAccountManager spySaploAccountManager = spy(saploAccountManager);
		
		SaploAuthManager mockSaploAuthManager = mock(SaploAuthManager.class);
		
		when(spyClient.getAccountManager()).thenReturn(spySaploAccountManager);
		when(spyClient.getAuthManager()).thenReturn(mockSaploAuthManager);
		when(mockSaploAuthManager.accessToken("apikey", "secretkey")).thenReturn("AT8413675556692900555");
		
		spyClient.getAccountManager().get();
	}
	
}
