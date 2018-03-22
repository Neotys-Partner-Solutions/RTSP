package com.neotys.rtsp.common;

import java.util.HashMap;

public class RTP_DATA {
	private long counter=0;
	private long Payloadtype=0;
	private long PacketSize=0;
	private long DataRate=0;
	private long FractionLost=0;
	
	
	public synchronized  void AddStat(double payloadType,double packetsize,double datarate,double fraction)
	{
		this.counter++;
		this.Payloadtype+=payloadType;
		this.PacketSize+=packetsize;
		this.DataRate+=datarate;
		this.FractionLost+=fraction;
	}

	private void init()
	{

		this.Payloadtype=0;
		this.PacketSize=0;
		this.DataRate=0;
		this.FractionLost=0;
		
		this.counter=0;
	}
	
	public synchronized HashMap<String,Double> GetAggregate()
	{
		HashMap<String,Double> map = new HashMap<String, Double>();
		map.put("PayLodtype",(double) (this.Payloadtype/this.counter));
		map.put("PacketSize",(double) (this.PacketSize/this.counter));
		map.put("DataRate",(double) (this.DataRate/this.counter));
		map.put("FractionLost",(double) (this.FractionLost/this.counter));
		init();
		return map;
		
	}
}
