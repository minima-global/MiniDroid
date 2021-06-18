package org.minima.system.network.minidapps;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.minima.objects.base.MiniData;
import org.minima.objects.base.MiniString;
import org.minima.system.Main;
import org.minima.system.brains.BackupManager;
import org.minima.system.network.commands.CMD;
import org.minima.system.network.minidapps.minihub.hexdata.downloadpng;
import org.minima.system.network.minidapps.minihub.hexdata.faviconico;
import org.minima.system.network.minidapps.minihub.hexdata.helphtml;
import org.minima.system.network.minidapps.minihub.hexdata.iconpng;
import org.minima.system.network.minidapps.minihub.hexdata.indexhtml;
import org.minima.system.network.minidapps.minihub.hexdata.installdapphtml;
import org.minima.system.network.minidapps.minihub.hexdata.invalidlogonhtml;
import org.minima.system.network.minidapps.minihub.hexdata.logonhtml;
import org.minima.system.network.minidapps.minihub.hexdata.manropettf;
import org.minima.system.network.minidapps.minihub.hexdata.minidapphubpng;
import org.minima.system.network.minidapps.minihub.hexdata.minidappscss;
import org.minima.system.network.minidapps.minihub.hexdata.sharepng;
import org.minima.system.network.minidapps.minihub.hexdata.tilegreyjpeg;
import org.minima.system.network.minidapps.minihub.hexdata.uninstalldapphtml;
import org.minima.system.network.minidapps.minihub.hexdata.uninstallpng;
import org.minima.utils.MiniFile;
import org.minima.utils.MinimaLogger;
import org.minima.utils.json.JSONArray;
import org.minima.utils.json.JSONObject;
import org.minima.utils.messages.Message;
import org.minima.utils.nanohttpd.protocols.http.IHTTPSession;
import org.minima.utils.nanohttpd.protocols.http.NanoHTTPD;
import org.minima.utils.nanohttpd.protocols.http.request.Method;
import org.minima.utils.nanohttpd.protocols.http.response.Response;
import org.minima.utils.nanohttpd.protocols.http.response.Status;

public class DAPPServer extends NanoHTTPD{

	DAPPManager mDAPPManager;
	
	BackupManager mBackup;

	/**
	 * Store the current page and MiniDAPP list..
	 * So you don't recreate it EVERY time..
	 */
	String mCurrentIndex     = "**";
	String mCurrentMiniDAPPS = "**";
	
	File mWebRoot;
	
	Hashtable<String, JSONObject> mParams;
	
	private String LOGON_SESSIONID_CODE = "0xFFEEDD";
	
	public DAPPServer(int zPort, DAPPManager zDAPPManager) {
		super(zPort);
		
		mDAPPManager = zDAPPManager;
		mBackup      = Main.getMainHandler().getBackupManager();
		mWebRoot     = mBackup.getWebRoot();
		
		//Store of all the params and files for a MiniDAPP..
		mParams = new Hashtable<>();
	}

	public void setLogonCode(String zCode) {
		LOGON_SESSIONID_CODE = zCode;
	}
	
	public String getLogonCode() {
		return LOGON_SESSIONID_CODE;
	}
	
