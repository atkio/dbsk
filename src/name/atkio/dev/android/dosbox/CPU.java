package name.atkio.dev.android.dosbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.qualcomm.snapdragon.sdk.deviceinfo.QCDeviceInfo;

import android.os.Build;


public class CPU {
	
	public static boolean IsKrait()
	{
		return 
		QCDeviceInfo.isSnapdragon() &&
		getInfo().contains("vfpv4");		
	}	
	
	public static  String getInfo() {

	    StringBuffer sb = new StringBuffer();

	    sb.append("abi: ").append(Build.CPU_ABI).append("\n");

	    if (new File("/proc/cpuinfo").exists()) {

	        try {

	            BufferedReader br = new BufferedReader(new FileReader(new File("/proc/cpuinfo")));

	            String aLine;

	            while ((aLine = br.readLine()) != null) {

	                sb.append(aLine + "\n");

	            }

	            if (br != null) {

	                br.close();

	            }

	        } catch (IOException e) {

	            e.printStackTrace();

	        } 

	    }

	    return sb.toString();

	}

}
