package com.neotys.rtsp.common;

import java.nio.ByteBuffer;

public class RTCPpacket {
	    final  int HEADER_SIZE ;
	    final  int BODY_SIZE ;

		public int Version;			// Version number 2
	    public int Padding;			// Padding of packet
	    public int RC; 				// Reception report count = 1 for one receiver
	    public int PayloadType;		// 201 for Receiver Report
	    public int length;			// 1 source is always 32 bytes: 8 header, 24 body
	    public int Ssrc;			// Ssrc of sender
	    public int identifier;
	    public float fractionLost;	// The fraction of RTP data packets from sender lost since the previous RR packet was sent
	    public int cumLost;			// The total number of RTP data packets from sender that have been lost since the beginning of reception.
	    public int highSeqNb;		// Highest sequence number received
	    public int jitter;			// Not used
	    public int LSR =0;				// Not used
	    public int DLSR=0;			// Not used
	    private int intervealed;
		public byte[] header;	//Bitstream of header
		public byte[] body;		//Bitstream of the body

		 // Constructor from field values
	    public RTCPpacket(float fractionLost, int cumLost, int highSeqNb,int Source,int SS) {
	    	Version = 2;
	    	Padding = 0;
	    	RC = Math.round(1);
	    	PayloadType = 201;
	    	length = 7;
	    	Ssrc=SS;
	    	//Other fields not used
	    	identifier=Source;
	    	this.fractionLost = fractionLost;
	    	this.cumLost = cumLost;
	    	this.highSeqNb = highSeqNb;
	    	HEADER_SIZE = 8;
		    BODY_SIZE = 24;
	    	//Construct the bitstreams
	    	header = new byte[HEADER_SIZE];
	    	body = new byte[BODY_SIZE];

	   		header[0] = (byte)(Version << 6 | Padding << 5 | RC);
	        header[1] = (byte)(PayloadType & 0xFF);
	        header[2] = (byte)(length >> 8);
	        header[3] = (byte)(length & 0xFF); 
	        header[4] = (byte)(Ssrc >> 24);
	        header[5] = (byte)(Ssrc >> 16);
	        header[6] = (byte)(Ssrc >> 8);
	        header[7] = (byte)(Ssrc & 0xFF);

			ByteBuffer bb = ByteBuffer.wrap(body);
			bb.putInt(identifier);
			bb.putFloat(fractionLost);
			bb.putInt(cumLost);
			bb.putInt(highSeqNb);
			
	    }
	    public void SetSsrc(int i)
	    {
	    	Ssrc=i;
	    }
	    
