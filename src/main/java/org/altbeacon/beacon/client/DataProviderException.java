package org.altbeacon.beacon.client;

public class DataProviderException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2574842662565384114L;
	public DataProviderException() {
		super();
	}
	public DataProviderException(String msg) {
		super(msg);		
	}
	public DataProviderException(String msg, Throwable t) {
		super(msg, t);		
	}
}
