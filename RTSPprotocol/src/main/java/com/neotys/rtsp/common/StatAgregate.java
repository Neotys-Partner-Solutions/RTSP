package com.neotys.rtsp.common;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TimerTask;

import com.neotys.rest.dataexchange.client.DataExchangeAPIClient;
import com.neotys.rest.dataexchange.model.EntryBuilder;
import com.neotys.rest.error.NeotysAPIException;

public class StatAgregate extends TimerTask{
	DataExchangeAPIClient client;
	EntryBuilder entry;
	static RTP_STAT stat;
	
	public StatAgregate(RTP_STAT sta,DataExchangeAPIClient c)
	{
		StatAgregate.stat=sta;
		client=c;
		
	}
	
	private void CreateEntry(String Cat,String metricname,double value,String unit) throws GeneralSecurityException, IOException, URISyntaxException, NeotysAPIException
    {
      	entry=new EntryBuilder(Arrays.asList("RTSP", Cat, metricname), System.currentTimeMillis());
    	entry.unit(unit);
    	entry.value(value);
    	client.addEntry(entry.build());
    }
	 public void run() {
		 
		   HashMap<String, Double> map;
		   if(stat!=null)
		   {
				   map=stat.GetAggregate();
				   try {
					CreateEntry("Network","Jitter",(double)map.get("Jitter"),"byte");
					CreateEntry("Network","Interarrival",(double)map.get("Interarrival"),"ms");
				} catch (GeneralSecurityException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NeotysAPIException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			 
		   }
	 }
}
