package org.minima.database.mmr;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.minima.objects.base.MiniData;
import org.minima.objects.base.MiniNumber;
import org.minima.utils.MinimaLogger;
import org.minima.utils.Streamable;
import org.minima.utils.json.JSONObject;

public class MMREntry implements Streamable {

	/**
	 * Global MMR position
	 */
	
	MiniNumber mEntryNumber;
	int mRow;
	
	/**
	 * The blocktime..
	 */
	MiniNumber mBlockTime = new MiniNumber(0);
	
	/**
	 * The data stored here
	 */
	MMRData    mData;
	
	/**
	 * Valid entry
	 */
	boolean mIsEmpty;
	
	/**
	 * Default constructor
	 * 
	 * @param zRow
	 * @param zEntry
	 */
	public MMREntry(int zRow, MiniNumber zEntry) {
		mRow = zRow;
		mEntryNumber = zEntry;
		mIsEmpty = true;
	}
	
	public boolean isEmpty() {
		return mIsEmpty;
	}
	
	public boolean checkPosition(int zRow, MiniNumber zEntry) {
		return (zRow == mRow) && zEntry.isEqual(mEntryNumber);
	}
	
	public boolean checkPosition(MMREntry zEntry) {
		return (zEntry.getRow() == mRow) && zEntry.getEntryNumber().isEqual(mEntryNumber);
	}
	
	public void setData(MMRData zData) {
		mData    = zData;
		mIsEmpty = false;
	}
	
	public void clearData() {
		mIsEmpty = true;
		mData    = null;
	}
	
	public MMRData getData() {
		return mData;
	}
	
	public void setBlockTime(MiniNumber zBlockTime) {
		mBlockTime = zBlockTime;
	}
	
	public MiniNumber getBlockTime() {
		return mBlockTime;
	}
	
	public MiniData getHashValue() {
		if(isEmpty()) {
			MinimaLogger.log("ERROR NULL Entry : "+this);
		}
		return mData.getFinalHash();
	}
	
	public JSONObject toJSON() {
		JSONObject ret = new JSONObject();
		
		ret.put("block", mBlockTime.toString());
		ret.put("row", mRow);
		ret.put("entry", mEntryNumber.toString());
		ret.put("data", mData.toJSON());
		
		return ret;
	}
	
	@Override
	public String toString() {
		return "BLKTIME:"+mBlockTime+" R:"+mRow+" E:"+mEntryNumber+" D:"+mData;
	}
	
	/**
	 * 
	 * UTILITY FUNCTIONS FOR NAVIGATING THE MMR
	 * 
	 */
	public MiniNumber getEntryNumber() {
		return mEntryNumber;
	}
	
	public int getRow() {
		return mRow;
	}
	
	public int getParentRow() {
		return mRow+1;
	}
	
	public int getChildRow() {
		return mRow-1;
	}
	
	public boolean isLeft() {
		return mEntryNumber.modulo(MiniNumber.TWO).isEqual(MiniNumber.ZERO);
	}
	
	public boolean isRight() {
		return !isLeft();
	}
	
//	public MiniInteger getLeftSibling() {
//		return mEntryNumber.decrement();
//	}
//	
//	public MiniInteger getRightSibling() {
//		return mEntryNumber.increment();
//	}
	
	public MiniNumber getSibling() {
		if(isLeft()) {
			return mEntryNumber.increment();
		}else {
			return mEntryNumber.decrement();
		}
	}
	
	public MiniNumber getParentEntry() {
		return mEntryNumber.div(MiniNumber.TWO).floor();
	}
	
	public MiniNumber getLeftChildEntry() {
		return mEntryNumber.mult(MiniNumber.TWO);
	}
	
	public MiniNumber getRightChildEntry() {
		return getLeftChildEntry().add(MiniNumber.ONE);
	}

	@Override
	public void writeDataStream(DataOutputStream zOut) throws IOException {
		//Entry number
		mEntryNumber.writeDataStream(zOut);
		
		//The Row..
		MiniNumber row = new MiniNumber(mRow);
		row.writeDataStream(zOut);
		
		//And finally the data
		mData.writeDataStream(zOut);
	}

	@Override
	public void readDataStream(DataInputStream zIn) throws IOException {
		mEntryNumber = MiniNumber.ReadFromStream(zIn);
		mRow         = MiniNumber.ReadFromStream(zIn).getAsInt();
		mData        = MMRData.ReadFromStream(zIn);
		mIsEmpty     = false;
	}
}
