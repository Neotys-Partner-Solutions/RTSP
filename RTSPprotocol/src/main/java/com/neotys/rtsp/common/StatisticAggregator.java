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

public class StatisticAggregator extends TimerTask{
	DataExchangeAPIClient client;
	EntryBuilder entry;
	static RTPSTATS stat;
	
	private void CreateEntry(String Cat,String metricname,double value,String unit) throws GeneralSecurityException, IOException, URISyntaxException, NeotysAPIException
	{
	  	entry=new EntryBuilder(Arrays.asList("RTSP", Cat, metricname), System.currentTimeMillis());
		entry.unit(unit);
		entry.value(value);
		client.addEntry(entry.build());
	}
	public StatisticAggregator(RTPSTATS sta,DataExchangeAPIClient c)
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
						CreateEntry("Network","TotalRTPPackets",(double)map.get("TotalRTPPackets"),"packets");
						CreateEntry("Network","TotalBytes",(double)map.get("TotalBytes"),"byte");
						CreateEntry("Network","BadRTPpackets",(double)map.get("BadRTPpackets"),"packets");
						CreateEntry("Network","LoacalCollisions",(double)map.get("LoacalCollisions"),"packets");
						CreateEntry("Network","RemoteCollisions",(double)map.get("RemoteCollisions"),"packets");
						CreateEntry("Network","PacketsLooped",(double)map.get("PacketsLooped"),"packets");
						CreateEntry("Network","FailedTransmissions",(double)map.get("FailedTransmissions"),"packets");
						CreateEntry("Network","RTCPpackets",(double)map.get("RTCPpackets"),"packets");
						CreateEntry("Network","SrcPackets",(double)map.get("SrcPackets"),"packets");
						CreateEntry("Network","BadRtcPPackets",(double)map.get("BadRtcPPackets"),"packets");
						CreateEntry("Network","UnknownRTCPpackets",(double)map.get("UnknownRTCPpackets"),"packets");
						CreateEntry("Network","MalformedByePackets",(double)map.get("MalformedByePackets"),"packets");
						CreateEntry("Network","MalFormedRRpackets",(double)map.get("MalFormedRRpackets"),"packets");
						CreateEntry("Network","MalFormedSDESPackets",(double)map.get("MalFormedSDESPackets"),"packets");
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
