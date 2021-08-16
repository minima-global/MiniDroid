package org.minima.system.brains;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Set;
import java.util.StringTokenizer;

import org.minima.GlobalParams;
import org.minima.database.MinimaDB;
import org.minima.database.mmr.MMRData;
import org.minima.database.mmr.MMREntry;
import org.minima.database.mmr.MMRProof;
import org.minima.database.mmr.MMRSet;
import org.minima.database.prefs.UserPrefs;
import org.minima.database.txpowdb.TxPOWDBRow;
import org.minima.database.txpowdb.TxPowDB;
import org.minima.database.userdb.UserDBRow;
import org.minima.kissvm.Contract;
import org.minima.kissvm.values.BooleanValue;
import org.minima.kissvm.values.HexValue;
import org.minima.kissvm.values.NumberValue;
import org.minima.kissvm.values.StringValue;
import org.minima.kissvm.values.Value;
import org.minima.objects.Address;
import org.minima.objects.Coin;
import org.minima.objects.StateVariable;
import org.minima.objects.Transaction;
import org.minima.objects.TxPoW;
import org.minima.objects.Witness;
import org.minima.objects.base.MiniData;
import org.minima.objects.base.MiniNumber;
import org.minima.objects.base.MiniString;
import org.minima.objects.keys.MultiKey;
import org.minima.objects.proofs.ScriptProof;
import org.minima.objects.proofs.TokenProof;
import org.minima.system.Main;
import org.minima.system.input.InputHandler;
import org.minima.system.input.functions.gimme50;
import org.minima.utils.Crypto;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.messages.Message;

public class ConsensusUser extends ConsensusProcessor {


	public static final String CONSENSUS_PREFIX 			= "CONSENSUSUSER_";
	
	public static final String CONSENSUS_NEWKEY 			= CONSENSUS_PREFIX+"NEWKEY";
	
	public static final String CONSENSUS_SIGN 			    = CONSENSUS_PREFIX+"SIGN";
	public static final String CONSENSUS_VERIFY 			= CONSENSUS_PREFIX+"VERIFY";
	
	public static final String CONSENSUS_NEWSIMPLE 			= CONSENSUS_PREFIX+"NEWSIMPLE";
	public static final String CONSENSUS_NEWSCRIPT 			= CONSENSUS_PREFIX+"NEWSCRIPT";
	public static final String CONSENSUS_EXTRASCRIPT 		= CONSENSUS_PREFIX+"EXTRASCRIPT";
	public static final String CONSENSUS_RUNSCRIPT 			= CONSENSUS_PREFIX+"RUNSCRIPT";
	public static final String CONSENSUS_CLEANSCRIPT 		= CONSENSUS_PREFIX+"CLEANSCRIPT";
	
	public static final String CONSENSUS_CURRENTADDRESS 	= CONSENSUS_PREFIX+"CURRENTADDRESS";
	
	public static final String CONSENSUS_KEEPCOIN 			= CONSENSUS_PREFIX+"KEEPCOIN";
	public static final String CONSENSUS_UNKEEPCOIN 		= CONSENSUS_PREFIX+"UNKEEPCOIN";
	
	public static final String CONSENSUS_CHECK 		        = CONSENSUS_PREFIX+"CHECK";
	
	public static final String CONSENSUS_FLUSHMEMPOOL 		= CONSENSUS_PREFIX+"FLUSHMEMPOOL";
	public static final String CONSENSUS_MEMPOOL 			= CONSENSUS_PREFIX+"MEMPOOL";
	
	public static final String CONSENSUS_EXPORTKEY 			= CONSENSUS_PREFIX+"EXPORTKEY";
	public static final String CONSENSUS_IMPORTKEY 			= CONSENSUS_PREFIX+"IMPORTKEY";
	public static final String CONSENSUS_EXPORTCOIN 		= CONSENSUS_PREFIX+"EXPORTCOIN";
	public static final String CONSENSUS_IMPORTCOIN 		= CONSENSUS_PREFIX+"IMPORTCOIN";
	
	public static final String CONSENSUS_MMRTREE 		    = CONSENSUS_PREFIX+"MMRTREE";
	
	public static final String CONSENSUS_CONSOLIDATE 		= CONSENSUS_PREFIX+"CONSOLIDATE";
	
	public ConsensusUser(MinimaDB zDB, ConsensusHandler zHandler) {
		super(zDB, zHandler);
	}
	 
