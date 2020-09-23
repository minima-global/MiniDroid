package org.minima.database.txpowdb.java;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import org.minima.GlobalParams;
import org.minima.database.txpowdb.TxPOWDBRow;
import org.minima.database.txpowdb.TxPowDB;
import org.minima.objects.TxPoW;
import org.minima.objects.base.MiniData;
import org.minima.objects.base.MiniNumber;
import org.minima.utils.MinimaLogger;

public class FastJavaDB implements TxPowDB {

	private Hashtable<String,JavaDBRow> mTxPoWRows;
	
	//The Children of a Parent..
	private Hashtable<String,ArrayList<TxPOWDBRow>> mChildrenOfParents;
	
	public FastJavaDB() {
		mTxPoWRows         = new Hashtable<>();
		mChildrenOfParents = new Hashtable<>();
	}
	
	@Override
	public TxPOWDBRow findTxPOWDBRow(MiniData zTxPOWID) {
		return mTxPoWRows.get(zTxPOWID.to0xString());
	}
	
	@Override
	public TxPOWDBRow addTxPOWDBRow(TxPoW zTxPOW) {
		String search = zTxPOW.getTxPowID().to0xString();
		
		//Is it already in there
		JavaDBRow row = mTxPoWRows.get(search);
		if(row != null) {
			return row;
		}
		
		//Create it
		row = new JavaDBRow(zTxPOW);
				
		//Add it..
		mTxPoWRows.put(search, row);
		
		//Add it to the Children List..
		if(zTxPOW.isBlock()) {
			//Get the Parent...
			String parentid = zTxPOW.getParentID().to0xString();
			
			//Get the ArrayList..
			ArrayList<TxPOWDBRow> children = mChildrenOfParents.get(parentid);
			
			//Has it begun.. ?
			if(children == null) {
				children = new ArrayList<>();
				
				//Add it to the HashTable..
				mChildrenOfParents.put(parentid, children);
			}
			
			//And Add this Row to it..
			children.add(row);
		}
		
		return row;
	}

	@Override
	public ArrayList<TxPOWDBRow> getAllTxPOWDBRow() {
		ArrayList<TxPOWDBRow> copy = new ArrayList<>();
		Enumeration<JavaDBRow> allrows = mTxPoWRows.elements();
		while(allrows.hasMoreElements()) {
			JavaDBRow row = allrows.nextElement();
			copy.add(row);
		}
		return copy;
	}

	@Override
	public void resetAllInBlocks() {
		Enumeration<JavaDBRow> allrows = mTxPoWRows.elements();
		while(allrows.hasMoreElements()) {
			JavaDBRow row = allrows.nextElement();
			row.setIsInBlock(false);
			row.setMainChainBlock(false);
		}
	}

	@Override
	public void resetBlocksFromOnwards(MiniNumber zFromBlock) {
		Enumeration<JavaDBRow> allrows = mTxPoWRows.elements();
		while(allrows.hasMoreElements()) {
			JavaDBRow row = allrows.nextElement();
			if(row.isInBlock() && row.getInBlockNumber().isMoreEqual(zFromBlock)) {
				row.setIsInBlock(false);
				row.setMainChainBlock(false);
			}
		}
	}

	@Override
	public void removeTxPOW(MiniData zTxPOWID) {
		String txpid = zTxPOWID.to0xString();
		
		//Remove from the main List
		mTxPoWRows.remove(txpid);
		
		//And the children..
		mChildrenOfParents.remove(txpid);
	}

