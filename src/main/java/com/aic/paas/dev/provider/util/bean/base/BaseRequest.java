package com.aic.paas.dev.provider.util.bean.base;

public class BaseRequest {
	
	private String namespace;
	private String repo_name;

	
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	public String getRepo_name() {
		return repo_name;
	}
	public void setRepo_name(String repo_name) {
		this.repo_name = repo_name;
	}
	
}