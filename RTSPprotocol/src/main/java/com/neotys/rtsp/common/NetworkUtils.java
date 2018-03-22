package com.neotys.rtsp.common;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class NetworkUtils {

	  public boolean isPortInUse(int portNumber) {
	        boolean result;

	        try {

	            DatagramSocket s = new DatagramSocket( portNumber);
	            s.close();
	            s.disconnect();
	            s=null;
	            result = true;

	        }
	        catch(Exception e) {
	            result = false;
	        }

	        return(result);
	}
	 public static boolean isPortInUse(int portNumber,String address) {
	        boolean result;
	        DatagramSocket s=null;
	        ServerSocket ss=null;
	        InetSocketAddress add= new InetSocketAddress(address, portNumber);
	        try {

	            s = new DatagramSocket( null);
	            ss = new ServerSocket();
	            ss.bind(add);
	      
	            ss.setReuseAddress(true);
	            s.bind(add);
	            s.setReuseAddress(true);
	            s.close();
	            s.disconnect();
	            
	            ss.close();
	            ss=null;
	            s=null;
	            result = false;

	        }
	        catch(Exception e) {
	            result = true;
	           
	        }
	        finally{
	        	 if(s!=null)
		            {
		            	s.close();
		            	s.disconnect();
		            	s=null;
		            }
	        	 if(ss!=null)
	        	 {
	        		 try {
						ss.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	 	            ss=null;
	        	 }
	        }
	        return(result);
	}
}