	public void processMessage(Message zMessage) throws Exception {
		
		if(zMessage.isMessageType(CONSENSUS_NEWSIMPLE)) {
			int bitlength = GlobalParams.MINIMA_DEFAULT_HASH_STRENGTH;
			int keys   = 16;
			int levels = 2;
			if(zMessage.exists("bitlength")) {
				bitlength = zMessage.getInteger("bitlength");
				keys = zMessage.getInteger("keys");
				levels = zMessage.getInteger("levels");
			}
			
			//Create a new key..
			MultiKey mkey = new MultiKey(bitlength, new MiniNumber(keys), new MiniNumber(levels));
			
			//Create a new simple address
			Address addr = getMainDB().getUserDB().newSimpleAddress(mkey);
			
			JSONObject resp = InputHandler.getResponseJSON(zMessage);
			resp.put("address", addr.toJSON());
			InputHandler.endResponse(zMessage, true, "");
		
			//Do a backup..
			getConsensusHandler().PostMessage(ConsensusBackup.CONSENSUSBACKUP_BACKUPUSER);
			
		}else if(zMessage.isMessageType(CONSENSUS_SIGN)) {
			String data   = zMessage.getString("data");
			String pubkey = zMessage.getString("publickey");
			
			//Is it HEX or String..
			String type="";
			MiniData hexdat = null;
			if(data.startsWith("0x")) {
				type = "HEX";
				hexdat  = new MiniData(data);
			}else {
				type = "STRING";
				hexdat  = new MiniData(data.getBytes(MiniString.MINIMA_CHARSET));
			}
			
			//Convert
			MiniData hexpubk = new MiniData(pubkey);
			
			//Get the public key..
			MultiKey key = getMainDB().getUserDB().getPubPrivKey(hexpubk);
			
			if(key == null) {
				InputHandler.endResponse(zMessage, false, "Public key not found");
				return;
			}
			
			//Now do it..
			MiniData result = key.sign(hexdat);
		
			//Output
			JSONObject resp = InputHandler.getResponseJSON(zMessage);
			resp.put("original", data);
			resp.put("type", type);
			resp.put("data", hexdat.to0xString());
			resp.put("publickey", hexpubk.to0xString());
			resp.put("signature", result.to0xString());
			resp.put("length", result.getLength());
			InputHandler.endResponse(zMessage, true, "");
		
		}else if(zMessage.isMessageType(CONSENSUS_VERIFY)) {
			String data   = zMessage.getString("data");
			String pubkey = zMessage.getString("publickey");
			String sig    = zMessage.getString("signature");
			
			//Convert
			MiniData hexdata = new MiniData(data);
			MiniData hexpubk = new MiniData(pubkey);
			MiniData hexsig  = new MiniData(sig);
			
			//Create a MultiKey..
			MultiKey key = new MultiKey(hexpubk);
			
			//Now verify..
			boolean verify = key.verify(hexdata, hexsig);
			
			if(verify) {
				InputHandler.endResponse(zMessage, true, "Valid Signature");
			}else {
				InputHandler.endResponse(zMessage, false, "Invalid Signature");
			}
			
		}else if(zMessage.isMessageType(CONSENSUS_EXTRASCRIPT)) {
			//Get the script
			String script = zMessage.getString("script");
			
			//Check we don't already have it..
			Address addrchk = new Address(script);
			String scriptcheck = getMainDB().getUserDB().getScript(addrchk.getAddressData());
			if(scriptcheck.equals("")) {
				getMainDB().getUserDB().newExtraAddress(script);
			}
			
			JSONObject resp = InputHandler.getResponseJSON(zMessage);
			resp.put("address", addrchk.toJSON());
			InputHandler.endResponse(zMessage, true, "");
		
			//Do a backup..
			getConsensusHandler().PostMessage(ConsensusBackup.CONSENSUSBACKUP_BACKUPUSER);
			
		}else if(zMessage.isMessageType(CONSENSUS_NEWSCRIPT)) {
			//Get the script
			String script = zMessage.getString("script");
			
			//Check we don't already have it..
			Address addrchk = new Address(script);
			String scriptcheck = getMainDB().getUserDB().getScript(addrchk.getAddressData());
			if(scriptcheck.equals("")) {
				getMainDB().getUserDB().newScriptAddress(script);
			}
			
			JSONObject resp = InputHandler.getResponseJSON(zMessage);
			resp.put("address", addrchk.toJSON());
			InputHandler.endResponse(zMessage, true, "");
		
			//Do a backup..
			getConsensusHandler().PostMessage(ConsensusBackup.CONSENSUSBACKUP_BACKUPUSER);
			
		}else if(zMessage.isMessageType(CONSENSUS_NEWKEY)) {
			//Get the bitlength
			int bitl   = zMessage.getInteger("bitlength");
			int keys   = zMessage.getInteger("keys");
			int levels = zMessage.getInteger("levels");
			
			//Create a new key pair..
			MultiKey key = getMainDB().getUserDB().newPublicKey(bitl,keys,levels);
			
			//return to sender!
			JSONObject resp = InputHandler.getResponseJSON(zMessage);
			resp.put("key", key.toJSON());
			InputHandler.endResponse(zMessage, true, "");
			
			//Do a backup..
			getConsensusHandler().PostMessage(ConsensusBackup.CONSENSUSBACKUP_BACKUPUSER);
			
		}else if(zMessage.isMessageType(CONSENSUS_CURRENTADDRESS)) {
			//Get the current address
			Address current = getMainDB().getUserDB().getCurrentAddress(getConsensusHandler());
		
			//get the pubkey..
			MiniData pubkey = getMainDB().getUserDB().getPublicKeyForSimpleAddress(current.getAddressData());
			
			//And now the multikey
			MultiKey key = getMainDB().getUserDB().getPubPrivKey(pubkey);
			
			JSONObject resp = InputHandler.getResponseJSON(zMessage);
			resp.put("current", current.toJSON());
			resp.put("key", key.toJSON());
			
			InputHandler.endResponse(zMessage, true, "");
			
		}else if(zMessage.isMessageType(CONSENSUS_CHECK)) {
			String data = zMessage.getString("data");
			
			//How much to who ?
			MiniData check = null;
			if(data.startsWith("0x")) {
				//It's a regular HASH address
				check  = new MiniData(data);
			}else if(data.startsWith("Mx")) {
				//It's a Minima Address!
				check = Address.convertMinimaAddress(data);
			
			}else {
				InputHandler.endResponse(zMessage, false, "INVALID KEY - "+data);	
				return;
			}
			
			//Now check..
			String type = "none";
			boolean found=false;
			if(getMainDB().getUserDB().isAddressRelevant(check)) {
				found=true;
				type = "address";
			}else if(getMainDB().getUserDB().getPubPrivKey(check)!=null) {
				found=true;
				type = "publickey";
			}
					
			JSONObject resp = InputHandler.getResponseJSON(zMessage);
			resp.put("relevant", found);
			resp.put("type", type);
			InputHandler.endResponse(zMessage, true, "");
			
		}else if(zMessage.isMessageType(CONSENSUS_MMRTREE)) {
			//What type SCRIPT or HASHES
			int bitlength = zMessage.getInteger("bitlength");
			
			//Create an MMR TREE from the array of inputs..
			ArrayList<MiniString> leaves = (ArrayList<MiniString>) zMessage.getObject("leaves");
		
			//First create an MMR Tree..
			MMRSet mmr = new MMRSet(bitlength);
			
			//Now add each 
			JSONArray nodearray = new JSONArray();
			for(MiniString leaf : leaves) {
				String leafstr = leaf.toString();
				JSONObject mmrnode = new JSONObject();
				MiniData finaldata = null;
				
				//What type of data..
				int valtype = Value.getValueType(leafstr);
				if(valtype == HexValue.VALUE_HEX ) {
					finaldata = new MiniData(leafstr);
					mmrnode.put("data",finaldata.toString());
					
				}else if(valtype == BooleanValue.VALUE_BOOLEAN ) {
					MiniNumber num = MiniNumber.ZERO;
					if(leaf.toString().equals("TRUE")) {
						num = MiniNumber.ONE;	
					}
					finaldata = MiniData.getMiniDataVersion(num);
					mmrnode.put("data",num.toString());
					
				}else if(valtype == NumberValue.VALUE_NUMBER) {
					MiniNumber num = new MiniNumber(leaf.toString());
					finaldata = MiniData.getMiniDataVersion(num);
					mmrnode.put("data",num.toString());
					
					
				}else{
					//DEFAULT IS SCRIPT
					finaldata = new MiniData(leaf.getData());
					mmrnode.put("data",leafstr);
				}
				
				
				//Now HASH what we have..
				byte[] hash = Crypto.getInstance().hashData(finaldata.getData(), bitlength);
				MiniData finalhash = new MiniData(hash);
				
				//That hash is the actual leaf node of the tree
				mmrnode.put("leaf", finalhash.to0xString());
				
				//Add to the complete array
				nodearray.add(mmrnode);
				
				//Add to the MMR
				mmr.addUnspentCoin(new MMRData(finalhash,MiniNumber.ZERO));
			}

			//Now finalize..
			mmr.finalizeSet();
			
			//Now add the proofs..
			int size=nodearray.size();
			for(int i=0;i<size;i++) {
				JSONObject node = (JSONObject) nodearray.get(i);
				
				//Get the proof..
				MMRProof proof = mmr.getProof(new MiniNumber(i));
				
				//Calculate the CHAINSHA proof..
				node.put("chainsha", proof.getChainSHAProof().to0xString());
			}
			
			//return to sender!
			JSONObject resp = InputHandler.getResponseJSON(zMessage);
			resp.put("nodes", nodearray);
			resp.put("root", mmr.getMMRRoot().getFinalHash().to0xString());
			InputHandler.endResponse(zMessage, true, "");
			
		}else if(zMessage.isMessageType(CONSENSUS_CLEANSCRIPT)) {
			//Get the Script
			String script = zMessage.getString("script");
			
			//Clean it..
			String clean = Contract.cleanScript(script);
			
			JSONObject resp = InputHandler.getResponseJSON(zMessage);
			resp.put("script", script);
			resp.put("clean", clean);
			InputHandler.endResponse(zMessage, true, "");
		
		}else if(zMessage.isMessageType(CONSENSUS_RUNSCRIPT)) {
			String script    = zMessage.getString("script").trim();
			if(script.equals("")) {
				InputHandler.endResponse(zMessage, false, "Cannot have a blank script!");
				return;
			}
			
			String sigs      = zMessage.getString("sigs").trim();
			String state     = zMessage.getString("state").trim();
			String prevstate = zMessage.getString("prevstate").trim();
			String globals   = zMessage.getString("globals").trim();
			String outputs   = zMessage.getString("outputs").trim();
			String scripts   = zMessage.getString("scripts").trim();
			
			//Create the transaction..
			Transaction trans = new Transaction();
			Witness wit       = new Witness();
			
			//OUTPUTS
			if(!outputs.equals("")) {
				//Add the outputs to the Transaction..
				StringTokenizer strtok = new StringTokenizer(outputs,"#");
				while(strtok.hasMoreElements()){
					String tok = strtok.nextToken().trim();
					
					//Now split this token..
					if(!tok.equals("")) {
						//Address
						int index = tok.indexOf(":");
						String address = tok.substring(0,index).trim();
						
						//Amount
						int oldindex = index;
						index = tok.indexOf(":", index+1);
						String amount = tok.substring(oldindex+1,index).trim();
						MiniNumber amt = new MiniNumber(amount);
						
						//Tokenid
						String tokenid = tok.substring(index+1).trim();
						
						//Add the details to the witness..
						TokenProof tprf = getMainDB().getUserDB().getTokenDetail(new MiniData(tokenid));
						if(tprf != null) {
							wit.addTokenDetails(tprf);
						
							//Recalculate the amount.. given the token scale..
							amt = tprf.getScaledMinimaAmount(amt);
//							amt = amt.div(tprf.getScaleFactor());
						}
						
						//Create this coin
						Coin outcoin = new Coin(new MiniData("0x00"), 
												new MiniData(address), 
												amt, 
												new MiniData(tokenid));
						
						//Add this output to the transaction..
						trans.addOutput(outcoin);	
					}
				}
			}
			
			//STATE
			if(!state.equals("")) {
				//Add all the state variables..
				StringTokenizer strtok = new StringTokenizer(state,"#");
				while(strtok.hasMoreElements()){
					String tok = strtok.nextToken().trim();
					
					//Now split this token..
					if(!tok.equals("")) {
						int split = tok.indexOf(":");
						String statenum = tok.substring(0,split).trim();
						String value    = tok.substring(split+1).trim();
						
						//Set it..
						trans.addStateVariable(new StateVariable(Integer.parseInt(statenum), value));
					}
				}
			}
			
			//PREVSTATE
			ArrayList<StateVariable> pstate = new ArrayList<>();
			if(!prevstate.equals("")) {
				//Add all the state variables..
				StringTokenizer strtok = new StringTokenizer(prevstate,"#");
				while(strtok.hasMoreElements()){
					String tok = strtok.nextToken().trim();
					
					//Now split this token..
					if(!tok.equals("")) {
						int split = tok.indexOf(":");
						String statenum = tok.substring(0,split).trim();
						String value = tok.substring(split+1).trim();
						
						//Set it..
						pstate.add(new StateVariable(Integer.parseInt(statenum), value));
					}
				}
			}
			
			//SCRIPTS
			if(!scripts.equals("")) {
				//Add all the state variables..
				StringTokenizer strtok = new StringTokenizer(scripts,"#");
				while(strtok.hasMoreElements()){
					String tok = strtok.nextToken().trim();
					
					//Now split this token..
					if(!tok.equals("")) {
						int split = tok.indexOf(":");
						
						String mastscript = tok.substring(0,split).trim();
						String chainsha   = tok.substring(split+1).trim();
						
						//Set it..
						wit.addScript(new ScriptProof(mastscript, chainsha));
					}
				}
			}
			
			//Create a contract
			Contract cc = new Contract(script, sigs, wit, trans, pstate);
			
			//Create an address
			Address ccaddress = new Address(cc.getMiniScript());
			
			//Set the environment
			TxPoW top = getMainDB().getTopTxPoW();
			MiniNumber blocknum  = top.getBlockNumber();
			MiniNumber blocktime = top.getTimeMilli();
			
			//These 2 are set automatically..
			cc.setGlobalVariable("@ADDRESS", new HexValue(ccaddress.getAddressData()));
			cc.setGlobalVariable("@SCRIPT", new StringValue(script));
			
			//These can be played with..
			cc.setGlobalVariable("@BLKNUM", new NumberValue(blocknum));
			cc.setGlobalVariable("@BLKTIME", new NumberValue(blocktime));
			cc.setGlobalVariable("@INPUT", new NumberValue(0));
			cc.setGlobalVariable("@INBLKNUM", new NumberValue(0));
			cc.setGlobalVariable("@AMOUNT", new NumberValue(0));
			cc.setGlobalVariable("@COINID", new HexValue("0x00"));
			cc.setGlobalVariable("@TOTIN", new NumberValue(1));
			cc.setGlobalVariable("@TOTOUT", new NumberValue(trans.getAllOutputs().size()));
			
			cc.setGlobalVariable("@TOKENID", new HexValue("0x00"));
			cc.setGlobalVariable("@TOKENSCRIPT", new StringValue(""));
			cc.setGlobalVariable("@TOKENTOTAL", new NumberValue(MiniNumber.BILLION));
			
			
			//#TODO
			//previous block hash.. FOR NOW.. Should be set in Script IDE 
			MiniData prevblkhash = MiniData.getRandomData(64);
			MiniData prng        = MiniData.getRandomData(64);
			
			cc.setGlobalVariable("@PREVBLKHASH", new HexValue(prevblkhash));
			cc.setGlobalVariable("@PRNG", new HexValue(prng));
			
			//GLOBALS.. Overide if set..
			if(!globals.equals("")) {
				//Add all the state variables..
				StringTokenizer strtok = new StringTokenizer(globals,"#");
				while(strtok.hasMoreElements()){
					String tok = strtok.nextToken().trim();
					
					//Now split this token..
					if(!tok.equals("")) {
						int split = tok.indexOf(":");
						String global = tok.substring(0,split).trim().toUpperCase();
						String value = tok.substring(split+1).trim();
						
						if(global.equals("@TOKENID")) {
							//Add the details to the witness..
							TokenProof tprf = getMainDB().getUserDB().getTokenDetail(new MiniData(value));
							if(tprf != null) {
								wit.addTokenDetails(tprf);
							}	
						}
						
						//Set it..
						cc.setGlobalVariable(global, Value.getValue(value));
					}
				}
			}
			
			//Set the BLKDIFF
			MiniNumber blk   = ((NumberValue)cc.getGlobal("@BLKNUM")).getNumber();
			MiniNumber blkin = ((NumberValue)cc.getGlobal("@INBLKNUM")).getNumber();
			cc.setGlobalVariable("@BLKDIFF", new NumberValue(blk.sub(blkin)));
			
			//Run it!
			cc.run();
		
			//Detailed results..
			JSONObject resp = InputHandler.getResponseJSON(zMessage);
			resp.put("script", script);
			resp.put("clean", cc.getMiniScript());
			resp.put("size", cc.getMiniScript().length());
			resp.put("instructions", cc.getNumberOfInstructions());
			resp.put("address", ccaddress.getAddressData().to0xString());
			resp.put("parseok", cc.isParseOK());
			resp.put("variables",cc.getAllVariables());
			resp.put("parse", cc.getCompleteTraceLog());
			resp.put("exception", cc.isException());
			resp.put("excvalue", cc.getException());
			resp.put("result", cc.isSuccess());
			InputHandler.endResponse(zMessage, true, "");
		
		}else if(zMessage.isMessageType(CONSENSUS_FLUSHMEMPOOL)) {
			boolean hard = false;
			if(zMessage.exists("hard")) {
				hard = zMessage.getBoolean("hard");	
			}
			
			//Clear the current Requested Transactions.. this should ask for them all anyway..
			getNetworkHandler().clearAllrequestedTxPow();
			
			//JSON response..
			JSONObject resp = InputHandler.getResponseJSON(zMessage);
			resp.put("hard", hard);
			
			//TxPOW DB
			TxPowDB tdb = getMainDB().getTxPowDB();
			
			//Check the MEMPOOL transactions..
			ArrayList<TxPOWDBRow> unused = tdb.getAllUnusedTxPOW();
			int tested = unused.size();
			ArrayList<MiniData> remove = new ArrayList<>();
			JSONArray found     = new JSONArray();
			JSONArray requested = new JSONArray();
		
			ArrayList uniqueRequest = new ArrayList<>();
			
			//Check them all..
			for(TxPOWDBRow txrow : unused) {
				TxPoW txpow    = txrow.getTxPOW();
				found.add(txpow.getTxPowID().to0xString());
				
				//Do we just remove them all.. ?
				if(hard) {
					//Remove all..
					remove.add(txpow.getTxPowID());
				}else{
					//Check All..
					if(txpow.isBlock()) {
						MiniData parent = txpow.getParentID();
						if(tdb.findTxPOWDBRow(parent) == null) {
							//Send a broadcast request
							getConsensusHandler().getConsensusNet().sendTxPowRequest(parent);

							//Add to out list
							requested.add(parent.to0xString());
						}
						
						//Get all the messages in the block..
						ArrayList<MiniData> txns = txpow.getBlockTransactions();
						for(MiniData txn : txns) {
							if(tdb.findTxPOWDBRow(txn) == null) {
								//Send a broadcast request
								getConsensusHandler().getConsensusNet().sendTxPowRequest(txn);
								
								//Add to out list
								requested.add(txn.to0xString());
							}
						}
					}		
				}
			}
			
			//Now remove these..
			JSONArray rem = new JSONArray();
			for(MiniData remtxp : remove) {
				rem.add(remtxp.to0xString());
				getMainDB().getTxPowDB().removeTxPOW(remtxp);
			}
			
			//Now you have the proof..
			resp.put("number", tested);
			resp.put("found", found);
			resp.put("removed", rem);
			resp.put("requested", requested);
			InputHandler.endResponse(zMessage, true, "Mempool Flushed");
			
		}else if(zMessage.isMessageType(CONSENSUS_MEMPOOL)) {
			//JSON response..
			JSONObject resp = InputHandler.getResponseJSON(zMessage);
			
			//TxPOW DB
			TxPowDB tdb = getMainDB().getTxPowDB();
			
			//Check the MEMPOOL transactions..
			ArrayList<TxPOWDBRow> unused = tdb.getAllUnusedTxPOW();
			int tested = unused.size();
			
			JSONArray alltrans = new JSONArray();
			int blocks 	= 0;
			int txns 	= 0;
			
			//Check them all..
			for(TxPOWDBRow txrow : unused) {
				TxPoW txpow    = txrow.getTxPOW();
				
				if(txpow.isBlock()) {
					blocks++;
				}
				
				if(txpow.isTransaction()) {
					txns++;
					
					JSONObject trx = new JSONObject();
					trx.put("txpowid", txpow.getTxPowID().to0xString());
					trx.put("relevant", txrow.getLatestRelevantBlockTime());
					
					alltrans.add(trx);
				}
			}
			
			//Now you have the proof..
			resp.put("allmempool", tested);
			resp.put("lastrelevant", getMainDB().getMainTree().getCascadeNode().getBlockNumber().sub(MiniNumber.SIXTYFOUR));
			resp.put("alltransactions", alltrans);
			resp.put("transactions", txns);
			resp.put("blocks", blocks);
			InputHandler.endResponse(zMessage, true, "Mempool Details");
		
		}else if(zMessage.isMessageType(CONSENSUS_UNKEEPCOIN)) {
//			//Once a coin has been used - say in a DEX.. you can remove it from your coinDB
//			String cid = zMessage.getString("coinid");
//			
//			//Remove from the UserDB
//			getMainDB().getUserDB().removeRelevantCoinID(new MiniData(cid));
//			
//			//Get the MMRSet
//			MMRSet basemmr = getMainDB().getMMRTip();
//			
//			//Search for the coin..
//			MiniData coinid = new MiniData(cid);
//			MMREntry entry =  basemmr.findEntry(coinid);
//			
//			//Now ask to keep it..
//			MMRSet coinset = basemmr.getParentAtTime(entry.getBlockTime());
//			boolean found = coinset.removeKeeper(entry.getEntryNumber());
//			
//			//Now you have the proof..
//			JSONObject resp = InputHandler.getResponseJSON(zMessage);
//			resp.put("found", found);
//			resp.put("coinid", cid);
//			InputHandler.endResponse(zMessage, true, "Coin removed");
			
		}else if(zMessage.isMessageType(CONSENSUS_KEEPCOIN)) {
			String cid = zMessage.getString("coinid");
			
			JSONObject resp = InputHandler.getResponseJSON(zMessage);
			resp.put("coinid", cid);
			
			//Get the MMRSet
			MMRSet basemmr = getMainDB().getMMRTip();
			
			//Search for the coin..
			MiniData coinid = new MiniData(cid);
			MMREntry entry =  basemmr.findEntry(coinid);
			
			//If NULL not found..
			if(entry == null) {
				InputHandler.endResponse(zMessage, false, "CoinID not found");
				return;
			}
			
			//Add it to the Database
			getMainDB().getUserDB().addRelevantCoinID(coinid);
			
			//Now ask to keep it..
			MMRSet coinset = basemmr.getParentAtTime(entry.getBlockTime());
			coinset.addKeeper(entry.getEntryNumber());
			
//			//Get the coin
//			Coin cc = entry.getData().getCoin();
//			
//			//add it to the database
//			CoinDBRow crow = getMainDB().getCoinDB().addCoinRow(cc);
//			crow.setRelevant(true);
//			crow.setKeeper(true);
//			crow.setIsSpent(entry.getData().isSpent());
//			crow.setIsInBlock(true);
//			crow.setInBlockNumber(entry.getData().getInBlock());
//			crow.setMMREntry(entry.getEntryNumber());
			
			//Now you have the proof..
			resp.put("coin", basemmr.getProof(entry.getEntryNumber()));
			InputHandler.endResponse(zMessage, true, "");
			
			//Do a backup..
			getConsensusHandler().PostMessage(ConsensusBackup.CONSENSUSBACKUP_BACKUP);
			
		}else if(zMessage.isMessageType(CONSENSUS_IMPORTCOIN)) {
//			MiniData data = (MiniData)zMessage.getObject("proof");
//			
//			ByteArrayInputStream bais = new ByteArrayInputStream(data.getData());
//			DataInputStream dis = new DataInputStream(bais);
//			
//			//Now make the proof..
//			MMRProof proof = MMRProof.ReadFromStream(dis);
//			
//			if(proof.getMMRData().isSpent()) {
//				//ONLY UNSPENT COINS..
//				InputHandler.endResponse(zMessage, false, "Coin already SPENT!");
//				return;
//			}
//			
//			//Get the MMRSet
//			MMRSet basemmr = getMainDB().getMainTree().getChainTip().getMMRSet();
//			
//			//Check it..
//			boolean valid  = basemmr.checkProof(proof);
//			
//			//Stop if invalid.. 
//			if(!valid) {
//				//Now you have the proof..
//				InputHandler.endResponse(zMessage, false, "INVALID PROOF");
//				return;
//			}
//			
//			//Get the MMRSet where this proof was made..
//			MMRSet proofmmr = basemmr.getParentAtTime(proof.getBlockTime());
//			if(proofmmr == null) {
//				//Now you have the proof..
//				InputHandler.endResponse(zMessage, false, "Proof too old - no MMRSet found @ "+proof.getBlockTime());
//				return;
//			}
//			
//			//Now add this proof to the set.. if not already added
//			MMREntry entry =  proofmmr.addExternalUnspentCoin(proof);
//			
//			//Error.
//			if(entry == null) {
//				InputHandler.endResponse(zMessage, false, "Consensus error addding proof !");
//				return;
//			}
//			
//			//And now refinalize..
//			proofmmr.finalizeSet();
//			
//			//Get the coin
//			Coin cc = entry.getData().getCoin();
//			
//			//Is it relevant..
//			boolean rel = false;
//			if( getMainDB().getUserDB().isAddressRelevant(cc.getAddress()) ){
//				rel = true;
//			}
//			
//			//add it to the database
//			CoinDBRow crow = getMainDB().getCoinDB().addCoinRow(cc);
//			crow.setRelevant(rel);
//			crow.setKeeper(true);
//			crow.setIsSpent(entry.getData().isSpent());
//			crow.setIsInBlock(true);
//			crow.setInBlockNumber(entry.getData().getInBlock());
//			crow.setMMREntry(entry.getEntryNumber());
//			
//			//Now you have the proof..
//			JSONObject resp = InputHandler.getResponseJSON(zMessage);
//			resp.put("proof", proof.toJSON());
//			InputHandler.endResponse(zMessage, true, "");
//			
//			//Do a backup..
//			getConsensusHandler().PostMessage(ConsensusBackup.CONSENSUSBACKUP_BACKUP);
			
		}else if(zMessage.isMessageType(CONSENSUS_EXPORTCOIN)) {
			MiniData coinid = (MiniData)zMessage.getObject("coinid");
			
			//The Base current MMRSet
			MMRSet basemmr  = getMainDB().getMainTree().getChainTip().getMMRSet();
			
			//Get proofs from a while back so reorgs don't invalidate them..
			MMRSet proofmmr = basemmr.getParentAtTime(getMainDB().getTopBlock().sub(GlobalParams.MINIMA_CONFIRM_DEPTH));
			
			//Find this coin..
			MMREntry coin  = proofmmr.findEntry(coinid);
//			CoinDBRow row  = getMainDB().getCoinDB().getCoinRow(coinid);
			
			//Get a proof from a while back.. more than confirmed depth, less than cascade
//			MMRProof proof = getMainTree().getChainTip().getMMRSet().getProof(row.getMMREntry());
			MMRProof proof = proofmmr.getProof(coin.getEntryNumber());
			
			//Now write this out to  MiniData Block
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);
			proof.writeDataStream(dos);
			dos.flush();
			
			//Now get the data..
			MiniData pd = new MiniData(baos.toByteArray());
			
			//Now you have the proof..
			JSONObject resp = InputHandler.getResponseJSON(zMessage);
			resp.put("coinid", coinid.to0xString());
			resp.put("proof", proof.toJSON());
			resp.put("data", pd.to0xString());
			InputHandler.endResponse(zMessage, true, "");
			
			dos.close();
			baos.close();
			
		}else if(zMessage.isMessageType(CONSENSUS_EXPORTKEY)) {
			MiniData pubk = (MiniData)zMessage.getObject("publickey");
			
			//Get it..
			MiniData priv = getMainDB().getUserDB().getPubPrivKey(pubk).getPrivateSeed();
			
			MinimaLogger.log(priv.toString());
			
		}else if(zMessage.isMessageType(CONSENSUS_IMPORTKEY)) {
			MiniData priv = (MiniData)zMessage.getObject("privatekey");

			MultiKey newkey = new MultiKey(priv, MultiKey.DEFAULT_KEYS_PER_LEVEL, MultiKey.DEFAULT_LEVELS);
//			PubPrivKey newkey = new PubPrivKey(priv);
			
			if(getMainDB().getUserDB().getPubPrivKey(newkey.getPublicKey())!=null) {
				MinimaLogger.log("Key allready in DB!");
			}else {
				getMainDB().getUserDB().newSimpleAddress(newkey);
			}
			
			//Do a backup..
			getConsensusHandler().PostMessage(ConsensusBackup.CONSENSUSBACKUP_BACKUPUSER);
		
			
		}else if(zMessage.isMessageType(CONSENSUS_CONSOLIDATE)) {
			//Is there a parameter ?
			boolean infoonly = false;
			if(zMessage.exists("param")) {
				String param 	= zMessage.getString("param");
				UserPrefs prefs	= Main.getMainHandler().getUserPrefs();
				
				if(param.equals("on")) {
					prefs.setBoolean("consolidate", true);
					InputHandler.getResponseJSON(zMessage).put("auto", true);
					InputHandler.endResponse(zMessage, true, "AUTO Coin Consolidation turned ON");
					return;
				}else if(param.equals("off")) {
					prefs.setBoolean("consolidate", false);
					InputHandler.getResponseJSON(zMessage).put("auto", false);
					InputHandler.endResponse(zMessage, true, "AUTO Coin Consolidation turned OFF");
					return;
				}else if(param.equals("info")) {
					boolean auto = prefs.getBoolean("consolidate", false);
					InputHandler.getResponseJSON(zMessage).put("auto", auto);
					infoonly = true;
				}else {
					InputHandler.endResponse(zMessage, false, "Unknown parameter : "+param);
					return;
				}
			}
			
			//Is this a manual Consolidation
			boolean manual = false;
			if(zMessage.exists("manual")) {
				manual = true;
			}
			
			//List of tokens..
			ArrayList<String> alltokens = new ArrayList<>();
			
			//Get a list of all the tokens this user has..
			ArrayList<MMREntry> relevant = getMainDB().getMMRTip().searchAllRelevantCoins();
			for(MMREntry relcoin : relevant) {
				MMRData coindata = relcoin.getData();
				Coin coin 		 = coindata.getCoin();
				String token 	 = coin.getTokenID().to0xString();
				
				if(!alltokens.contains(token)) {
					alltokens.add(token);
				}
			}
			
			//Now cycle through and consolidate each token..
			JSONArray coininfo = new JSONArray();
			for(String tok : alltokens) {
				//Token..
				MiniData tokenid = new MiniData(tok);
				if(infoonly) {
					//Work out the details only..
					consolidateTokenInfo(tokenid, coininfo);
				}else {
					JSONArray coinret = null;
					if(manual) {
						coinret = consolidateToken(tokenid,1);
					}else {
						coinret = consolidateToken(tokenid,3);
					}
					
					//Add to coininfo
					coininfo.add(coinret);
				}
				
				//Uses memory up.. clean it..
				System.gc();
			}
			
			//All done..
			InputHandler.getResponseJSON(zMessage).put("coins", coininfo);
			if(infoonly) {
				InputHandler.endResponse(zMessage, true, "Coins Consolidation Info");
			}else {
				InputHandler.endResponse(zMessage, true, "Coins Consolidated");
			}
		}
	}
	
	private void consolidateTokenInfo(MiniData zTokenID, JSONArray zCoinInfo) throws Exception {
		//A list of coins per pub key
		Hashtable<String, ArrayList<Coin>> pubcoins = new Hashtable<>();
		
		//First get a list of coins..
		ArrayList<Coin> coins = getMainDB().getTotalSimpleSpendableCoins(zTokenID);
		for(Coin coin : coins) {
			
			//Get the Public Key..
			MiniData pubk 	= getMainDB().getUserDB().getPublicKeyForSimpleAddress(coin.getAddress());
			String pk 		= pubk.to0xString();
			
			//Get the current array
			ArrayList<Coin> curr = pubcoins.get(pk);
			if(curr == null) {
				curr = new ArrayList<Coin>();
				pubcoins.put(pk, curr);
			}
		
			//Now add this coin..
			curr.add(coin);
		}
		
		//Now create transactions..
		JSONArray consarray = new JSONArray();
		Set<String> keys = pubcoins.keySet();
		for(String key : keys) {
			ArrayList<Coin> allcoins = pubcoins.get(key);
			int coinsize = allcoins.size();
			
			//Enough coins to consolidate
			JSONObject ccoin = new JSONObject();
			ccoin.put("key", key);
			ccoin.put("number", coinsize);
			consarray.add(ccoin);
		}	
		
		//Add it
		JSONObject cointok = new JSONObject();
		cointok.put("tokenid", zTokenID.to0xString());
		cointok.put("coins", consarray);
		zCoinInfo.add(cointok);
		
		return;
	}
	
	
	private JSONArray consolidateToken(MiniData zTokenID, int zTrigger) throws Exception {
		//The consolidated coins
		JSONArray cons = new JSONArray();
		
		//A list of coins per pub key
		Hashtable<String, ArrayList<Coin>> pubcoins = new Hashtable<>();
		
		//First get a list of coins..
		ArrayList<Coin> coins = getMainDB().getTotalSimpleSpendableCoins(zTokenID);
		for(Coin coin : coins) {
			
			//Get the Public Key..
			MiniData pubk 	= getMainDB().getUserDB().getPublicKeyForSimpleAddress(coin.getAddress());
			String pk 		= pubk.to0xString();
			
			//Get the current array
			ArrayList<Coin> curr = pubcoins.get(pk);
			if(curr == null) {
				curr = new ArrayList<Coin>();
				pubcoins.put(pk, curr);
			}
		
			//Now add this coin..
			curr.add(coin);
		}
	
		int MAX_COLL = 5;
		int TRIGGER  = zTrigger;
		
		//Now create transactions..
		Set<String> keys = pubcoins.keySet();
		for(String key : keys) {
			ArrayList<Coin> allcoins = pubcoins.get(key);
			int coinsize = allcoins.size();
			
			//Are there more than 1..
			if(coinsize>TRIGGER) {
				MiniNumber totalval = MiniNumber.ZERO;
				
				//Now create a transaction
				Transaction trans = new Transaction();
				Witness wit 	  = new Witness();
				
				//Cycle through the inputs and get the total..
				int tot = 0;
				ArrayList<Coin> usecoins = new ArrayList<>();
				for(Coin incoin : allcoins) {
					//Use this coin
					usecoins.add(incoin);
					
					//Total value of the inputs
					totalval = totalval.add(incoin.getAmount());
				
					tot++;
					if(tot>=MAX_COLL) {
						break;
					}
				}
				
				//Add the token proofs..
				MiniNumber showamount = totalval;
				if(!zTokenID.isEqual(Coin.MINIMA_TOKENID)) {
					//Get the token proof..
					TokenProof tokendets = getMainDB().getUserDB().getTokenDetail(zTokenID);
					
					//Add to the witness..
					wit.addTokenDetails(tokendets);
					
					//How much is it..
					showamount = tokendets.getScaledTokenAmount(totalval);
				}
				
				//Create a transaction..
				MinimaLogger.log("Consolidate "+usecoins.size()+"/"+allcoins.size()+" "+zTokenID.to0xString()
						+" with pubkey "+key+" total value :"+showamount);
		
				//Add to response..
				JSONObject ccoin = new JSONObject();
				ccoin.put("token", zTokenID.to0xString());
				ccoin.put("publickey", key);
				ccoin.put("allcoins", allcoins.size());
				ccoin.put("coins", usecoins.size());
				ccoin.put("value", showamount.toString());
				cons.add(ccoin);
				
				//Send back to me..
				Address recipient = getMainDB().getUserDB().getCurrentAddress(getConsensusHandler());
				
				//Create Transaction
				for(Coin incoin : usecoins) {
					//Add it
					trans.addInput(incoin);
					
					//Get the Script associated with this coin
					String script = getMainDB().getUserDB().getScript(incoin.getAddress());
					
					//Add to the witness
					wit.addScript(script, incoin.getAddress().getLength()*8);
				}
				
				//Add one Output..
				trans.addOutput(new Coin(Coin.COINID_OUTPUT, recipient.getAddressData(), totalval, zTokenID, false, false));
				
				//Create the correct MMR Proofs
				Witness newwit = getMainDB().createValidMMRPRoofs(trans, wit);
				
				//Now sign it..
				MiniData publick = new MiniData(key);
				MultiKey pubkkey = getMainDB().getUserDB().getPubPrivKey(publick);
				
				//Hash of the transaction
				MiniData transhash = Crypto.getInstance().hashObject(trans);
				
				//Sign it
				MiniData signature = pubkkey.sign(transhash);
				
				//Now set the SIG.. 
				wit.addSignature(publick, signature);
				
				//Post it..
				Message msg = new Message(ConsensusHandler.CONSENSUS_SENDTRANS)
									.addObject("transaction", trans)
									.addObject("witness", newwit);
				
				//Add all the inputs to the mining..
				getMainDB().addMiningTransaction(trans);
				
				//Notify listeners that Mining is starting...
				getConsensusHandler().PostDAPPStartMining(trans);
				
				//Post it..
				getConsensusHandler().PostMessage(msg);
		
			}else {
				//MinimaLogger.log("Not enough "+zTokenID.to0xString()+" coins @ "+key+" only "+allcoins.size()+" coins..");
			}
		}
		
		return cons;
	}
	
	
