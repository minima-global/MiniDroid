package org.minima.system.network.rpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.Date;
import java.util.StringTokenizer;

import org.minima.system.network.commands.CMD;
import org.minima.system.network.commands.FILE;
import org.minima.system.network.commands.NET;
import org.minima.system.network.commands.SQL;
import org.minima.utils.MinimaLogger;

/**
 * This class handles a single request then exits
 * 
 * @author spartacusrex
 *
 */
public class RPCHandler implements Runnable {

	/**
	 * The Net Socket
	 */
	Socket mSocket;
	
	/**
	 * Main COnstructor
	 * @param zSocket
	 */
	public RPCHandler(Socket zSocket) {
		//Store..
		mSocket = zSocket;
	}

	@Override
	public void run() {
		// we manage our particular client connection
		BufferedReader in 	 		 	= null; 
		PrintWriter out 	 			= null; 
		String firstline = "no first line..";
		
		try {
			// Input Stream
			in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
			
			// Output Stream
			out = new PrintWriter(mSocket.getOutputStream());
			
			// get first line of the request from the client
			String input = in.readLine();
			firstline = new String(input);
			
			// we parse the request with a string tokenizer
			StringTokenizer parse = new StringTokenizer(input);
			String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client
			
			// we get file requested
			String fileRequested = parse.nextToken();
			
			//Get the Headers..
			String MiniDAPPID = "0x00";
			int contentlength = 0;
			while(input != null && !input.trim().equals("")) {
				int ref = input.indexOf("Referer:"); 
				if(ref != -1) {
					//Get the referer..
					int start  = input.indexOf("0x");
	        		int end    = -1;
	        		if(start!=-1) {
	        			end    = input.indexOf("/", start);
	        		}
	        		if(end!=-1) {
	        			MiniDAPPID = input.substring(start, end);
	        		}
				}else {
					ref = input.indexOf("Content-Length:"); 
					if(ref != -1) {
						//Get it..
						int start     = input.indexOf(":");
						contentlength = Integer.parseInt(input.substring(start+1).trim());
					}
				}
					
				input = in.readLine();
			}
			
			//The final result
			String finalresult = "";
			String reqtype     = "";
			String command     = "";
			
			//POST can handle longer messages
			if (method.equals("POST")){
				//Create a char buffer
				char[] cbuf = new char[contentlength];
				
				//Lets see..
				in.read(cbuf);
				
				//What is being asked..
				command = URLDecoder.decode(new String(cbuf),"UTF-8").trim();
				
				//Remove slashes..
				reqtype = new String(fileRequested);
				if(reqtype.startsWith("/")) {
					reqtype = reqtype.substring(1);
				}
				if(reqtype.endsWith("/")) {
					reqtype = reqtype.substring(0,reqtype.length()-1);
				}
				
				//Currently POST is the default..
			}else if (method.equals("GET")){
				//decode URL message
				String function = URLDecoder.decode(fileRequested,"UTF-8").trim();
				if(function.startsWith("/")) {
					function = function.substring(1);
				}
			
				if(function.startsWith("sql/")) {
					//Get the SQL function
					reqtype="sql";
					command = function.substring(4).trim();
					
				}else if(function.startsWith("net/")) {
					reqtype="net";
					command = function.substring(4).trim();
					
				}else if(function.startsWith("file/")) {
					reqtype="file";
					command = function.substring(5).trim();
					
				}else {
					reqtype="cmd";
					command = function.trim();
				}
			
			}else {
				throw new IOException("Unsupported Method in RPCHandler : "+firstline);
			}
			
			//MinimaLogger.log("RPCHandler "+method+" "+reqtype+" "+command);
			
			//Is this a SQL function
			if(reqtype.equals("sql")) {
				//Create a SQL object
				SQL sql = new SQL(command, MiniDAPPID);
				
				//Run it..
				sql.run();
				
				//Get the Response..
            	finalresult = sql.getFinalResult();
				
			}else if(reqtype.equals("cmd")) {
				CMD cmd = new CMD(command);
            	
            	//Run it..
            	cmd.run();
 
            	//Get the Response..
            	finalresult = cmd.getFinalResult();
			
			}else if(reqtype.equals("file")) {
				//File access..
				FILE file = new FILE(command, MiniDAPPID);
				
				//Run it..
				file.run();
				
				//Get the Response..
            	finalresult = file.getFinalResult();
			
			}else if(reqtype.equals("net")) {
				//Network Comms
				NET netcomm = new NET(command, MiniDAPPID);
				
				//Run it..
				netcomm.run();
				
				//Get the Response..
            	finalresult = netcomm.getFinalResult();
			}
			
			// send HTTP Headers
			out.println("HTTP/1.1 200 OK");
			out.println("Server: HTTP RPC Server from Minima : 1.0");
			out.println("Date: " + new Date());
			out.println("Content-type: text/plain");
			out.println("Content-length: " + finalresult.length());
			out.println("Access-Control-Allow-Origin: *");
			out.println(); // blank line between headers and content, very important !
			out.println(finalresult);
			out.flush(); // flush character output stream buffer
			
		} catch (Exception ioe) {
			MinimaLogger.log("RPCHANDLER : "+ioe+" "+firstline);
			ioe.printStackTrace();
			
		} finally {
			try {
				in.close();
				out.close();
				mSocket.close(); // we close socket connection
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			} 	
		}	
	}
}
