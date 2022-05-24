package org.minima.system.network.minidapps.minihub.hexdata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.minima.objects.base.MiniData;

public class indexhtml {
	//FILE SIZE 3159
	//HEX NUM 4
	public final static int HEXNUM = 4;
	public final static byte[][] HEXDATA = new byte[4][];
	public static byte[] FINAL_ARRAY = null;
	static {
	//0 - 1000
	HEXDATA[0] = new MiniData("0x3C68746D6C3E0D0A3C686561643E0D0A0D0A093C6C696E6B2072656C3D277374796C6573686565742720747970653D27746578742F6373732720687265663D276D696E6964617070732E637373273E0D0A202020200D0A093C6D657461206E616D653D2776696577706F72742720636F6E74656E743D2777696474683D6465766963652D77696474682C20696E697469616C2D7363616C653D312E302C206D6178696D756D2D7363616C653D31273E0D0A0D0A093C7469746C653E4D494E4944415050533C2F7469746C653E0D0A0D0A3C2F686561643E0D0A0D0A3C626F6479207374796C653D276261636B67726F756E642D636F6C6F723A20236630663066613B666F6E742D66616D696C793A6D616E726F70653B273E0D0A0D0A3C73637269707420747970653D27746578742F6A617661736372697074273E0D0A0D0A092F2F5761697420666F7220746865207061676520746F206C6F61642E2E0D0A0977696E646F772E6164644576656E744C697374656E657228276C6F6164272C2066756E6374696F6E28297B0D0A0909636F6E737420696E707574456C656D656E74203D20646F63756D656E742E676574456C656D656E7442794964282766696C652D696E70757427293B0D0A0909696E707574456C656D656E742E6164644576656E744C697374656E657228276368616E6765272C2068616E646C6546696C65732C2066616C7365293B0D0A09090D0A090966756E6374696F6E2068616E646C6546696C65732829207B0D0A09092020646F63756D656E742E676574456C656D656E74427949642827696E7374616C6C666F726D27292E7375626D697428293B0D0A09097D0D0A097D293B0D0A090D0A0D0A0966756E6374696F6E20696E7374616C6C4441505028297B0D0A09092F2F4F70656E20612066696C65206469616C6F672E2E0D0A0909646F63756D656E742E676574456C656D656E7442794964282766696C652D696E70757427292E636C69636B28293B0D0A097D0D0A090D0A0966756E6374696F6E20756E696E7374616C6C44415050286E616D652C20726F6F74297B0D0A0909696628636F6E6669726D282741726520796F75207375726520796F75207769736820746F20756E696E7374616C6C20272B6E616D652B27203F2729297B0D0A0909092F2F556E696E7374616C6C207468697320726F6F74206170702E2E0D0A09090977696E646F772E6C6F636174696F6E2E687265663D27756E696E7374616C6C646170702E68746D6C3F756E696E7374616C6C3D272B726F6F743B0D0A09097D0D0A097D0D0A0D0A3C2F7363726970743E0D0A0D0A3C7461626C6520616C69676E3D2763656E74657227207374796C653D2777696474683A313030253B686569").getData();
	//1000 - 2000
	HEXDATA[1] = new MiniData("0x6768743A313030253B6D61782D77696474683A3730303B273E0D0A3C74723E0D0A3C74643E0D0A090D0A093C212D2D204D41494E205449544C45202D2D3E0D0A093C7461626C6520636C6173733D276D61696E7469746C65272077696474683D313030253E0D0A09093C74723E0D0A0909093C7464207374796C653D27746578742D616C69676E3A6C6566743B27206E6F777261703E266E6273703B266E6273703B3C696D67206865696768743D3330207372633D276D696E69646170706875622E706E67273E203C2F74643E0D0A0909093C7464207374796C653D27746578742D616C69676E3A72696768743B636F6C6F723A2366306630666127206E6F777261703E266E6273703B266E6273703B266E6273703B266E6273703B506F7765726564206279204D696E696D6120266E6273703B266E6273703B3C2F74643E090D0A09093C2F74723E0D0A093C2F7461626C653E0D0A090D0A3C2F74643E0D0A3C2F74723E0D0A0D0A3C74723E0D0A093C74642077696474683D31303025206865696768743D3130302520636C6173733D276D696E69646170706C697374273E0D0A09092323232323230D0A09090D0A09093C212D2D0D0A09093C7461626C6520636C6173733D276D696E6964617070272077696474683D3130302520626F726465723D303E0D0A0909093C74723E0D0A090909093C7464206F6E636C69636B3D2777696E646F772E6F70656E2822626C61682E68746D6C22293B2720726F777370616E3D323E0D0A09090909093C696D67206865696768743D3530207372633D2769636F6E2E706E6727207374796C653D27637572736F723A706F696E7465723B626F726465722D7261646975733A313070783B273E266E6273703B266E6273703B20090D0A090909093C2F74643E0D0A090909093C7464206F6E636C69636B3D2777696E646F772E6F70656E2822626C61682E68746D6C22293B27207374796C653D27637572736F723A706F696E7465723B666F6E742D73697A653A313670783B273E3C423E4D696E696D612057616C6C65743C2F423E3C2F74643E0D0A090909093C746420726F777370616E3D32206E6F777261703E0D0A0909090909266E6273703B3C6120687265663D272720646F776E6C6F61643E3C696D67206865696768743D3330207372633D27646F776E6C6F61642E706E67273E3C2F613E266E6273703B266E6273703B266E6273703B0D0A09090909093C696D6720206F6E636C69636B3D27756E696E7374616C6C4441505028293B27206865696768743D3330207372633D27756E696E7374616C6C2E706E67273E266E6273703B0D0A090909093C2F74643E0D0A0909093C2F74723E0D0A0909093C74723E0D0A090909093C7464206F6E").getData();
	//2000 - 3000
	HEXDATA[2] = new MiniData("0x636C69636B3D2777696E646F772E6F70656E2822626C61682E68746D6C22293B27207374796C653D27637572736F723A706F696E7465723B666F6E742D73697A653A313070783B766572746963616C2D616C69676E3A746F703B272077696474683D31303025206865696768743D313030253E0D0A090909092053696D706C65204F6E2D436861696E20444558202820446563656E7472616C697365642045786368616E676520292E2E204E4F204B59432E2E20302520466565732031303025205365637572652E2E204265617420546861742E0D0A090909093C2F74643E0D0A0909093C2F74723E0D0A09093C2F7461626C653E0D0A09090D0A09093C646976207374796C653D276865696768743A313570783B2077696474683A313030253B20636C6561723A626F74683B273E3C2F6469763E0D0A09092D2D3E0D0A09090D0A093C2F74643E0D0A3C2F74723E0D0A0D0A3C74723E0D0A093C74643E0D0A0D0A093C7461626C6520636C6173733D276D61696E666F6F746572272077696474683D3130302520626F726465723D303E0D0A09093C74723E0D0A0909093C746420636C6173733D27627574746F6E626F7827207374796C653D27746578742D616C69676E3A72696768743B27206E6F777261703E200D0A090909093C627574746F6E20636C6173733D27696E7374616C6C6461707027206F6E636C69636B3D2277696E646F772E6C6F636174696F6E2E687265663D2768656C702E68746D6C273B223E48656C703C2F627574746F6E3E266E6273703B200D0A0909093C2F74643E0D0A0909090D0A0909093C746420636C6173733D27627574746F6E626F7827207374796C653D27746578742D616C69676E3A6C6566743B27206E6F777261703E200D0A09090909266E6273703B3C627574746F6E20636C6173733D27696E7374616C6C6461707027206F6E636C69636B3D27696E7374616C6C4441505028293B273E496E7374616C6C3C2F627574746F6E3E20090D0A0909093C2F74643E0D0A09093C2F74723E0D0A093C2F7461626C653E0D0A090D0A3C2F74643E0D0A3C2F74723E0D0A3C2F7461626C653E0D0A0D0A3C212D2D205468652061637475616C20666F726D2074686174206973207573656420746F2075706C6F6164206D696E696461707073202D2D3E0D0A3C666F726D20656E63747970653D276D756C7469706172742F666F726D2D64617461272069643D27696E7374616C6C666F726D2720616374696F6E3D27696E7374616C6C646170702E68746D6C27206D6574686F643D27706F737427207374796C653D27646973706C61793A6E6F6E653B273E0D0A202020203C212D2D205573656420746F206F70656E207468652073656C6563742046696C65").getData();
	//3000 - 3159
	HEXDATA[3] = new MiniData("0x204469616C6F672E2E202D2D3E0D0A093C696E707574206163636570743D272E6D696E6964617070272069643D2766696C652D696E707574272076616C75653D276E6F66696C652720747970653D2766696C6527206E616D653D276D696E696461707027207374796C653D27646973706C61793A6E6F6E653B27202F3E200D0A3C2F666F726D3E0D0A0D0A0D0A3C2F626F64793E0D0A0D0A3C2F68746D6C3E").getData();
	}

	                public static byte[] returnData() throws IOException {
	                        if(FINAL_ARRAY == null) {
	                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
	                                for(int i=0;i<HEXNUM;i++) {
	                                        baos.write(HEXDATA[i]);
	                                }
	                                baos.flush();
	                                FINAL_ARRAY = baos.toByteArray();
	                        }
	                    return FINAL_ARRAY;
	                }
}