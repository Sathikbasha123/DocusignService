package com.saaspe.docusign.model;

import java.util.Map;

public class API {
	private String cls;
	private String opp;
	private Map<String, Object> ipl;
	private Object rsp;

	public String getCls() {
		return cls;
	}

	public void setCls(String cls) {
		this.cls = cls;
	}

	public String getOpp() {
		return opp;
	}

	public void setOpp(String opp) {
		this.opp = opp;
	}

	public Map<String, Object> getIpl() {
		return ipl;
	}

	public void setIpl(Map<String, Object> ipl) {
		this.ipl = ipl;
	}

	public Object getRsp() {
		return rsp;
	}

	public void setRsp(Object rsp) {
		this.rsp = rsp;
	}

	@Override
	public String toString() {
		return "API [cls=" + cls + ", opp=" + opp + ", ipl=" + ipl + ", rsp=" + rsp + "]";
	}

}
