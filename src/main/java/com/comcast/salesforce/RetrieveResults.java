package com.comcast.salesforce;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.comcast.salesforce.utils.ConnectionUtils;
import com.comcast.salesforce.utils.EncryptionUtil;
import com.sforce.async.BatchInfo;
import com.sforce.async.BatchInfoList;
import com.sforce.async.BulkConnection;
import com.sforce.async.CSVReader;
import com.sforce.soap.partner.PartnerConnection;

import org.apache.commons.logging.*;
public class RetrieveResults {

	private static final Log log = LogFactory.getLog(RetrieveResults.class);
	
	private static final String JOBID_MATCH = "Created Bulk API Job";
	private static final String SUCCESS = "Sucess";
	private static final String ERROR = "Error";
	public static void main(String args[]) throws Exception{
		RetrieveResults launch = new RetrieveResults();
		launch.retrieveResults(args);
	}
	/**
	 * @param args
	 * @throws Exception 
	 */
	public void retrieveResults(String[] args) throws Exception {
		Options options = new Options();
		Properties props = new Properties();
		props.load(ClassLoader.getSystemResourceAsStream("dataloadutils.properties"));
		options.addOption("u",true,"Salesforce username");
		options.addOption("p",true,"Salesforce password");
		options.addOption("url",true,"Salesforce loginUrl");
		options.addOption("logFile",true,"Data loader log file");
		options.addOption("successFile",true,"Location to create Success CSV file");
		options.addOption("errorFile",true,"Location to create Error CSV file");
		options.addOption("operation",true,"update|upsert|insert");
		options.addOption("encryptionKeyFile",true,"Path to encryption key file");
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = parser.parse( options, args);
		if( !cmd.hasOption("u") || !cmd.hasOption("p") || 
			!cmd.hasOption("url") || !cmd.hasOption("logFile") ||
			!cmd.hasOption("successFile") || !cmd.hasOption("errorFile") ||
			!cmd.hasOption("operation")){
			HelpFormatter formater = new HelpFormatter();
		    formater.printHelp("RetrieveResults", options);
		    System.exit(-1);
		}else{
			log.info(String.format("Starting to retrieve results"));
			log.info(String.format("Username:%s",cmd.getOptionValue("u")));
			log.info(String.format("Password:*****"));
			log.info(String.format("loginUrl:%s",cmd.getOptionValue("url")));
			log.info(String.format("logFile:%s",cmd.getOptionValue("logFile")));
			log.info(String.format("successFile:%s",cmd.getOptionValue("successFile")));
			log.info(String.format("errorFile:%s",cmd.getOptionValue("errorFile")));
			log.info(String.format("encryptionKeyFile:%s",cmd.getOptionValue("encryptionKeyFile")));

			EncryptionUtil encUtil = new EncryptionUtil();
			if(cmd.hasOption("encryptionKeyFile")){
				log.info(String.format("Setting Encryption key file from %s",cmd.getOptionValue("encryptionKeyFile")));
				encUtil.setCipherKeyFromFilePath(cmd.getOptionValue("encryptionKeyFile"));
			}
			String jobId = getJobId(cmd.getOptionValue("logFile"));
			log.info(String.format("Fetched job id from logFile:%s",jobId));
			PartnerConnection pConn = ConnectionUtils.getPartnerConnection(	cmd.getOptionValue("u"),
																			encUtil.decryptString(cmd.getOptionValue("p")),
																			cmd.getOptionValue("url"));
			BulkConnection bulkConn = ConnectionUtils.getBulkConnection(pConn.getConfig(),props.getProperty("apiversion"));
			generateSuccessAndErrorFile(jobId,
										cmd.getOptionValue("successFile"),
										cmd.getOptionValue("errorFile"),
										bulkConn,
										cmd.getOptionValue("operation")
			);
		}
		

	}
	
