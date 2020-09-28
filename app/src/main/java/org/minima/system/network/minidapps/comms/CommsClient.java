package org.minima.system.network.minidapps.comms;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.minima.objects.base.MiniData;
import org.minima.objects.base.MiniString;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.messages.Message;
import org.minima.utils.messages.MessageProcessor;

public class CommsClient extends MessageProcessor {
	
	public static final String COMMSCLIENT_INIT     = "COMMSCLIENT_INIT";
	public static final String COMMSCLIENT_START    = "COMMSCLIENT_START";
	public static final String COMMSCLIENT_SHUTDOWN = "COMMSCLIENT_SHUTDOWN";
	
	public static final String COMMSCLIENT_RECMESSAGE  = "COMMSCLIENT_RECMESSAGE";
	public static final String COMMSCLIENT_SENDMESSAGE = "COMMSCLIENT_SENDMESSAGE";
	
	//The main internal comms hub
	CommsManager mCommsManager;
	
	//The socket
	Socket mSocket;
	
	//Output streams
	DataOutputStream mOutput;
	
	Thread 		       mInputThread;
	CommsClientReader  mInputReader;
	
	//Create a UID
	String mUID = MiniData.getRandomData(20).to0xString();
			
	//The Host and Port
	String mHost;
	int    mPort;
	
	boolean mOutBound;
	
	String mMiniDAPPID;
	
	/**
	 * Constructor
	 * 
	 * @param zSock
	 * @param zNetwork
	 * @throws IOException 
	 * @throws UnknownHostException 
	 */
	public CommsClient(String zHost, int zPort, String zMiniDAPPID, CommsManager zCommsManager) {
		super("COMMSCLIENT");
		
		//Store
		mHost = zHost;
		mPort = zPort;
		mMiniDAPPID = zMiniDAPPID;
		mOutBound = true;
		
		mCommsManager = zCommsManager;
		
		//Start the connection
		PostMessage(COMMSCLIENT_INIT);
	}
	
	public CommsClient(Socket zSock, int zActualPort, String zMiniDAPPID, CommsManager zCommsManager) {
		super("COMMSCLIENT");
		
		//Store
		mSocket   = zSock;
		mOutBound = false;
		
		//Store
		mHost = mSocket.getInetAddress().getHostAddress();
		mPort = zActualPort;
		mMiniDAPPID = zMiniDAPPID;
		
		mCommsManager = zCommsManager;
		
		//Start the system..
		PostMessage(COMMSCLIENT_START);
	}
	
	public Socket getSocket() {
		return mSocket;
	}
	
	public String getHost() {
		return mHost;
	}
	
	public int getPort() {
		return mPort;
	}
	
	public boolean isOutBound() {
		return mOutBound;
	}
	
	public boolean isInBound() {
		return !mOutBound;
	}
	
	public String getMiniDAPPID() {
		return mMiniDAPPID;
	}
	
	public String getUID() {
		return mUID;
	}
	
	public JSONObject toJSON() {
		JSONObject ret = new JSONObject();
		
		ret.put("uid", mUID);
		ret.put("host", getHost());
		ret.put("port", getPort());
		ret.put("minidappid", mMiniDAPPID);
		ret.put("outbound", mOutBound);
		
		return ret;
	}
	
	@Override
	public String toString() {
		return toJSON().toString();
	}
	
	private void shutdown() {
		try {mOutput.close();}catch(Exception exc) {}
		try {mInputThread.interrupt();}catch(Exception exc) {}
		try {mSocket.close();}catch(Exception exc) {}
		
		stopMessageProcessor();
	}
	
	public void postSend(String zMessage) {
		Message msg = new Message(COMMSCLIENT_SENDMESSAGE);
		msg.addString("message", zMessage);
		PostMessage(msg);
	}
	
	@Override
	protected void processMessage(Message zMessage) throws Exception {
		
		//MinimaLogger.log("CommsClient "+mUID+" "+zMessage);
		
		if(zMessage.isMessageType(COMMSCLIENT_INIT)) {
			try {
				mSocket = new Socket();
				
				//Connect with timeout
				mSocket.connect(new InetSocketAddress(mHost, mPort), 60000);
				
			}catch (Exception e) {
				MinimaLogger.log("COMMS: Error @ connection start : "+mHost+":"+mPort+" "+e);
				
				//Shut down the client..
				Message shutdown = new Message(CommsClient.COMMSCLIENT_SHUTDOWN);
				shutdown.addString("minidappid", mMiniDAPPID);
				shutdown.addString("error", e.toString());
				PostMessage(shutdown);
				
				return;
			}	
			
			//Start the system..
			PostMessage(COMMSCLIENT_START);
			
		}else if(zMessage.isMessageType(COMMSCLIENT_START)) {
			//Create the streams on this thread
			mOutput 	= new DataOutputStream(mSocket.getOutputStream());
			
			//Start reading
			mInputReader = new CommsClientReader(this);
			mInputThread = new Thread(mInputReader, "CommsClientReader");
			mInputThread.start();
		
			//Post a message..
			Message newclient = new Message(CommsManager.COMMS_NEWCLIENT);
			newclient.addString("minidappid", mMiniDAPPID);
			newclient.addObject("client", this);
			
			mCommsManager.PostMessage(newclient);
			
		}else if(zMessage.isMessageType(COMMSCLIENT_RECMESSAGE)) {
			//Message received..
			String msg = zMessage.getString("message");
			
			//Pass it on..
			JSONObject netaction = new JSONObject();
			netaction.put("action", "message");
			
			netaction.put("port", getPort());
			netaction.put("host", getHost());
			netaction.put("hostport", getHost()+":"+getPort());
			netaction.put("uid", getUID());
			netaction.put("outbound", isOutBound());
			netaction.put("minidappid", mMiniDAPPID);
			
			netaction.put("message", msg);
			
			//Send it on..
			mCommsManager.postCommsMssage(netaction,mMiniDAPPID);
			
		}else if(zMessage.isMessageType(COMMSCLIENT_SENDMESSAGE)) {
			String message = zMessage.getString("message");
			sendMessage(new MiniString(message));
			
		}else if(zMessage.isMessageType(COMMSCLIENT_SHUTDOWN)) {
			shutdown();
			
			//And Notify the Manager..
			Message clientshut = new Message(CommsManager.COMMS_CLIENTSHUT);
			clientshut.addString("minidappid", mMiniDAPPID);
			clientshut.addObject("client", this);
			
			//Is there an error..
			if(zMessage.exists("error")) {
				clientshut.addString("error", zMessage.getString("error"));
			}
			
			mCommsManager.PostMessage(clientshut);
		}
	}
	
	
	
	/**
	 * Send a message down the network
	 */
	protected void sendMessage(MiniString zMessage) {
		//Send it..
		try {
			//Write to Stream
			zMessage.writeDataStream(mOutput);
			
			//Pump it!
			mOutput.flush();
			
		}catch(Exception ec) {
			//Show..
			//MinimaLogger.log("COMMS Error sending message : "+zMessage.toString()+" "+ec);
			
			//Shut down the client..
			Message shutdown = new Message(CommsClient.COMMSCLIENT_SHUTDOWN);
			shutdown.addString("minidappid", mMiniDAPPID);
			shutdown.addString("error", ec.toString());
			PostMessage(shutdown);
		}
	}	
}
