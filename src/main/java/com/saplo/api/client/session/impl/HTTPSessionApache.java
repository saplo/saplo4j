/**
 * 
 */
package com.saplo.api.client.session.impl;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NoHttpResponseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.saplo.api.client.ClientError;
import com.saplo.api.client.ClientProxy;
import com.saplo.api.client.ResponseCodes;
import com.saplo.api.client.SaploClientException;
import com.saplo.api.client.entity.JSONRPCRequestObject;
import com.saplo.api.client.entity.JSONRPCResponseObject;
import com.saplo.api.client.session.Session;
import com.saplo.api.client.session.TransportRegistry;
import com.saplo.api.client.session.TransportRegistry.SessionFactory;

/**
 * @author progre55
 *
 */
public class HTTPSessionApache implements Session {

	private static final String encoding = "UTF-8";
	protected URI endpoint;
	protected String params;
	private final RequestConfig requestConfig;
	protected CloseableHttpClient httpClient;
	protected ClientProxy clientProxy;
	protected CredentialsProvider proxyCredentials;

	/**
	 * Main constructor
	 *  @param endpoint - a saplo api endpoint
	 * @param params - access_token="token_here"
	 * @param requestConfig x
	 */
	public HTTPSessionApache(URI endpoint, String params, RequestConfig requestConfig) {
		this(endpoint, params, null, requestConfig);
	}
	
	public HTTPSessionApache(URI endPoint, String params, ClientProxy clientProxy, RequestConfig requestConfig) {
		this.clientProxy = clientProxy;
		this.endpoint = endPoint;
		this.params = params;
		this.requestConfig = requestConfig;
		init();
	}

	/*
	 * Initialize the httpClient with a pooled connection manager
	 */
	protected void init() {

		PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registerScheme());
		
		// increase max total connection
		cm.setMaxTotal(50);
		cm.setDefaultMaxPerRoute(20);
		// increase max connections for our endpoint
		HttpHost saploHost = new HttpHost(endpoint.getHost(), (endpoint.getPort() > 0 ? endpoint.getPort() : 80));
		cm.setMaxPerRoute(new HttpRoute(saploHost), 40);
		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
				.setConnectionManager(cm)
				.setDefaultRequestConfig(requestConfig);
		if (clientProxy != null) {
			HttpHost proxy = new HttpHost(clientProxy.getHost(), clientProxy.getPort());
			httpClientBuilder.setProxy(proxy);
			if (clientProxy.isSecure()) {
				proxyCredentials = new BasicCredentialsProvider();
				proxyCredentials.setCredentials(new AuthScope(clientProxy.getHost(), clientProxy.getPort()), new UsernamePasswordCredentials(clientProxy.getUsername(), clientProxy.getPassword()));
				httpClientBuilder.setDefaultCredentialsProvider(proxyCredentials);
			}
		}

		this.httpClient = httpClientBuilder.build();
	}
	
	/*
	 * This method will be overridden by the SSL implementation to register an SSL socket factory
	 */
	protected Registry<ConnectionSocketFactory> registerScheme() {
		return RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", new PlainConnectionSocketFactory())
				.build();
	}

	/**
	 * Sends a given request to the Saplo API and returns a response got from the API,
	 * or throws a SaploClientException
	 * 
	 * @param message - a message to send
	 * @return response object got back from the API
	 * @throws SaploClientException error
	 */
	public JSONRPCResponseObject sendAndReceive(JSONRPCRequestObject message)
			throws SaploClientException {

		HttpPost httpost = new HttpPost(String.format("%s?%s",endpoint.toString(), params));

		ByteArrayEntity ent = new ByteArrayEntity(message.toString().getBytes(Charset.forName(encoding)));
		ent.setContentEncoding(encoding);
		ent.setContentType("application/json");
		httpost.setEntity(ent);

		try {
			// the main call that sends the request to the client
			HttpResponse response = httpClient.execute(httpost);
			HttpEntity entity = response.getEntity();
			int statusCode = response.getStatusLine().getStatusCode();

			// first we "consume" the entity so that the connection is returned to the pool
			String responseStr = "";
			if (entity != null) {
				responseStr = EntityUtils.toString(entity, "UTF-8");
			}
			
			if (statusCode != HttpStatus.SC_OK) {
				// probably the API is down..
				throw new SaploClientException(ResponseCodes.MSG_API_DOWN_EXCEPTION, ResponseCodes.CODE_API_DOWN_EXCEPTION, statusCode);
			}
			
			return processResponse(responseStr);

		} catch (ClientProtocolException e) {
			httpost.abort();
			throw new ClientError(e);
		} catch (NoHttpResponseException nr) {
			// TODO what code to send here? 404? for now, just send 777 
			// cause 404 is thrown when response.getStatusLine().getStatusCode() == 404 above
			throw new SaploClientException(ResponseCodes.MSG_API_DOWN_EXCEPTION, ResponseCodes.CODE_API_DOWN_EXCEPTION, 777);
		} catch (IOException e) {
			httpost.abort();
			throw new SaploClientException(e);
		}
	}

	/*
	 * parse the response string received from the API
	 */
	private JSONRPCResponseObject processResponse(String response) throws SaploClientException {
		JSONTokener tokener = new JSONTokener(response);
		Object rawResponseMessage;
		try {
			rawResponseMessage = tokener.nextValue();
		} catch (JSONException e) {
			throw new SaploClientException(ResponseCodes.MSG_MALFORMED_RESPONSE, ResponseCodes.CODE_MALFORMED_RESPONSE);
		}
		JSONObject responseMessage = (JSONObject) rawResponseMessage;
		if (null == responseMessage)
			throw new SaploClientException("Got invalid response type - " + rawResponseMessage);
		
		return new JSONRPCResponseObject(responseMessage);
	}
	
	public synchronized void setParams(String params) {
		this.params = params;
	}


	/**
	 * Close all the clients and clear the pool.
	 */
	public synchronized void close() throws IOException {
		httpClient.close();
	}

	static class SessionFactoryImpl implements SessionFactory {
		volatile HashMap<URI, Session> sessionMap = new HashMap<URI, Session>();

		public Session newSession(URI uri, String params, ClientProxy proxy, RequestConfig httpParams) {
			Session session = sessionMap.get(uri);
			if (session == null) {
				synchronized (sessionMap) {
					session = sessionMap.get(uri);
					if(session == null) {
						if(proxy != null)
							session = new HTTPSessionApache(uri, params, proxy, httpParams);
						else
							session = new HTTPSessionApache(uri, params, httpParams);
						sessionMap.put(uri, session);
					}
				}
			}
			return session;
		}
	}

	/**
	 * Register this transport in 'registry'
	 * @param registry registry
	 */
	public static void register(TransportRegistry registry) {
		registry.registerTransport("http", new SessionFactoryImpl());
	}

	/**
	 * De-register this transport from the 'registry'
	 * @param registry registry
	 */
	public static void deregister(TransportRegistry registry) {
		registry.deregisterTransport("http");
	}

}
