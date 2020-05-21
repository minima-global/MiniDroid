package org.minima.system.input.functions;

import java.util.ArrayList;

import org.minima.objects.base.MiniString;
import org.minima.system.brains.ConsensusUser;
import org.minima.system.input.CommandFunction;
import org.minima.utils.messages.Message;

public class chainsha extends CommandFunction{

	public chainsha() {
		super("chainsha");
		setHelp("[bitlength] [data_list]", "Build an MMR Hash Tree from the data list. Use with CHAINSHA in script.", "");
	}
	
	@Override
	public void doFunction(String[] zInput) throws Exception {
		//Get a response message
		Message msg = getResponseMessage(ConsensusUser.CONSENSUS_MMRTREE);
		msg.addInt("bitlength", Integer.parseInt(zInput[1])  );
		
		//Get all of the input params.. clean and send..
		ArrayList<MiniString> data = new ArrayList<>();
		for(int i=2;i<zInput.length;i++) {
			data.add(new MiniString(zInput[i]));			
		}
		
		//Send a backup message - with no request to shutdown at the end..
		msg.addObject("leaves", data);
		getMainHandler().getConsensusHandler().PostMessage(msg);
	}
	
	@Override
	public CommandFunction getNewFunction() {
		// TODO Auto-generated method stub
		return new chainsha();
	}
}