	    public RTCPpacket(float fractionLost, int cumLost, int highSeqNb,int ss) {
	    	Version = 2;
	    	Padding = 0;
	    	RC = Math.round(1);
	    	PayloadType = 201;
	    	length = 7;
	    	//Other fields not used
	    	identifier=0;
	    	this.fractionLost = fractionLost;
	    	this.cumLost = cumLost;
	    	this.highSeqNb = highSeqNb;
	    	Ssrc=ss;
	    	HEADER_SIZE = 8;
		    BODY_SIZE = 24;
	    	//Construct the bitstreams
	    	header = new byte[HEADER_SIZE];
	    	body = new byte[BODY_SIZE];

	   		header[0] = (byte)(Version << 6 | Padding << 5 | RC);
	        header[1] = (byte)(PayloadType & 0xFF);
	        header[2] = (byte)(length >> 8);
	        header[3] = (byte)(length & 0xFF); 
	        header[4] = (byte)(Ssrc >> 24);
	        header[5] = (byte)(Ssrc >> 16);
	        header[6] = (byte)(Ssrc >> 8);
	        header[7] = (byte)(Ssrc & 0xFF);

			ByteBuffer bb = ByteBuffer.wrap(body);
			bb.putInt(identifier);
			bb.putFloat(fractionLost);
			bb.putInt(cumLost);
			bb.putInt(highSeqNb);
	    }
	    public RTCPpacket(String Source,int id) {
	    	Version = 2;
	    	Padding = 0;
	    	RC = 1;
	    	PayloadType = 202;
	    	length = 6;
	    	//Other fields not used
	    	Ssrc=id;
	    	HEADER_SIZE = 4;
		    BODY_SIZE = 24;

	    	//Construct the bitstreams
	    	header = new byte[HEADER_SIZE];
	    	body = new byte[BODY_SIZE];

	   		header[0] = (byte)(Version << 6 | Padding << 5 | RC);
	        header[1] = (byte)(PayloadType & 0xFF);
	        header[2] = (byte)(length >> 8);
	        header[3] = (byte)(length & 0xFF); 
	        
	       
	        
			ByteBuffer bb = ByteBuffer.wrap(body);
			bb.putInt(Ssrc);
			body[4]=1;
			body[5]=(byte)(Source.length() & 0xFF);
			int j=6;
			byte[] bs= Source.getBytes();
			for(int i=0;i<Source.getBytes().length;i++)
			{
				body[j]=bs[i];
				j++;
			}
			body[j]=0;
			j++;
			while(j<=23)
			{
				body[j]=0;
				j++;
			}
	    }
	    public void setintervealed(int inter)
	    {
	    	this.intervealed=inter;
	    }
	    // Constructor from bit stream
	    public RTCPpacket(byte[] packet, int packet_size) {

	    	HEADER_SIZE = 8;
		    BODY_SIZE = 24;
	    	header = new byte[HEADER_SIZE];
	    	body = new byte[BODY_SIZE];

	        System.arraycopy(packet, 0, header, 0, HEADER_SIZE);
	        System.arraycopy(packet, HEADER_SIZE, body, 0, BODY_SIZE);

	    	// Parse header fields
	        Version = (header[0] & 0xFF) >> 6;
	        PayloadType = header[1] & 0xFF;
	        length = (header[3] & 0xFF) + ((header[2] & 0xFF) << 8);
	        Ssrc = (header[7] & 0xFF) + ((header[6] & 0xFF) << 8) + ((header[5] & 0xFF) << 16) + ((header[4] & 0xFF) << 24);

	    	// Parse body fields
	    	ByteBuffer bb = ByteBuffer.wrap(body); // big-endian by default
	    	fractionLost = bb.getFloat();
	    	cumLost = bb.getInt();
	    	highSeqNb = bb.getInt();
	    }

	    //--------------------------
	    //getpacket: returns the packet bitstream and its length
	    //--------------------------
	    public int getpacket(byte[] packet)
	    {
	        //construct the packet = header + body
	        System.arraycopy(header, 0, packet, 0, HEADER_SIZE);
	        System.arraycopy(body, 0, packet, HEADER_SIZE, BODY_SIZE);

	        //return total size of the packet
	        return (BODY_SIZE + HEADER_SIZE);
	    }
	    //--------------------------
	    public int getinterleavedpacket(byte[] packet)
	    {
	    	byte[] inter;
	    	inter=new byte[4];
	    	inter[0]=36;
	    	inter[1]=(byte) this.intervealed;
	    	int l =BODY_SIZE + HEADER_SIZE;
	    	inter[2]= (byte)(l >> 8);
	    	inter[3] = (byte)(l & 0xFF);
	        //construct the packet = inter+ header + body
	    	System.arraycopy(inter, 0, packet, 0, 4);
	        System.arraycopy(header, 0, packet, 4, HEADER_SIZE);
	    	System.arraycopy(body, 0, packet, HEADER_SIZE+4, BODY_SIZE);

	        //return total size of the packet
	        return (4+BODY_SIZE + HEADER_SIZE);
	    }
	    //--------------------------
	    //getlength: return the total length of the RTCP packet
	    //--------------------------
	    public int getlength() {
	        return (BODY_SIZE + HEADER_SIZE);
	    }

	    public String toString() {
	    	return "[RTCP] Version: " + Version + ", Fraction Lost: " + fractionLost 
	    		   + ", Cumulative Lost: " + cumLost + ", Highest Seq Num: " + highSeqNb;
	    }
}
