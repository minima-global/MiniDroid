package org.minima.system.commands;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.minima.system.commands.all.automine;
import org.minima.system.commands.all.backup;
import org.minima.system.commands.all.balance;
import org.minima.system.commands.all.coins;
import org.minima.system.commands.all.connect;
import org.minima.system.commands.all.debugflag;
import org.minima.system.commands.all.disconnect;
import org.minima.system.commands.all.getaddress;
import org.minima.system.commands.all.hashtest;
import org.minima.system.commands.all.help;
import org.minima.system.commands.all.incentivecash;
import org.minima.system.commands.all.maxima;
import org.minima.system.commands.all.message;
import org.minima.system.commands.all.missingcmd;
import org.minima.system.commands.all.mmrcreate;
import org.minima.system.commands.all.mmrproof;
import org.minima.system.commands.all.network;
import org.minima.system.commands.all.newaddress;
import org.minima.system.commands.all.printmmr;
import org.minima.system.commands.all.printtree;
import org.minima.system.commands.all.quit;
import org.minima.system.commands.all.restore;
import org.minima.system.commands.all.rpc;
import org.minima.system.commands.all.runscript;
import org.minima.system.commands.all.send;
import org.minima.system.commands.all.sshtunnel;
import org.minima.system.commands.all.status;
import org.minima.system.commands.all.test;
import org.minima.system.commands.all.tokencreate;
import org.minima.system.commands.all.tokens;
import org.minima.system.commands.all.trace;
import org.minima.system.commands.all.tutorial;
import org.minima.system.commands.all.txpow;
import org.minima.system.commands.all.webhooks;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.json.parser.JSONParser;
import org.minima.utils.json.parser.ParseException;

public abstract class Command {

	public static final Command[] ALL_COMMANDS = 
		{   new quit(), new status(), new coins(), new txpow(), new connect(), new disconnect(), new network(),
			new message(), new trace(), new help(), new printtree(), new automine(), new printmmr(), new rpc(),
			new send(), new balance(), new tokencreate(), new tokens(),new getaddress(), new newaddress(), new debugflag(),
			new incentivecash(), new sshtunnel(), new webhooks(),
			new backup(), new restore(), new test(), new hashtest(),
			new runscript(), new tutorial(), new maxima(),
			new mmrcreate(), new mmrproof()};
	
	String mName;
	String mHelp;
	
	JSONObject mParams = new JSONObject();
	
	public Command(String zName, String zHelp) {
		mName = zName;
		mHelp = zHelp;
	}
	
	public String getHelp() {
		return mHelp;
	}
	
	public String getFullHelp() {
		return mHelp;
	}
	
	public JSONObject getJSONReply() {
		JSONObject json = new JSONObject();
		json.put("command", getname());
		
		//Are they empty..
		if(!getParams().isEmpty()) {
			json.put("params", getParams());
		}
		
		json.put("status", true);
		return json;
	}
	
	public String getname() {
		return mName;
	}
	
	public JSONObject getParams() {
		return mParams;
	}
	
	public boolean existsParam(String zParamName) {
		return mParams.containsKey(zParamName);
	}
	
	public String getParam(String zParamName) {
		return getParam(zParamName, "");
	}
	
	public String getParam(String zParamName, String zDefault) {
		if(existsParam(zParamName)) {
			return (String) mParams.get(zParamName);
		}
		
		return zDefault;
	}
	
	public JSONObject getJSONObjectParam(String zParamName) {
		return (JSONObject) mParams.get(zParamName);
	}
	
	public JSONArray getJSONArrayParam(String zParamName) {
		return (JSONArray) mParams.get(zParamName);
	}
	