	public String getJobId(String logFile) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(logFile));
		String jobId=null;
		try{
			String currLine=null;
			while((currLine = reader.readLine())!=null){
				int startIdx = currLine.indexOf(JOBID_MATCH);
				if(startIdx>-1){
					jobId = currLine.substring(	currLine.indexOf(':',startIdx)+1,
												currLine.length()
							);
					jobId  = jobId.replaceAll("^\\s+|\\s+$", "");
					break;
				}
			}
		}finally{
			if(reader!=null){
				reader.close();
			}
		}
		return jobId;
	}

	private void generateSuccessAndErrorFile(String jobId, String successFile,String errorFile,BulkConnection bulkConn,String operation)
			throws Exception {

		String batchId = null;
		BatchInfoList bList = bulkConn.getBatchInfoList(jobId);
		log.info(String.format("JobId: %s - Total Batches: %s",jobId,bList.getBatchInfo().length));
		File theFile = new File(successFile);
		if(theFile.exists()){
			log.info(String.format("Overwriting success file at %s",successFile));
		}
		
		theFile = new File(errorFile);
		if(theFile.exists()){
			log.info(String.format("Overwriting error file at %s",errorFile));
		}
		
		PrintWriter successFileOut = new PrintWriter(new FileOutputStream(successFile));
		PrintWriter errorFileOut = new PrintWriter(new FileOutputStream(errorFile));
		Integer batchIdx=1,successCnt=0,errorCnt=0,totalCnt=0;
		Map<String,Integer> recCounts;
		BatchInfo[] batchInfoList = bList.getBatchInfo();
		try{
			for (BatchInfo batchInfo : batchInfoList) {
					batchId = batchInfo.getId();
					log.info(String.format(	"JobId: %s - Starting to process BatchId: %s",jobId,batchId));
					recCounts = addToSuccessErrorFiles(	bulkConn.getBatchRequestInputStream(jobId, batchId),
														bulkConn.getBatchResultStream(jobId, batchId),
														successFileOut, errorFileOut,
														batchIdx,operation);
		
					successCnt+=recCounts.get(SUCCESS);
					errorCnt+=recCounts.get(ERROR);
					totalCnt =successCnt+errorCnt;
					log.info(String.format(	"JobId: %s - BatchId: %s [ %d of %d batches processed] - Successful: %s records, Errors: %s records added to success and error files",
											jobId,batchId,
											batchIdx,
											batchInfoList.length,
											recCounts.get(SUCCESS),
											recCounts.get(ERROR)
										)
					);
					batchIdx++;
					successFileOut.flush();
					errorFileOut.flush();
	
			}
		}finally{
			if(successFileOut !=null){
				successFileOut.close();
			}
			if(errorFileOut !=null){
				errorFileOut.close();
			}
		}
		log.info(String.format(	"JobId: %s - Total = %d, Success = %d, Errors = %d",jobId,totalCnt,successCnt,errorCnt));
		log.info(String.format(	"JobId: %s - Completed success and error file retrieval",jobId));

	}

	private  Map<String,Integer> addToSuccessErrorFiles(	InputStream batchIn,InputStream batchOut,
															PrintWriter successFile,PrintWriter errorFile,
															Integer batchNbr,String operation)
			throws Exception 
	{
		HashMap<String,Integer> recCounts= new HashMap<String,Integer>();
		CSVReader results = new CSVReader(new InputStreamReader(batchOut));
		BufferedReader request = new BufferedReader(new InputStreamReader(batchIn));
		List<String> resultHeader = results.nextRecord();
        int resultCols = resultHeader.size();
        Map<String, String> resultInfo;
        List<String> row;
        //Get header of the request;
		String currReqLine=request.readLine();
		
		Integer successCnt=0,errorCnt=0;
		//If this is the first batch the write the header to the success and error file
		if(batchNbr==1){
			writeHeaderLine(successFile,errorFile,currReqLine,operation);
		}
		Integer lineNbr=1;
		while((row=results.nextRecord())!=null){
			currReqLine = request.readLine();
            resultInfo = new HashMap<String, String>();
            for (int i = 0; i < resultCols; i++) {
                resultInfo.put(resultHeader.get(i), row.get(i));
            }
            boolean success = Boolean.valueOf(resultInfo.get("Success"));
            //boolean created = Boolean.valueOf(resultInfo.get("Created"));
            if (success) {
            	successCnt++;
            	writeSuccessLine(successFile,currReqLine,operation,resultInfo);
            } else if (!success) {
            	errorCnt++;
            	writeErrorLine(errorFile,currReqLine,operation,resultInfo);
            }
            log.debug(String.format("Processed Line#%s",lineNbr++));
		}
		recCounts.put(SUCCESS,successCnt);
		recCounts.put(ERROR,errorCnt);
		return recCounts;

	}
	
	private void writeHeaderLine(PrintWriter successFile,PrintWriter errorFile,String hdrLine,String operation) throws Exception{
		if("update".equalsIgnoreCase(operation)){
			String hdr = "\"ID\","+hdrLine +",\"STATUS\"";
			successFile.println(hdr);
			hdr = hdrLine +",\"ERROR\"";
			errorFile.println(hdr);
		}else if("insert".equalsIgnoreCase(operation)){
			String hdr = "\"ID\","+hdrLine +",\"STATUS\"";
			successFile.println(hdr);
			hdr = hdrLine +",\"ERROR\"";
			errorFile.println(hdr);
		}else {
			throw new Exception(String.format("Unsupported operation %s",operation));
		}		
	}

	private void writeSuccessLine(	PrintWriter successFile,
									String reqLine,String operation,
									Map<String, String> resultInfo) throws Exception{
		if("update".equalsIgnoreCase(operation)){
			successFile.println(String.format(	"\"%s\",%s,\"Item Updated\"",
												(resultInfo.get("Id")!=null?resultInfo.get("Id"):""),
												reqLine
											)
			);
		}else if("insert".equalsIgnoreCase(operation)){
			successFile.println(String.format(	"\"%s\",%s,\"Item Created\"",
					(resultInfo.get("Id")!=null?resultInfo.get("Id"):""),
					reqLine
				)
			);
		}else {
			throw new Exception(String.format("Unsupported operation %s",operation));
		}		
	}

	private void writeErrorLine(	PrintWriter errorFile,
									String reqLine,String operation,
									Map<String, String> resultInfo) throws Exception{
		if("update".equalsIgnoreCase(operation)){
			errorFile.println(reqLine+String.format(",\"%s\"",
													  resultInfo.get("Error")
													)
			);
		}else if("insert".equalsIgnoreCase(operation)){
			errorFile.println(reqLine+String.format(",\"%s\"",
					  		resultInfo.get("Error")
					)
			);
			
		}else {
			throw new Exception(String.format("Unsupported operation %s",operation));
		}		
	}

}
