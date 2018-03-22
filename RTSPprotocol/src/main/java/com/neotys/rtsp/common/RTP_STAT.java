package com.neotys.rtsp.common;

import java.util.HashMap;

public class RTP_STAT {
	private long counter=0;
	private long delay=0;
	private long jitter=0;
	
	
	
	public synchronized  void AddStat(double delay,double jitter)
	{
		this.counter++;
		this.delay+=delay;
		this.jitter+=jitter;
		
	}

	private void init()
	{

		this.delay=0;
		this.jitter=0;
	
		this.counter=0;
	}
	
	public synchronized HashMap<String,Double> GetAggregate()
	{
		HashMap<String,Double> map = new HashMap<String, Double>();
		map.put("Interarrival",(double) (this.delay/this.counter));
		map.put("Jitter",(double) (this.jitter/this.counter));
		
		init();
		return map;
		
	}
}
