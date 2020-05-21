package org.minima.system.network;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.SocketException;

import org.minima.objects.TxPoW;
import org.minima.objects.base.MiniByte;
import org.minima.objects.base.MiniData;
import org.minima.system.backup.SyncPackage;
import org.minima.system.brains.ConsensusHandler;
import org.minima.system.brains.ConsensusNet;
import org.minima.utils.MinimaLogger;
import org.minima.utils.messages.Message;

public class NetClientReader implements Runnable {
	
	/**
	 * Greeting message that tells what Net Protocol this peer speaks, and a complete block chain header list. Any Blocks 
	 * the peer doesn't have he can request. Both peers send this to each other when they connect.
	 */
	public static final MiniByte NETMESSAGE_INTRO			= new MiniByte(0);
	
	/**
	 * The peer has a new TXPOW. This is the ID. If the peer 
	 * doesn't have it already he will request it 
	 */
	public static final MiniByte NETMESSAGE_TXPOWID			= new MiniByte(1);
	
	/**
	 * Request the full details of a TXPOW. Either the complete 
	 * txpow, or just the MMR proofs and updates.
	 */
	public static final MiniByte NETMESSAGE_TXPOW_REQUEST	= new MiniByte(2);
	
	/**
	 * Complete TXPOW. Only sent if the peer has requested it
	 */
	public static final MiniByte NETMESSAGE_TXPOW			= new MiniByte(3);
	
	
	/**
	 * Netclient owner
	 */
	NetClient 		mNetClient;
	
	/**
	 * Constructor
	 * 
	 * @param zNetClient
	 */
	public NetClientReader(NetClient zNetClient) {
		mNetClient 		= zNetClient;
	}

	@Override
	public void run() {
//		System.out.println("NetClientReader started");
		
		try {
			//Create an input stream
			DataInputStream mInput = new DataInputStream(new BufferedInputStream(mNetClient.getSocket().getInputStream()));
			
			//The message type
			MiniByte msgtype = new MiniByte();
			
			//The Consensus
			ConsensusHandler consensus = mNetClient.getNetworkHandler().getMainHandler().getConsensusHandler();
			
			while(true) {
				//What message type
				msgtype.readDataStream(mInput);
				
				//New Message received
				Message rec = new Message(ConsensusNet.CONSENSUS_PREFIX+"NET_MESSAGE_"+msgtype);
				
				//Always add the client
				rec.addObject("netclient", mNetClient);
				
				//Do we have a valid message
				boolean valid = true;
				
				//What kind of message is it..
				if(msgtype.isEqual(NETMESSAGE_INTRO)) {
					//Read in the SyncPackage
					SyncPackage sp = new SyncPackage();
					sp.readDataStream(mInput);
					
					//Add and send
					rec.addObject("sync", sp);
					
				}else if(msgtype.isEqual(NETMESSAGE_TXPOWID)) {
					//Peer now has this TXPOW - if you don't you can request the full version
					MiniData hash  = new MiniData();
					hash.readDataStream(mInput);
					
					//Add this ID
					rec.addObject("txpowid", hash);
					
				}else if(msgtype.isEqual(NETMESSAGE_TXPOW)) {
					//A complete TxPOW
					TxPoW tx = new TxPoW();
					tx.readDataStream(mInput);
					
					//Add this ID
					rec.addObject("txpow", tx);
					
				}else if(msgtype.isEqual(NETMESSAGE_TXPOW_REQUEST)) {
					//Requesting a TxPOW
					MiniData hash  = new MiniData();
					hash.readDataStream(mInput);
					
					//Add this ID
					rec.addObject("txpowid", hash);
				
				}else {
					valid = false;
					
					MinimaLogger.log("Invalid message on network : "+rec);
				}
				
				//Tell upstream
				if(valid) {
					//Post it..
					consensus.PostMessage(rec);
				}else {
					break;
				}
			}
		
		}catch(SocketException exc) {
			//Network error.. reset and reconnect..
		}catch(IOException exc) {
			//Network error.. reset and reconnect..
		}catch(Exception exc) {
			//This more serious error.. print it..
//			exc.printStackTrace();
//			MinimaLogger.log("NetClientReader closed UID "+mNetClient.getUID()+" exc:"+exc);
		}
		
		//Tell the network Handler
		mNetClient.getNetworkHandler().PostMessage(new Message(NetworkHandler.NETWORK_CLIENTERROR).addObject("client", mNetClient));
	}
}

