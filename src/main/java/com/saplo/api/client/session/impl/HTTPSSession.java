/**
 * 
 */
package com.saplo.api.client.session.impl;

import java.net.URI;
import java.util.HashMap;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;

import com.saplo.api.client.ClientProxy;
import com.saplo.api.client.session.Session;
import com.saplo.api.client.session.TransportRegistry;
import com.saplo.api.client.session.TransportRegistry.SessionFactory;

/**
 * @author progre55
 *
 */
public class HTTPSSession extends HTTPSessionApache {

	public HTTPSSession(URI uri, String params, RequestConfig requestConfig) {
		super(uri, params, requestConfig);
	}

	public HTTPSSession(URI uri, String params, ClientProxy proxy, RequestConfig requestConfig) {
		super(uri, params, proxy, requestConfig);
	}

	@Override
	protected Registry<ConnectionSocketFactory> registerScheme() {
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
			.register("https", SSLConnectionSocketFactory.getSocketFactory()).build();
		return socketFactoryRegistry;
	}
	
	
	static class SessionFactoryImpl implements SessionFactory {
		volatile HashMap<URI, Session> sessionMap = new HashMap<URI, Session>();
		
		public Session newSession(URI uri, String params, ClientProxy proxy, RequestConfig requestConfig) {
			Session session = sessionMap.get(uri);
			if (session == null) {
				synchronized (sessionMap) {
					session = sessionMap.get(uri);
					if(session == null) {
						if(null != proxy)
							session = new HTTPSSession(uri, params, proxy, requestConfig);
						else
							session = new HTTPSSession(uri, params, requestConfig);
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
		registry.registerTransport("https", new SessionFactoryImpl());
	}

	/**
	 * De-register this transport from the 'registry'
	 * @param registry registry
	 */
	public static void deregister(TransportRegistry registry) {
		registry.deregisterTransport("https");
	}


}
