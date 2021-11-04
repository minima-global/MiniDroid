package org.minima.system.network;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.minima.system.network.minima.NIOManager;
import org.minima.system.network.p2p.P2PFunctions;
import org.minima.system.network.p2p.P2PManager;
import org.minima.system.params.GeneralParams;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONObject;
import org.minima.utils.messages.Message;
import org.minima.utils.messages.MessageProcessor;

public class NetworkManager {

	/**
	 * NIO Manager
	 */
	NIOManager mNIOManager;
	
	/**
	 * P2P Manager..
	 */
	MessageProcessor mP2PManager;
	
	public NetworkManager() {
		//Calculate the local host
		calculateHostIP();
		
		//Is the P2P Enabled
		if(GeneralParams.P2P_ENABLED) {
			//Create the Manager
			mP2PManager = new P2PManager();
		}else {
			//Create a Dummy listener.. 
			mP2PManager = new MessageProcessor("P2P_DUMMY") {
				@Override
				protected void processMessage(Message zMessage) throws Exception {
					if(zMessage.isMessageType(P2PFunctions.P2P_SHUTDOWN)) {
						stopMessageProcessor();
					}
				}
			};
		}
		
		//The main NIO server manager
		mNIOManager = new NIOManager();
	}
	
	public void calculateHostIP() {
		
		//Has it been specified at the commandline..?
		if(!GeneralParams.MINIMA_HOST.equals("")) {
			return;
		}
		
		//Cycle through all the network interfaces 
		try {
			boolean found = false;
		    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
	        while (!found && interfaces.hasMoreElements()) {
	            NetworkInterface iface = interfaces.nextElement();
	            // filters out 127.0.0.1 and inactive interfaces
	            if (iface.isLoopback() || !iface.isUp())
	                continue;

	            Enumeration<InetAddress> addresses = iface.getInetAddresses();
	            while(!found && addresses.hasMoreElements()) {
	                InetAddress addr = addresses.nextElement();
	                String ip   = addr.getHostAddress();
	                String name = iface.getDisplayName();
	                
	                //Only get the IPv4
	                if(!ip.contains(":")) {
	                	GeneralParams.MINIMA_HOST = ip;
	                	
	                	//If you're on WiFi..
	                	if(name.startsWith("wl")) {
	                		found = true;
	                		break;
	                	}
	                }
	            }
	        }
	    } catch (SocketException e) {
	        MinimaLogger.log("ERROR calculating host IP : "+e);
	    }
	}
	
	public JSONObject getStatus() {
		JSONObject stats = new JSONObject();
		
		stats.put("host", GeneralParams.MINIMA_HOST);
		stats.put("port", GeneralParams.MINIMA_PORT);
		stats.put("connecting", mNIOManager.getConnnectingClients());
		stats.put("connected", mNIOManager.getConnectedClients());
		
		//RPC Stats
		//..
		
		//P2P stats
		if(GeneralParams.P2P_ENABLED) {
			stats.put("p2p", ((P2PManager)mP2PManager).getStatus());
		}else {
			stats.put("p2p", "disabled");
		}
		
		return stats;
	}
	
	public void shutdownNetwork() {
		mNIOManager.PostMessage(NIOManager.NIO_SHUTDOWN);
		
		//Send a message to the P2P
		mP2PManager.PostMessage(P2PFunctions.P2P_SHUTDOWN);
	}
	
	public boolean isShutDownComplete() {
		return mNIOManager.isShutdownComplete() && mP2PManager.isShutdownComplete();
	}
	
	public MessageProcessor getP2PManager() {
		return mP2PManager;
	}
	
	public NIOManager getNIOManager() {
		return mNIOManager;
	}
}