	public boolean isParamJSONObject(String zParamName) {
		if(existsParam(zParamName)) {
			Object obj = mParams.get(zParamName);
			if(obj instanceof JSONObject) {
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isParamJSONArray(String zParamName) {
		if(existsParam(zParamName)) {
			Object obj = mParams.get(zParamName);
			if(obj instanceof JSONArray) {
				return true;
			}
		}
		
		return false;
	}
	
	
	public abstract JSONObject runCommand() throws Exception;
	
	public abstract Command getFunction();
	
	/**
	 * Run a with possible multiple functions
	 * 
	 * @param zCommand
	 */
	public static JSONArray runMultiCommand(String zCommand) {
		JSONArray res = new JSONArray();
		
		//First break it up..
		StringTokenizer strtok = new StringTokenizer(zCommand, ";");
		while(strtok.hasMoreTokens()) {
			String command = strtok.nextToken().trim();
			
			//Run this command..
			Command cmd = Command.getCommand(command);
			
			//Run it..
			JSONObject result = null;
			try {
				result = cmd.runCommand();
			}catch(Exception exc) {
				MinimaLogger.log(exc);
				
				result = cmd.getJSONReply();
				result.put("status", false);
				result.put("error", exc.getMessage());
			}
			
			//Add it..
			res.add(result);
		}
		
		return res;
	}
	
	public static Command getCommand(String zCommand) {
		int commandlen = ALL_COMMANDS.length;
		
		//Get the first word..
//		String[] split = splitString(zCommand);
		String[] split = splitStringJSON(zCommand);
		
		//The first is the command..
		String command = split[0];
		
		Command comms = null;
		for(int i=0;i<commandlen;i++) {
			if(ALL_COMMANDS[i].getname().equals(command)) {
				comms = ALL_COMMANDS[i].getFunction();
				break;
			}
		}
		
		//If not found return error
		if(comms == null) {
			return new missingcmd(command,"Command not found");
		}
		
		//get the parameters if any
		int len = split.length;
		for(int i=1;i<len;i++) {
			String token = split[i];
			
			//Find the :
			int index 	 = token.indexOf(":");
			if(index == -1) {
				return new missingcmd(command,"Invalid parameters for "+command+" @ "+token);
			}
			
			
			String name  = token.substring(0, index).trim();
			String value = token.substring(index+1).trim();
			
			//Is the value a JSON..or JSONArray..
			if(value.startsWith("{") && value.endsWith("}")) {
				
				//It's a JSON..!
				JSONObject json = null;
				try {
					json = (JSONObject) new JSONParser().parse(value);
				} catch (ParseException e) {
					return new missingcmd(command,"Invalid JSON parameter for "+command+" @ "+token+" "+e.toString());
				}
				
				//Store this parameter..
				comms.getParams().put(name, json);
			
			}else if(value.startsWith("[") && value.endsWith("]")) {
				
				//It's a JSONArray..!
				JSONArray json = null;
				try {
					json = (JSONArray) new JSONParser().parse(value);
				} catch (ParseException e) {
					return new missingcmd(command,"Invalid JSON parameter for "+command+" @ "+token+" "+e.toString());
				}
				
				//Store this parameter..
				comms.getParams().put(name, json);
				
			}else {
				
				//Add normal String parameter to..
				comms.getParams().put(name, value);
				
			}
			
			
		}
		
		return comms;
	}

	/**
	 * Split the input string keeping quoted sections as single units
	 * 
	 * @param zString
	 * @return
	 */
	private static String[] splitString(String zInput) {
		ArrayList<String> token = new ArrayList<>();
		String ss = zInput.trim();
		
		//Cycle through looking for spaces or quotes..
		String current = new String();
		boolean quoted = false;
		int len = ss.length();
		for(int i=0;i<len;i++) {
			char cc = ss.charAt(i);
			
			if(cc == ' ') {
				//End of the line..
				if(!quoted) {
					//Add current
					if(!current.equals("")) {
						token.add(current.trim());
					}
						
					//New Current
					current = new String();
				}else {
					current += cc;
				}
			}else if(cc == '\"') {
				//it's a quote!
				if(quoted) {
					//It's finished..
					quoted=false;
				}else {
					quoted=true;
				}
			}else {
				current += cc;
			}
		}
		
		//Add the last bit..
		if(!current.equals("")) {
			token.add(current.trim());
		}
		
		return token.toArray(new String[0]);
	}
	
	private static String[] splitStringJSON(String zInput) {
		//Are there any JSON in this..
		if(zInput.length()>10000 && zInput.indexOf("{") == -1) {
			return splitterQuotedPattern(zInput);
		}
		
		ArrayList<String> token = new ArrayList<>();
		String ss = zInput.trim();
		
		//Cycle through looking for spaces or quotes..
		String current = new String();
		
		int jsoned 		= 0;
		boolean quoted 	= false;
		
		int len = ss.length();
		for(int i=0;i<len;i++) {
			char cc = ss.charAt(i);
			
			if(cc == ' ') {
				//End of the line..
				if(!quoted && jsoned==0) {
					
					//Add current
					if(!current.equals("")) {
						token.add(current.trim());
					}
						
					//New Current
					current = new String();
					
				}else {
					current += cc;
				}
			
			}else if(cc == '{') {
				jsoned++;
				current += cc;
				
			}else if(cc == '}') {
				jsoned--;
				current += cc;
			
			}else if(cc == '[') {
				jsoned++;
				current += cc;
				
			}else if(cc == ']') {
				jsoned--;
				current += cc;
			
			
			}else if(cc == '\"') {
				if(jsoned>0) {
					
					//It's in a JSON.. so keep it..
					current += cc;
					
				}else {
					//it's a quote!
					if(quoted) {
						//It's finished..
						quoted=false;
					}else {
						quoted=true;
					}
				}
				
			}else {
				current += cc;
			}
		}
		
		//Add the last bit..
		if(!current.equals("")) {
			token.add(current.trim());
		}
		
		return token.toArray(new String[0]);
	}
	
	public static String[] splitterQuotedPattern(String zInput) {
		ArrayList<String> token = new ArrayList<>();
		String ss = zInput.trim();
		
		String regex = "\"([^\"]*)\"|(\\S+)";
		
		Matcher m = Pattern.compile(regex).matcher(ss);
	    while (m.find()) {
	        if (m.group(1) != null) {
	        	token.add(m.group(1));
	        } else {
	        	token.add(m.group(2));
	        }
	    }
		
		return token.toArray(new String[0]);
	}
	
	public static void main(String[] zArgs) {
		
		String tester2 = "maxima action:send to:MxC41JSZ0Q1G95A34H1NN1K0G2085001Z3380620ZW0K1G40CEVC7MGRP3UTDC8ZY29NAR2AH0B3GZ8R0GKQDZ1JVV36N4QS0YPUG200VG08U14TERE8WP6WUM2N5FBV8J1EFNVQE3SCYJK2DJFV392ZE8722RY3ZU1CV3U0E2C520GY41DKEG7424U72VVQRKD6B314PC2HP2TGUTTTS76TEDPPGNZ5BMM43VZMGJVVE5FAR4D2VJTEQGBJJ081G2001@35.246.45.106:9001 application:maxweb data:0x7B22616374696F6E223A22726573706F6E7365222C2266696C65223A22696D616765735C2F6265727365726B796F2E706E67222C226C656E677468223A3133313836312C22636F6E74656E7473223A2230783839353034453437304430413141304130303030303030443439343834343532303030303031353230303030303134303038303630303030303045454342463437463030303030303031373335323437343230304145434531434539303030303030303436373431344434313030303042313846304246433631303530303030303030393730343835393733303030303045433330303030304543333031433736464138363430303030464641353439343434313534373835454243444430374234364435373539463646313135373741434438423032323736343142303737423136323037343131343134353445433732313432433938303444303030313135303336323434343230393732383031353136434530333930383741454342443137423046373545433144453742454632354446464134444337394633314337314146423943374239333446394436334143423146373545364239364237334345464633424537334545374445454244454344424245454444423645424546454641454239373337374545333337354545453741443742423245364646464636364642464243433939424243433946323242424646323242434236464644443636463244374646333337374642333743463131373746463137324537334244463739373938464637373838464535314646454531314639363937374646393937354644454644444444463744463939323246463939324535454242454545454239364246464444424246354433454634343333463734423943443644364542334643463838464646463846323331314646333331434237373746463737373246334646444433334642444643443333464644443346323241414646323241434238333145463441304535304646454530304639363737374545373737354542454543434242453643463938424246463838424535303130463738433046323035354646303035384246364343363733453733334342463730453143323732444244424445373646394231314646424231453530333345453030333936304646464630304635464245454141424245364137394531304235464238424344333342424444334632353131464635353143424342424343434342324341464630304141463330453442334445364542373142464441433733463945353937324639464646463939464246374345393937374545394632373131464637373143423033314646384330453537373746463737373937353543464535414141424145354146454631314646463731374337454133333737414133453544353545454444354336314345344131433337464644443737463244314646444431314633443734464438393946463838394231433641424245454141423245333744443734443344304439464137374239434235443936313742444538343543423646464343363646384333353546464242353546374246394237374646424237453531394346373843363732374337434243334345343231304635394445463030444446373042394531383631423936334244434531304543424246464646424246324641464645454141464245424344434342424444433930464441464646464141464337444331464643433131464243464346454546464646454632464141464646464145334439324646464632324632463546464444353546424446434531314646454531463230454546463030454342314445463738433745354544444545454544393637464639393737463139434644383835314345364645393636464639393645353142424646313142383731444445463131444446373137394439393737444439453541444446464141443937443737424244443735424645453333464645363346393943434646393943363138374346463838434346313833453630434235373741413535373141423242464434344242444434463242444446464242444342424445463744454645353044444545303044393643373345463642314342374242454537374230453146423033424246464546363646464646363538383346444546373441373342324439373544373644393738434533453746464343373746424344433743463343444333443633464642423333463342454537464437373737444437353839374544374545464637374536464639434545464643434533313937344237463538463238433243463442464537334445463339443638303031393836313146423231424642464444464646464443424346464444434346324445464632324545464232334345323131384631384636463844313146464444313631463344464643434444463143423646394634344646464634453533464646463333464337444342304341343634"
				+ "";
				
//		String tester = " maxima action:send to:Mx87898@45:9001 application:maxweb data:0x87987987987 ";
//		String tester = "maxima action:send \"to:Mx 8 7 898@45:9001\" \"application:maxweb is the app\"";
		
		String [] split = splitStringJSON(tester2);
		for(int i=0;i<split.length;i++) {
			System.out.println(i+") "+split[i]);
		}
		
		System.out.println("\nNEW WAY\n");
		
		split = splitterQuotedPattern(tester2);
		for(int i=0;i<split.length;i++) {
			System.out.println(i+") "+split[i]);
		}
	}
}
