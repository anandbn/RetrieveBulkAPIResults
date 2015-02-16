package com.comcast.salesforce.utils;

import com.sforce.async.AsyncApiException;
import com.sforce.async.BulkConnection;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;


public class ConnectionUtils {
	
	public static PartnerConnection getPartnerConnection(String username,String password,String loginUrl) throws Exception  {
		PartnerConnection connection=null;
		try { 
		   // login to the Force.com Platform
		   ConnectorConfig config = new ConnectorConfig();
		   config.setTraceMessage(true);
		   config.setUsername(username);
		   config.setPassword(password);
		   config.setAuthEndpoint(loginUrl);
		   connection = com.sforce.soap.partner.Connector.newConnection(config);
		} catch ( ConnectionException ce) {
		}
	   return connection;
		
	}
	public static PartnerConnection getPartnerConnection(ConnectorConfig connCfg) throws Exception  {
		return com.sforce.soap.partner.Connector.newConnection(connCfg);
	}

	/**
	 * Create the BulkConnection used to call Bulk API operations.
	 */
	public static BulkConnection getBulkConnection(ConnectorConfig partnerConfig,String apiVersion)
			throws ConnectionException, AsyncApiException {
		ConnectorConfig config = new ConnectorConfig();
		config.setSessionId(partnerConfig.getSessionId());
		// The endpoint for the Bulk API service is the same as for the normal
		// SOAP uri until the /Soap/ part. From here it's '/async/versionNumber'
		String soapEndpoint = partnerConfig.getServiceEndpoint();
		String restEndpoint = soapEndpoint.substring(0,
				soapEndpoint.indexOf("Soap/"))
				+ "async/" + apiVersion;
		config.setRestEndpoint(restEndpoint);
		// This should only be false when doing debugging.
		config.setCompression(true);
		// Set this to true to see HTTP requests and responses on stdout
		config.setTraceMessage(false);
		BulkConnection connection = new BulkConnection(config);
		return connection;
	}	

	public static void main(String[] args) throws Exception{
	}
	

}
