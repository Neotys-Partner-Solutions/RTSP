package com.neotys.rtsp.common;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Date;
import java.util.TimerTask;
import java.util.Timer;
import com.neotys.rest.dataexchange.client.DataExchangeAPIClient;
import com.neotys.rest.dataexchange.model.Entry;
import com.neotys.rest.dataexchange.model.EntryBuilder;
import com.neotys.rest.error.NeotysAPIException;

public class RTP_TCP {
	private Socket RTPsocket=null;
	private Socket RTPCsocket=null;
	private DataInputStream RTPreader;
	private OutputStream Streamwriter;
	private InputStream Streamreader;
	private DataInputStream RTPCreader;
	private DataOutputStream RTCPwriter;
    private String ipAddress;
    private String LocalIP;
    private int portRTPStr;
    private int portRTCPStr;
	private int LocalRTPPort;
	private int LocalRTCPPort;
	
	   //------------------
    double statDataRate;        //Rate of video data received in bytes/s
    int statTotalBytes;         //Total number of bytes received in a session
    double statStartTime;       //Time in milliseconds when start is pressed
    double statTotalPlayTime;   //Time in milliseconds of video playing since beginning
    float statFractionLost;     //Fraction of RTP data packets from sender lost since the prev packet was sent
    int statCumLost;            //Number of packets lost
    int statExpRtpNb;           //Expected Sequence number of RTP messages within the session
    int statHighSeqNb;          //Highest sequence number received in session
    Timer TimerRTP;
    Timer TimerRTPC;
    int sourceid;
    String sourceName;
    RTP_DATA data;
    RTP_STAT stat;
  // Constructor
	
	public RTP_TCP(String ServerHost,int RTCPserverport,int RTPserverport,int localRTPport , int loalRTCPport,String Ip, int LocalRTCPPort) throws Exception
	{
		this.ipAddress = ServerHost;
		this.portRTPStr = RTPserverport;
		this.portRTCPStr=RTCPserverport;
		this.LocalRTPPort =localRTPport;
		this.LocalRTCPPort =LocalRTCPPort;
		this.LocalIP=Ip;
		
		
		
	}
	
	public RTP_TCP(Socket soc,InputStream s,OutputStream RTSPBufferedWriter,String SourceName,RTP_STAT sta ,RTP_DATA dat) throws Exception
	{
		this.RTPsocket=soc;
		Streamreader=s;
		this.Streamwriter=RTSPBufferedWriter;
		
		sourceid=(int) new Date().getTime();
		sourceName=SourceName;
		data=dat;
		stat=sta;
		
		//InitCommunicationIntervealed();
		
	}
	public void UntervealedStartTimer()
	{
		TimerRTP = new Timer();
		TimerRTP.scheduleAtFixedRate(new TimerReceiverIntervealed(Streamreader,RTPsocket,data,stat),0,100);

		TimerRTPC =new Timer();
		TimerRTPC.scheduleAtFixedRate(new RTCPSender(), 500,4000); 
	}
	

	
	public void StopTimer()
	{
		TimerRTP.cancel();
		TimerRTP=null;
		TimerRTPC.cancel();
		TimerRTPC=null;
		CleanInputstream();
	}