//	public static boolean importCoin(MinimaDB zDB, MMRProof zProof) throws IOException{
//		//Get the MMRSet
//		MMRSet basemmr = zDB.getMainTree().getChainTip().getMMRSet();
//		
//		//Check it..
//		boolean valid  = basemmr.checkProof(zProof);
//		
//		//Stop if invalid.. 
//		if(!valid) {
//			return false;
//		}
//		
//		//Get the MMRSet where this proof was made..
//		MMRSet proofmmr = basemmr.getParentAtTime(zProof.getBlockTime());
//		if(proofmmr == null) {
//			return false;
//		}
//		
//		//Now add this proof to the set.. if not already added
//		MMREntry entry =  proofmmr.addExternalUnspentCoin(zProof);
//		
//		//Error..
//		if(entry == null) {
//			return false;
//		}
//		
//		//And now refinalize..
//		proofmmr.finalizeSet();
//		
//		//Get the coin
//		Coin cc = entry.getData().getCoin();
//		
//		//Is it relevant..
//		boolean rel = false;
//		if( zDB.getUserDB().isAddressRelevant(cc.getAddress()) ){
//			rel = true;
//		}
//		
//		//add it to the database
//		CoinDBRow crow = zDB.getCoinDB().addCoinRow(cc);
//		crow.setKeeper(true);
//		crow.setRelevant(rel);
//		crow.setIsSpent(entry.getData().isSpent());
//		crow.setIsInBlock(true);
//		crow.setInBlockNumber(entry.getData().getInBlock());
//		crow.setMMREntry(entry.getEntryNumber());
//		
//		return true;
//	}
	
//	public static MiniData exportCoin(MinimaDB zDB, MiniData zCoinID) throws IOException {
//		//The Base current MMRSet
//		MMRSet basemmr  = zDB.getMainTree().getChainTip().getMMRSet();
//		
//		//Get proofs from a while back so reorgs don't invalidate them..
//		MMRSet proofmmr = basemmr.getParentAtTime(zDB.getTopBlock().sub(GlobalParams.MINIMA_CONFIRM_DEPTH));
//		
//		//Find this coin..
//		CoinDBRow row  = zDB.getCoinDB().getCoinRow(zCoinID);
//		
//		//Get a proof from a while back.. more than confirmed depth, less than cascade
//		MMRProof proof = proofmmr.getProof(row.getMMREntry());
//		
//		//Now write this out to  MiniData Block
//		ByteArrayOutputStream baos = new ByteArrayOutputStream();
//		DataOutputStream dos = new DataOutputStream(baos);
//		proof.writeDataStream(dos);
//		dos.flush();
//		
//		return new MiniData(baos.toByteArray());
//	}
}
