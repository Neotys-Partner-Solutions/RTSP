package com.neotys.rtsp.common;

import java.util.HashMap;

import javax.media.rtp.GlobalReceptionStats;

public class RTPSTATS {
	long TotalRtpPackets=0;
	long TotalBytes=0;
	long BadRTPpackets=0;
	long localCollisions=0;
	long RemoteCollision=0;
	long PAcketslooped=0;
	long failedTrasmission=0;
	long RTCPpaclets=0;
	long srcpackets=0;
	long badrtcpackets=0;
	long unknownrtcptypes=0;
	long malformedrr=0;
	long malformedsdes=0;
	long malformebye=0;
	int count=0;
	
	public synchronized void addStat(GlobalReceptionStats stat)
	{
		TotalRtpPackets+=stat.getPacketsRecd();
		TotalBytes+=stat.getBytesRecd();
		BadRTPpackets+=stat.getBadRTPkts();
		localCollisions+=stat.getLocalColls() ;
		RemoteCollision+=stat.getRemoteColls() ;
		PAcketslooped+=stat.getPacketsLooped() ;
		failedTrasmission+=stat.getTransmitFailed() ;
		RTCPpaclets+= stat.getRTCPRecd() ;
		srcpackets+=stat.getSRRecd() ;
		badrtcpackets+=stat.getBadRTCPPkts() ;
		unknownrtcptypes+=stat.getUnknownTypes() ;
		malformedrr+=stat.getMalformedRR() ;
		malformedsdes+= stat.getMalformedSDES() ;
        malformedsdes+= stat.getMalformedBye() ;
        malformebye+= stat.getMalformedSR() ;
        count++;
	}
	
	private void init()
	{

		this.TotalRtpPackets=0;
		this.TotalBytes=0;
		this.BadRTPpackets=0;
		this.localCollisions=0;
		this.RemoteCollision=0;
		this.PAcketslooped=0;
		this.failedTrasmission=0;
		this.RTCPpaclets=0;
		this.srcpackets=0;
		this.badrtcpackets=0;
		this.unknownrtcptypes=0;
		this.malformebye=0;
		this.malformedrr=0;
		this.malformedsdes=0;
	
		this.count=0;
	}
	
	public synchronized HashMap<String,Double> GetAggregate()
	{
		HashMap<String,Double> map = new HashMap<String, Double>();
		map.put("TotalRTPPackets",(double) (this.TotalRtpPackets/this.count));
		map.put("TotalBytes",(double) (this.TotalBytes/this.count));
		map.put("BadRTPpackets",(double) (this.BadRTPpackets/this.count));
		map.put("LoacalCollisions",(double) (this.localCollisions/this.count));
		map.put("RemoteCollisions",(double) (this.RemoteCollision/this.count));
		map.put("PacketsLooped",(double) (this.PAcketslooped/this.count));
		map.put("FailedTransmissions",(double) (this.failedTrasmission/this.count));
		map.put("RTCPpackets",(double) (this.RTCPpaclets/this.count));
		map.put("SrcPackets",(double) (this.srcpackets/this.count));
		map.put("BadRtcPPackets",(double) (this.badrtcpackets/this.count));
		map.put("UnknownRTCPpackets",(double) (this.unknownrtcptypes/this.count));
		map.put("MalformedByePackets",(double) (this.malformebye/this.count));
		map.put("MalFormedRRpackets",(double) (this.malformedrr/this.count));
		map.put("MalFormedSDESPackets",(double) (this.malformedsdes/this.count));
		
		
		init();
		return map;
		
	}
}