	public void CleanInputstream()
	{
		int byteRead;
	
		
			if (RTPsocket.isConnected())
			{
				try {
					Streamreader.skip(Streamreader.available());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		
	}
	
	public void InitCommunicationIntervealed() throws Exception
	{
		if(RTPsocket!=null)
		{
		    String test="Version 3";
		  	RTPpacket rtp_packet = new RTPpacket(0,0,0,test.getBytes(),46);
		    int packet_length = rtp_packet.getlength();
		    byte[] packet_bits = new byte[packet_length];
		    rtp_packet.getpacket(packet_bits);
		    
		    Streamwriter.write(packet_bits);
		    Streamwriter.flush();
		}
		
	}
	
	
	
	public class RTCPSender extends TimerTask{
		

	      // Stats variables
	     InetAddress ServerIPAddr ;
	      DatagramSocket sock;
	      int receivedport;
	      
	      public RTCPSender() {
	           
	        }
	      
		public void run() {
		   // calculate the stats for this period
		   
		
		    //To test lost feedback on lost packets
		    // lastFractionLost = randomGenerator.nextInt(10)/10.0f;
		
		    RTCPpacket rtcp_packet = new RTCPpacket(sourceName,sourceid);
		    rtcp_packet.setintervealed(1);
		    int packet_length = rtcp_packet.getlength();
		    byte[] packet_bits = new byte[packet_length+4];
		    rtcp_packet.getinterleavedpacket(packet_bits);
		
		    try {
		    	Streamwriter.write(packet_bits);
		    	Streamwriter.flush();
		    } catch (InterruptedIOException iioe) {
		        System.out.println("Nothing to read");
		    } catch (IOException ioe) {
		        System.out.println("Exception caught: "+ioe);
		    }
		 }

	}
	
	public class TimerReceiver extends TimerTask
	{
		DataInputStream reader;
		private DatagramPacket rcvdp = null;
		// Buffer to receive the data
		byte[] buf = null;	
		DataExchangeAPIClient client;
		EntryBuilder entry;
		private int numPktsExpected;    // Number of RTP packets expected since the last RTCP packet
	    private int numPktsLost;        // Number of RTP packets lost since the last RTCP packet
	    private int lastHighSeqNb;      // The last highest Seq number received
	    private int lastCumLost;        // The last cumulative packets lost
	    private float lastFractionLost; // The last fraction lost

	    
		public TimerReceiver(DataExchangeAPIClient client,DataInputStream read) {
			// TODO Auto-generated constructor stub
	    	this.client=client;
	    	this.reader=read;
		}
		private void CreateEntry(String Cat,String metricname,double value,String unit) throws GeneralSecurityException, IOException, URISyntaxException, NeotysAPIException
	    {
	      	entry=new EntryBuilder(Arrays.asList("RTSP", Cat, metricname), System.currentTimeMillis());
	    	entry.unit(unit);
	    	entry.value(value);
	    	client.addEntry(entry.build());
	    }
		 public void run() {
		      // Construct a DatagramPacket
		      rcvdp = new DatagramPacket(buf, buf.length);
		     
		      try
		      {
		    	  reader.readFully(rcvdp.getData());
		    
		           double curTime = System.currentTimeMillis();
	              statTotalPlayTime += curTime - statStartTime; 
	              statStartTime = curTime;

	              //create an RTPpacket object from the DP
	              RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
	              int seqNb = rtp_packet.getsequencenumber();

	              //this is the highest seq num received
	              CreateEntry("Network","number of bytes",(double)rtp_packet.getlength(),"byte");
	              
	             
	              //print header bitstream:
	              System.out.println(rtp_packet.printheader());

	              //get the payload bitstream from the RTPpacket object
	              int payload_length = rtp_packet.getpayload_length();
	              byte [] payload = new byte[payload_length];
	              rtp_packet.getpayload(payload);

	              //compute stats and update the label in GUI
	              statExpRtpNb++;
	              if (seqNb > statHighSeqNb) {
	                  statHighSeqNb = seqNb;
	              }
	              if (statExpRtpNb != seqNb) {
	                  statCumLost++;
	              }
	              statDataRate = statTotalPlayTime == 0 ? 0 : (statTotalBytes / (statTotalPlayTime / 1000.0));
	              CreateEntry("Stat","DataRate",(double)statDataRate,"rate");
	              statFractionLost = (float)statCumLost / statHighSeqNb;
	              CreateEntry("Stat","FractionLost",(double)statFractionLost,"rate");
	              statTotalBytes += payload_length;
		       // Calculate the stats for this period
		          numPktsExpected = statHighSeqNb - lastHighSeqNb;
		          numPktsLost = statCumLost - lastCumLost;
		          lastFractionLost = numPktsExpected == 0 ? 0f : (float)numPktsLost / numPktsExpected;
		          lastHighSeqNb = statHighSeqNb;
		          lastCumLost = statCumLost;

		          //To test lost feedback on lost packets
		          // lastFractionLost = randomGenerator.nextInt(10)/10.0f;

		          /*RTCPpacket rtcp_packet = new RTCPpacket(lastFractionLost, statCumLost, statHighSeqNb);
		          int packet_length = rtcp_packet.getlength();
		          byte[] packet_bits = new byte[packet_length];
		          rtcp_packet.getpacket(packet_bits);
		          RTCPwriter.write(packet_bits);
		          RTCPwriter.flush();*/
		         
		        }
		       catch(SocketException se) { // We need to catch here if we decide to invoke TEARDOWN
		                       // while we are waiting for data coming from the DatagramSocket
		        System.out.println("Socket connection closed");
		        this.cancel();
		      }
		      catch (IOException e) {
		        System.out.println("Could not read from socket");
		        e.printStackTrace();
		        this.cancel();
		        return;
		      } catch (GeneralSecurityException e) {
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
	public class TimerReceiverIntervealed extends TimerTask
	{
		InputStream reader;
		private DatagramPacket rcvdp = null;
		// Buffer to receive the data
		byte[] buf = null;	
		RTP_DATA data;
		RTP_STAT stat;
		
		int numPackets = 0;
	    Socket soc;
	    private int numPktsExpected;    // Number of RTP packets expected since the last RTCP packet
	      private int numPktsLost;        // Number of RTP packets lost since the last RTCP packet
	      private int lastHighSeqNb;      // The last highest Seq number received
	      private int lastCumLost;        // The last cumulative packets lost
	      private float lastFractionLost; // The last fraction lost
	     private int packetcounter=0;
	     private long lasdiff=0;
	     private long lastJitter=0;
	     
		public TimerReceiverIntervealed(InputStream read,Socket S,RTP_DATA dat,RTP_STAT sta) {
			// TODO Auto-generated constructor stub
	    	data=dat;
	    	stat=sta;
	    	this.reader=read;
	    	this.soc=S;
		}
		
		 public void run() {
		      // Construct a DatagramPacket
		      int i = 0;
		      int interleaved;
		      int byteRead;
		      int byteRead2;
	    	  int lastSeqNo;
	    	  int numBytesRTP;
	    	  RTPpacket packet;
	    	  int RTpTs;
	    	  long reveivedtime;
	    	  long StatTs;
		      try
		      {
		    	
		    	  if (soc.isConnected()) {
		    		     while((byteRead = reader.read())!=-1)
		    		     {
			    	        
			    	         if (byteRead == 36)
			    	         {     // "$"
			    	        	 	 interleaved = reader.read(); // next byte is interleave
			    	                 byteRead = reader.read(); // next two bytes is # of bytes
			    	                 byteRead2 = reader.read();
			    	                 numBytesRTP = (byteRead << 8) | (byteRead2);
			    	                 if(interleaved==0 || interleaved==2 || interleaved==4)
			    	                 {
				    	                 byte[] rtppacket = new byte[numBytesRTP];
				    	                 if(reader.read(rtppacket)!=-1)
				    	                 {
				    	                
				    	                	  packet = new RTPpacket(rtppacket,numBytesRTP);
					    	               	 lastSeqNo=packet.getsequencenumber();
					    	               	RTpTs=packet.gettimestamp();
					    	               	reveivedtime=System.currentTimeMillis();
					    	               	
					    	               	if(lasdiff!=0)
					    	               	{
					    	               		StatTs=lasdiff-(reveivedtime-RTpTs);
					    	               		lastJitter=lastJitter-(Math.abs(StatTs)-lastJitter)/16;
					    	               		stat.AddStat((double)StatTs, (double)lastJitter);
						    	               
					    	               	}
					    	               	else
					    	               	{
					    	               		lasdiff=reveivedtime-RTpTs;
					    	               		
					    	               	}
					    	                  
					    	                //compute stats and update the label in GUI
					    	                statExpRtpNb++;
					    	                if (lastSeqNo > lastHighSeqNb) {
					    	                	lastHighSeqNb = lastSeqNo;
					    	                }
					    	                if (statExpRtpNb != lastSeqNo) {
					    	                    statCumLost++;
					    	                }
					    	                statDataRate = statTotalPlayTime == 0 ? 0 : (statTotalBytes / (statTotalPlayTime / 1000.0));
					    	                statFractionLost = (float)statCumLost / lastHighSeqNb;

					    	                data.AddStat(packet.getpayloadtype(), packet.getlength(), (double)statDataRate, (double)statFractionLost);
					    	                // Calculate the stats for this period
					    	                numPktsExpected = statHighSeqNb - lastHighSeqNb;
					    	  	          	numPktsLost = statCumLost - lastCumLost;
						    	  	        lastFractionLost = numPktsExpected == 0 ? 0f : (float)numPktsLost / numPktsExpected;
						    	  	        lastHighSeqNb = statHighSeqNb;
						    	  	        lastCumLost = statCumLost;
						    	  	        packetcounter++;
						    	  	          //To test lost feedback on lost packets
						    	  	          // lastFractionLost = randomGenerator.nextInt(10)/10.0f;
						    	  	        if(packetcounter>7)
						    	  	        {
							    	  	         RTCPpacket rtcp_packet = new RTCPpacket(lastFractionLost, statCumLost, lastHighSeqNb,packet.GetSourceIdentifier(),sourceid);
							    	  	         int packet_length = rtcp_packet.getlength();
							    	  	         byte[] packet_bits = new byte[packet_length+4];
							    	  	         rtcp_packet.setintervealed(interleaved+1);
							    	  	         rtcp_packet.getinterleavedpacket(packet_bits);
							    	  	         try {
							    	  	        	 Streamwriter.write(packet_bits);
							    	  	        	 Streamwriter.flush();
							    	  	        	 packetcounter=0;
							    	  	          } catch (InterruptedIOException iioe) {
							    	  	        	  System.out.println("Nothing to read");
							    	  	          } catch (IOException ioe) {
							    	  	        	  System.out.println("Exception caught: "+ioe);
							    	  	          }
						    	  	        }
					    	  	         }                        
					    	                
				    	         }
			    	        }
			    	         else
			    	         {
			    	        	 //----test if received SET PARAMETER--- server sends end of stream
			    	        	 if(byteRead==83)
			    	        	 {
			    	        		 byteRead=reader.read();
			    	        		 if(byteRead==69)
			    	        		 {	 //E
			    	        			 byteRead=reader.read();
				    	        		 if(byteRead==84)
				    	        		 	 //T
				    	        			 this.cancel();
			    	        		 } 
			    	        			 
			    	        	 }
			    	         }
		    		     }
		    	  }          // end if "$"
   	 
		    


		         
		        }
		       catch(SocketException se) { // We need to catch here if we decide to invoke TEARDOWN
		                       // while we are waiting for data coming from the DatagramSocket
		        System.out.println("Socket connection closed");
		        this.cancel();
		      }
		      catch (IOException e) {
		        System.out.println("Could not read from socket");
		        e.printStackTrace();
		        this.cancel();
		        return;
		      }
			
		    }
	}
}