	@Override
	public ArrayList<TxPOWDBRow> removeTxPOWInBlockLessThan(MiniNumber zCascade) {
		Hashtable<String,JavaDBRow> newtable = new Hashtable<>();
		ArrayList<TxPOWDBRow> removed = new ArrayList<>();
		
		//The minimum block before its too late for a USED TxPoW
		MiniNumber minused = zCascade.sub(MiniNumber.SIXTYFOUR);
		
		//The minimum block before its too late for an UNUSED TxPoW
		MiniNumber minunused = zCascade.add(MiniNumber.TWOFIVESIX);
				
		Enumeration<JavaDBRow> allrows = mTxPoWRows.elements();
		while(allrows.hasMoreElements()) {
			JavaDBRow row  = allrows.nextElement();
			TxPoW rowtxpow = row.getTxPOW();
			
			String txpid = rowtxpow.getTxPowID().to0xString();
			
				//It's a main block
			if(row.isMainChainBlock()) {
				newtable.put(txpid,row);
				
				//It's a transaction on the main chain
			}else if(row.isInBlock() && row.getInBlockNumber().isMoreEqual(minused)) {
				newtable.put(txpid,row);
			
				//It's a transaction but not that old
			}else if(rowtxpow.isTransaction() && !row.isInBlock() && row.getTxPOW().getBlockNumber().isMoreEqual(minunused)) {
				newtable.put(txpid,row);
			
				//It's a block but not past the cascade
			}else if(rowtxpow.isBlock() && !row.isMainChainBlock() && row.getTxPOW().getBlockNumber().isMoreEqual(minused)) {
				newtable.put(txpid,row);
				
			}else {
				if(GlobalParams.SHORT_CHAIN_DEBUG_MODE) {
					if(row.getTxPOW().isTransaction() && !row.isInBlock()) {
						MinimaLogger.log("Transaction NOT in block NOT removed.. "+row);
						
						//Add it anyway..
						newtable.put(txpid,row);
					}else {
						//Remove it..
						removed.add(row);
						mChildrenOfParents.remove(txpid);
					}
				}else{
					if(row.getTxPOW().isTransaction() && !row.isInBlock()) {
						MinimaLogger.log("Transaction NOT in block removed.. "+row);
					}
					
					//Remove it..
					removed.add(row);
					mChildrenOfParents.remove(txpid);
				}
			}		
		}
		
		//Switch to the new table..
		mTxPoWRows = newtable;
		
		return removed;
	}

	@Override
	public ArrayList<TxPOWDBRow> getAllUnusedTxPOW() {
		ArrayList<TxPOWDBRow> ret = new ArrayList<>();
		Enumeration<JavaDBRow> allrows = mTxPoWRows.elements();
		while(allrows.hasMoreElements()) {
			JavaDBRow row = allrows.nextElement();	
			if(!row.isInBlock()) {
				ret.add(row);
			}
		}
		return ret;
	}

	@Override
	public ArrayList<TxPOWDBRow> getChildBlocksTxPOW(MiniData zParent) {
		//FAST
		String parentid = zParent.to0xString();
		
		ArrayList<TxPOWDBRow> ret = mChildrenOfParents.get(parentid);
		if(ret == null) {
			ret = new ArrayList<>();
		}
		
		return ret;
		
		//SLOW
//		ArrayList<TxPOWDBRow> ret = new ArrayList<>();
//		Enumeration<JavaDBRow> allrows = mTxPoWRows.elements();
//		while(allrows.hasMoreElements()) {
//			JavaDBRow row = allrows.nextElement();
//			if(row.getTxPOW().isBlock() && row.getTxPOW().getParentID().isEqual(zParent)) {
//				ret.add(row);
//			}
//		}
//		
//		return ret;
	}

	@Override
	public ArrayList<TxPOWDBRow> getAllBlocksMissingTransactions() {
		ArrayList<TxPOWDBRow> ret = new ArrayList<>();
		Enumeration<JavaDBRow> allrows = mTxPoWRows.elements();
		while(allrows.hasMoreElements()) {
			JavaDBRow row = allrows.nextElement();
			if(row.getTxPOW().isBlock() && row.getBlockState() == TxPOWDBRow.TXPOWDBROW_STATE_BASIC) {
				ret.add(row);
			}
		}
		
		return ret;
	}
	
	@Override
	public int getSize() {
		return mTxPoWRows.size();
	}

	@Override
	public void ClearDB() {
		mTxPoWRows.clear();
	}
}