	@Override
    public Response serve(IHTTPSession session) {
        try {
        	//GET or POST
        	Method method = session.getMethod();
			
        	//What are they looking for..
        	String fileRequested = session.getUri();
//        	MinimaLogger.log("RPC REQUEST "+fileRequested);
        	
        	//Which MiniDAPP
        	String MiniDAPPID="";
        	String ref = session.getHeaders().get("referer");
        	if(ref == null) {
        		ref = fileRequested;
        	}
        	//MinimaLogger.log("HEADERS "+session.getHeaders());
        	if(ref != null) {
        		int start  = ref.indexOf("0x");
        		int end    = -1;
        		if(start!=-1) {
        			end    = ref.indexOf("/", start);
        		}
        		if(end!=-1) {
        			MiniDAPPID = ref.substring(start, end);
        		}
        	}

        	//Quick clean
			if(fileRequested.endsWith("/")) {
				fileRequested = fileRequested.concat("index.html");
			}
			if(fileRequested.startsWith("/")) {
				fileRequested = fileRequested.substring(1);
			}
        	
			//Any parameters
        	Map<String, List<String>> params = new HashMap<>();
        	
        	//Any Files Uploaded..
        	Map<String, String> files = new HashMap<String, String>();
            
        	//GET or POST
			if(Method.GET.equals(method)) {
				//Any parameters
	        	params = session.getParameters();
			
			}else if(Method.POST.equals(method)) {
				//get the files.. if any MUST DO THIS FIRST - for NANOHTTPD
		        session.parseBody(files);
            
	            //NOW - get any parameters
	        	params = session.getParameters();
			}
			
			//If there are any files uploaded.. move them to the Files folder..
			JSONObject minparams = new JSONObject();
			if(!params.isEmpty()) {
				//Get all the parameters..
				Set<String> keys =  params.keySet();
				for(String key : keys) {
					//Get the value..
					String value = params.get(key).get(0);
					
					//Add it..
					minparams.put(key, value);
				}
				
				//MinimaLogger.log("MiniDAPP:"+MiniDAPPID+" PARAMS:"+minparams);
				
				//Store it..
				mParams.put(MiniDAPPID, minparams);
			}
			
			//MINIHUB..
			int slash = fileRequested.indexOf("/");
			boolean isroot = (slash == -1);
			
				//Are we using the MiniHUB..!
			if(isroot) {
				
				if(fileRequested.equals("index.html")) {
					//Valid logon ?
					boolean validlogon   = false;
					boolean logonattempt = false;
					
					//Check if the correct sessionId is present..
					if(!params.isEmpty() && params.get("sessionid")!=null) {
						logonattempt = true;
						
						//Get the sessionod
						String sesh = params.get("sessionid").get(0);
						if(sesh.equals(LOGON_SESSIONID_CODE)) {
							//we good!
							validlogon = true;
						}
					}
					
					//Show page depending..
					if(validlogon) {
						//And create the Page...
						String page    = new String(indexhtml.returnData(),StandardCharsets.UTF_8);
						String newpage = page.replace("######", createMiniDAPPList());
						return getOKResponse(newpage.getBytes(), "text/html");
					}else if(logonattempt) {
						//Small Pause.. to stop someone grinding the keys
						Thread.sleep(2000);
						return getOKResponse(invalidlogonhtml.returnData(), "text/html");
					}
						
					//Otherwise.. show logon page..
					return getOKResponse(logonhtml.returnData(), "text/html");
					
				}else if(fileRequested.equals("logon.html")) {
					return getOKResponse(logonhtml.returnData(), "text/html");
					
				}else if(fileRequested.equals("minidapps.css")) {
					return getOKResponse(minidappscss.returnData(), "text/css");
					
				}else if(fileRequested.equals("favicon.ico")) {
					return getOKResponse(faviconico.returnData(), "image/ico");
					
				}else if(fileRequested.equals("help.html")) {
					return getOKResponse(helphtml.returnData(), "text/html");
				
				}else if(fileRequested.equals("icon.png")) {
					return getOKResponse(iconpng.returnData(), "image/png");
					
				}else if(fileRequested.equals("installdapp.html")) {
					//Get the File..
		            String minidappfile = files.get("minidapp");
		            
		            //Load the file..
		            byte[] file = MiniFile.readCompleteFile(new File(minidappfile));
					
		            //Create a MiniData Object..
		            MiniData dapp = new MiniData(file);
					
					//POST it..
//		            MinimaLogger.log("Attempt install : "+params.get("minidapp").get(0));
					
		            Message msg = new Message(DAPPManager.DAPP_INSTALL);
					msg.addObject("filename", params.get("minidapp").get(0));
					msg.addObject("minidapp", dapp);
					mDAPPManager.PostMessage(msg);
		            
	                return getOKResponse(installdapphtml.returnData(), "text/html");
					
				}else if(fileRequested.equals("uninstalldapp.html")) {
//					MinimaLogger.log("Attempt uninstall : "+params.get("uninstall").get(0));
					
					//POST it..
					Message msg = new Message(DAPPManager.DAPP_UNINSTALL);
					msg.addObject("minidapp", params.get("uninstall").get(0));
					mDAPPManager.PostMessage(msg);
			            
		            return getOKResponse(uninstalldapphtml.returnData(), "text/html");
				
				}else if(fileRequested.equals("tile-grey.jpeg")) {
					return getOKResponse(tilegreyjpeg.returnData(), "image/jpeg");	
				
				}else if(fileRequested.equals("download.png")) {
					return getOKResponse(downloadpng.returnData(), "image/jpeg");	
				
				}else if(fileRequested.equals("share.png")) {
					return getOKResponse(sharepng.returnData(), "image/jpeg");	
				
				}else if(fileRequested.equals("uninstall.png")) {
					return getOKResponse(uninstallpng.returnData(), "image/jpeg");	
				
				}else if(fileRequested.equals("minidapphub.png")) {
					return getOKResponse(minidapphubpng.returnData(), "image/jpeg");	
				
				}else if(fileRequested.equals("Manrope.ttf")) {
					return getOKResponse(manropettf.returnData(), "font/ttf");	
				
				}else if(fileRequested.equals("params")) {
					JSONObject pp = mParams.get(MiniDAPPID);
					if(pp == null) {
						pp = new JSONObject();
					}
					
					//Return the parameters..
					return getOKResponse(pp.toString().getBytes(), "text/plain"); 
				}else{
					MinimaLogger.log("ROOT Minihub File not found : "+fileRequested);
					
					return getNotFoundResponse();
				}
			}else if(fileRequested.startsWith("api/")) {
				//Which minidapp..
				int mini = fileRequested.indexOf("/",4);
				if(mini == -1) {
					MinimaLogger.log("Incorrect input for API call "+fileRequested);
					return getNotFoundResponse();
				}
				
				//Get the MiniDAPP Name..
				String name = fileRequested.substring(4,mini);
				
				//Run it..
				return runAPIcall(name, minparams);
			}
			
			//Are we uploading a file..
			if(!files.isEmpty()) {
				//Make the uploads folder
				File filefolder = mBackup.getMiniDAPPFolder(MiniDAPPID);
				File uploads    = new File(filefolder,"uploads"); 
				uploads.mkdirs();
				
				Set<String> keys = files.keySet();
				for(String key : keys) {
					//get the file..
					String thefile    = files.get(key);
					File uploadedfile = new File(thefile);
					
					//Get the filename.. 
					String filename = params.get(key).get(0);
					
					//Now move that file..
					File newfile = new File(uploads,filename);
					if(newfile.exists()) {
						newfile.delete();
					}
					
					//And Move..
					uploadedfile.renameTo(newfile);
				}
			}
			
			//Get the default file..
			File fullfile = new File(mWebRoot,fileRequested); 
			
			//Does it exist..
			if(!fullfile.exists()) {
				MinimaLogger.log("MiniDAPP file not found : "+fullfile.getAbsolutePath());
				return getNotFoundResponse();
			}
			
			//Load it..
			byte[] file   = MiniFile.readCompleteFile(fullfile);
			
			//Need to check if allowed.. hmm.. TODO
			if(file.length>0) {
				return getOKResponse(file, MiniFile.getContentType(fullfile.getAbsolutePath()));
			}
			
			return getNotFoundResponse();
			
        } catch (Exception ioe) {
        	MinimaLogger.log("DAPPSERVER Error : "+ioe);
        	MinimaLogger.log(ioe);
        	
        	return getInternalErrorResponse("INTERNAL ERROR");
        }
    }
	
