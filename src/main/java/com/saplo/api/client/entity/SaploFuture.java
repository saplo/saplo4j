/**
 * 
 */
package com.saplo.api.client.entity;

import static com.saplo.api.client.ResponseCodes.CODE_STILL_PROCESSING;
import static com.saplo.api.client.ResponseCodes.CODE_UNKNOWN_EXCEPTION;
import static com.saplo.api.client.ResponseCodes.MSG_STILL_PROCESSING;
import static com.saplo.api.client.ResponseCodes.MSG_UNKNOWN_EXCEPTION;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.saplo.api.client.SaploClientException;

/**
 * A class to wrap around {@link Future} objects and convert {@link ExecutionException}s to {@link SaploClientException}
 * 
 * @author progre55
 */
public class SaploFuture<V> {

	private Future<V> future;
	
	public SaploFuture(Future<V> future) {
		this.future = future;
	}
	
	public V get() throws SaploClientException {
		try {
			return future.get();
		} catch (ExecutionException ee) {
			if(ee.getCause() instanceof SaploClientException)
				throw (SaploClientException)ee.getCause();
			else
				throw new SaploClientException(MSG_UNKNOWN_EXCEPTION, CODE_UNKNOWN_EXCEPTION, ee);
		} catch (InterruptedException ie) {
			throw new SaploClientException(MSG_UNKNOWN_EXCEPTION, CODE_UNKNOWN_EXCEPTION, ie);
		}
	}
	
	public V get(long timeout, TimeUnit unit) throws SaploClientException {
		try {
			return future.get(timeout, unit);
		} catch (ExecutionException ee) {
			if(ee.getCause() instanceof SaploClientException)
				throw (SaploClientException)ee.getCause();
			else
				throw new SaploClientException(MSG_UNKNOWN_EXCEPTION, CODE_UNKNOWN_EXCEPTION, ee);
		} catch (InterruptedException ie) {
			throw new SaploClientException(MSG_UNKNOWN_EXCEPTION, CODE_UNKNOWN_EXCEPTION, ie);
		} catch (TimeoutException te) {
			throw new SaploClientException(MSG_STILL_PROCESSING, CODE_STILL_PROCESSING, te);
		}
	}
	
	public void cancel(boolean mayInterruptIfRunning) {
		future.cancel(mayInterruptIfRunning);
	}
	
	public boolean isCancelled() {
		return future.isCancelled();
	}

	public boolean isDone() {
		return future.isDone();
	}
	
	
}
