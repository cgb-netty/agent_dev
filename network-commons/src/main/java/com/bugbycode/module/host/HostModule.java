package com.bugbycode.module.host;

import java.io.Serializable;

public class HostModule implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3888808871401284011L;
	
	private int id;
	
	private String host;
	
	private int forward;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getForward() {
		return forward;
	}

	public void setForward(int forward) {
		this.forward = forward;
	}

}