	/**
	 * Send a API call to a specific MiniDAPP
	 * @param zMiniDAPP
	 * @param zParams
	 * @throws UnsupportedEncodingException 
	 */
	protected Response runAPIcall(String zMiniDAPP, JSONObject zParams) throws UnsupportedEncodingException {
		//Assume ID by defult
		String minidappid = zMiniDAPP;
		
		//Get the MIniDAPP
		if(!zMiniDAPP.startsWith("0x")) {
			//It's the name of the mindapp
			minidappid = mDAPPManager.getMiniDAPPID(zMiniDAPP);
			
			if(minidappid.equals("")) {
				MinimaLogger.log("API call fail to MiniDAPP "+zMiniDAPP);
				return getNotFoundResponse();
			}
		}
		
		//Get the params..
		String uriparam = zParams.toString();
		String encparams = URLEncoder.encode(uriparam, "UTF-8");
		
		//now post it..
		CMD poster = new CMD("minidapps post:"+minidappid+" "+encparams);
		poster.run();
		
		//Return the result
		return getOKResponse(poster.getFinalResult().getBytes(MiniString.MINIMA_CHARSET), "text/txt");
	}
	
	protected Response getOKResponse(byte[] zHTML, String zContentType) {
		Response resp = Response.newFixedLengthResponse(Status.OK, zContentType, zHTML);
		resp.addHeader("Server", "HTTP RPC Server from Minima v0.95.14");
		resp.addHeader("Date", new Date().toString());
		
		//CORS
		resp.addHeader("Access-Control-Allow-Origin", "*");
		
		//Cache images..
		if(zContentType.startsWith("image/")) {
			resp.addHeader("Cache-Control", "max-age=86400");
		}
				
        return resp;
    }
	
	protected Response getForbiddenResponse(String s) {
        return Response.newFixedLengthResponse(Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT, "FORBIDDEN: " + s);
    }

