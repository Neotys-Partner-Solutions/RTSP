	package com.neotys.rtsp.common;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TimerTask;

import com.neotys.rest.dataexchange.client.DataExchangeAPIClient;
import com.neotys.rest.dataexchange.model.EntryBuilder;
import com.neotys.rest.error.NeotysAPIException;

public class Aggregator extends TimerTask{
	DataExchangeAPIClient client;
	EntryBuilder entry;
	static RTP_DATA stat;
	
	private void CreateEntry(String Cat,String metricname,double value,String unit) throws GeneralSecurityException, IOException, URISyntaxException, NeotysAPIException
	{
	  	entry=new EntryBuilder(Arrays.asList("RTSP", Cat, metricname), System.currentTimeMillis());
		entry.unit(unit);
		entry.value(value);
		client.addEntry(entry.build());
	}
	public Aggregator(RTP_DATA sta,DataExchangeAPIClient c)
	{
		client=c;
		stat=sta;
	}
	public void run() {
		 
	HashMap<String, Double> map;
	   if(stat!=null)
	   {
			   map=stat.GetAggregate();
			   try {
					CreateEntry("Network","PayLodtype",(double)map.get("PayLodtype"),"byte");
					CreateEntry("Network","PacketSize",(double)map.get("PacketSize"),"byte");
					CreateEntry("Network","DataRate",(double)map.get("DataRate"),"ratio");
					CreateEntry("Network","FractionLost",(double)map.get("FractionLost"),"ratio");
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