    protected Response getInternalErrorResponse(String s) {
        return Response.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "INTERNAL ERROR: " + s);
    }

    protected Response getNotFoundResponse() {
        return Response.newFixedLengthResponse(Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");
    }
	
    
    public String createMiniDAPPList() throws Exception {
		//get the current MiniDAPPS
		JSONArray alldapps = mDAPPManager.getMiniDAPPS();
		
		//Check if there is a change
		String alldappstr = alldapps.toString();
		if(alldappstr.equals(mCurrentMiniDAPPS)) {
			return mCurrentIndex;
		}
		
		//Store it..
		mCurrentMiniDAPPS = alldappstr;
		
		//Build it..
		StringBuilder list = new StringBuilder();
		//list.append("<table width=100%>");
		
		int len = alldapps.size();
		for(int i=0;i<len;i++) {
			JSONObject app = (JSONObject) alldapps.get(i);
			
			//Now do it..
			String root  = (String) app.get("root");
			String uid  = (String) app.get("uid");
			
			//Get the code for this MiniDAPP to logon..
			String codeid = mDAPPManager.getMiniDAPPCodeID(uid);
			
			String name  = (String) app.get("name");
			String desc  = (String) app.get("description");
//			String backg = root+"/"+(String) app.get("background");
			String icon  = root+"/"+(String) app.get("icon");
			String webpage  = root+"/index.html?minicodeid="+codeid;
			String download =  (String) app.get("download");
			
			String version  = "1.0";
			if(app.containsKey("version")) {
				version = (String) app.get("version");
			}
			String openpage = "_"+name;
	
			boolean debug = false;
			if(uid.length()<16) {
				debug = true;
			}
			
//			String date = MinimaLogger.DATEFORMAT.format(new Date((Long)app.get("installed"))); 
//			String version = uid+" @ "+date;
			
			String openwebpage = "window.open(\""+webpage+"\",\""+openpage+"\");";
			
			String minis = "<table class='minidapp' width=100% border=0>\n"
					+ "			<tr>\n"
					+ "				<td onclick='"+openwebpage+"' rowspan=3>\n"
					+ "					<img height=50 src='"+icon+"' style='vertical-align:middle;cursor:pointer;border-radius:10px;'>&nbsp;&nbsp; 	\n"
					+ "				</td>\n"
					+ "				<td onclick='"+openwebpage+"' style='cursor:pointer;font-size:16px;'><B>"+name+"</B></td>\n"
					+ "				<td rowspan=3 nowrap>\n"
					+ "					&nbsp;<a href='"+download+"' download><img height=30 src='share.png'></a>&nbsp;&nbsp;&nbsp;\n"
					+ "					<img style='cursor:pointer;' onclick=\"uninstallDAPP('"+name+"','"+uid+"');\" height=30 src='uninstall.png'>&nbsp;\n"
					+ "				</td>\n"
					+ "			</tr>\n"
					+ "			<tr>\n"
					+ "				<td onclick='"+openwebpage+"' style='max-width: 0;cursor:pointer;font-size:10px;vertical-align:top;white-space: nowrap;overflow: hidden;text-overflow: ellipsis;text-overflow: ellipsis;' width=100% height=100%>\n"
					+ "				 "+desc
					+ "				</td>\n"
					+ "			</tr>\n"
					+ "			<tr>\n"
					+ "				<td onclick='"+openwebpage+"' style='cursor:pointer;font-size:8px;vertical-align:top;' width=100% height=100%>\n"
					+ "				 "+version
					+ "				</td>\n"
					+ "			</tr>\n"
					+ "		</table>\n"
					+ "		\n"
					+ "		<div style='height:15px; width:100%; clear:both;'></div>\n"
					+ "		";
			
			//Add to the list
			list.append(minis);
		}
		
		if(len == 0) {
			list.append("<table width=100%><tr><td style='text-align:center;font-size:'><br><br><b>No MiniDAPPs installed yet..</b>"
					+ "<br><br>"
//					+ "Go to <a href='http://mifi.minima.global/' target='_blank'>http://mifi.minima.global/</a> to find MiniDAPPs"
					+ "</td></tr></table>");
		}
		
		//Store A copy..
		mCurrentIndex = list.toString();
		
		return mCurrentIndex;
	}
	
    
    
    /**
     * Load a resource..
     * @param resource
     * @return
     */
    public URL getResource(String resource){
        URL url ;

        //Try with the Thread Context Loader. 
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if(classLoader != null){
            url = classLoader.getResource(resource);
            if(url != null){
                return url;
            }
        }

        //Let's now try with the classloader that loaded this class.
        classLoader = getClass().getClassLoader();
        if(classLoader != null){
            url = classLoader.getResource(resource);
            if(url != null){
                return url;
            }
        }

        //Last ditch attempt. Get the resource from the classpath.
        return ClassLoader.getSystemResource(resource);
    }
}
