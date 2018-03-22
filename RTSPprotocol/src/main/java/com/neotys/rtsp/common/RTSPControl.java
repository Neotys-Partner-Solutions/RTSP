package com.neotys.rtsp.common;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import javax.media.Manager;
import javax.media.MediaLocator;
import javax.media.Player;

import com.neotys.rest.dataexchange.client.DataExchangeAPIClient;
import com.neotys.rest.dataexchange.client.DataExchangeAPIClientFactory;
import com.neotys.rest.dataexchange.model.Context;
import com.neotys.rest.dataexchange.model.ContextBuilder;
import com.neotys.rest.dataexchange.model.EntryBuilder;
import com.neotys.rest.error.NeotysAPIException;
import com.neotys.rtsp.common.RTP_TCP.TimerReceiverIntervealed;



public class RTSPControl {
	 // RTP Variables

	  // UDP packet received from server
	  private DatagramPacket rcvdp = null;
	  // Socket used to send and receive UDP packets
	  private DatagramSocket AudioRTPsocket = null;
	  private DatagramSocket VideoRTPsocket = null;
		 
	  // Timer used to receive data from the UDP socket
	  static InputStream  bufInStream; 
	  static OutputStream bufoutStream;
	  Timer timerOptions = null;
	  boolean ISUDP=true;
	  boolean packetpair=false;
	  RTPReceiver VideoPlayer;
	  RTPReceiver AudioPlayer;
	  String StrStartTime=null;
	  String StrRangeMethod=null;
	  String AudioUrl=null;
	  String VideoUrl= null;
	  RTP_TCP TCPRTP=null;
	  int XPlaylistGenId=0;
	  int XBroadcastid=0;
	  RTP_DATA rtp_data;
	  RTP_STAT rtp_stat;
	  RTPSTATS video_rtsp_stat;
	  RTPSTATS audio_rtsp_stat;
	  
	  Timer Rtp_aggregate;
	  Timer Rtp_statagregage;
	  boolean KeepAlive_GetParameter=false;
	  boolean ForceAudioSession=false;
	  // Buffer to receive the data
	  byte[] buf = null;
	  
	  // RTSP Variables
	  // State variables
	  enum RTSPState {
	    INIT,
	    READY,
	    PLAYING
	  };

	  // Request variables
	  enum RTSPRequest {
	    SETUP,
	    DESCRIBE,
	    PLAY,
	    PAUSE,
	    TEARDOWN,
	    OPTIONS,
	    GET_PARAMETER
	  };
	  final DataExchangeAPIClient client;
	  final ContextBuilder cb;
	  // Current state of the client
	  private RTSPState state = null;

	  // Socket used to send/receive RTSP messages
	  private Socket RTSPSocket = null;
	
	  // Input and Output stream filters
	  // Used to write RTSP messages and send to server
	  private BufferedReader RTSPBufferedReader = null;

	  // Used to receive RTSP messages and data
	  private BufferedWriter RTSPBufferedWriter = null;

	  // Video file / Location to play
	  private String videoFile = null;

	  // Sequence number of RTSP messages within the session
	  // Initially set to zero
	  private int RTSPSeqNb = 1;

	  // Changed this to a string instead of an int
	  // as the actual RTSP server I'm testing on issues sessions
	  // in alphanumeric characters.  Leave this as a string to be safe
	  private String RTSPSessionID = null; // ID of RTSP sessions - given by RTSP server

	  // Carriage return and new line to send to server
	  private final String CRLF = "\r\n";

	  // Store server port to communicate with server
	  // Usually 554 for RTSP
	  private int serverPort = 554;

	  // Flag to establish that we have set up our parameters
	  private boolean isSetup = false;

	  // Store host name
	  private String hostName = null;

	  // For the audio and video track IDs issued from
	  // the server
	  private int audioTrackID = -1;
	  private int videoTrackID = -1;
	  private int applicationTrackID = -1;
	  private String ApplicaitonTrack=null;
	  private String audioTrack=null;
	  private String VideoTrack=null;
	  // Store the audio and video payload types
	  // We will need this to figure out what data is being sent
	  // from the server
	  private int audioPT = -1;
	  private int videoPT = -1;

	  // Flags that tell us whether SETUP using video and audio are finished
	  private boolean videoSetupDone = false;
	  private boolean audioSetupDone = false;
	  private boolean applicationsetupdone = false;
	  // The type of streaming protocol extracted from the
	  // server
	  private String streamingProtocolVideo = null;
	  private String streamingProtocolAudio = null;
	  private String streamingProtocolapplication = null;
	  // The total number of tracks
	  private int numberOfTracks = 0;

	  // Flag to determine if the trackID string is just
	  // trackID or track
	  private boolean galbarmFlag = false;

	  // Boolean flag that tells us whether or not the ID of the track
	  // is referenced by the key "trackID=" or "stream="
	  // GStreamer has it as the latter, while conventional RTSP servers
	  // have it as the former
	  boolean trackIDOrStream = true;

	  // Variables that define the periodic options request sent to the
	  // RTSP server
	  // In order to prevent a time out, it's good to periodically send
	  // something to the server to let you know that you're still here
	  // You can do this very innocuously with an OPTIONS request
	  // As such, every 45 seconds we send an OPTIONS request to let
	  // the server know we're still here
	  // We will also start the timer at about 15 seconds after we schedule
	  // the task to ensure no conflicts
	  private final int TIMEROPTIONSDELAY = 25000;
	  private final int TIMEROPTIONSFREQUENCY = 59000;

	  private int RTPServerAudioPort;
	  private int RTPServervideoPort;
	  private String RTPServerAudio =null;
	  private String RTPServerVideo=null ;
	  
	  private int RTPClientAudioPort;
	  private int RTPClientvideoPort;
	  //Video constants:
	    //------------------
	    static int MJPEG_TYPE = 26; //RTP payload type for MJPEG video

	    //Statistics variables:
	    //------------------
	    double statDataRate;        //Rate of video data received in bytes/s
	    int statTotalBytes;         //Total number of bytes received in a session
	    double statStartTime;       //Time in milliseconds when start is pressed
	    double statTotalPlayTime;   //Time in milliseconds of video playing since beginning
	    float statFractionLost;     //Fraction of RTP data packets from sender lost since the prev packet was sent
	    int statCumLost;            //Number of packets lost
	    int statExpRtpNb;           //Expected Sequence number of RTP messages within the session
	    int statHighSeqNb;          //Highest sequence number received in session
	  // Constructor
	  // serverHost - Alphabetical URL or IP Address
	  // etc. www.bublcam.com or 192.168.0.100
	  // fileName - Exact relevant path and filename to what we want to play
	  // Set serverPort to -1 for default port of 554
	    
	  //RTCP variables
	    //----------------
	    DatagramSocket AudioRTCPsocket;          //UDP socket for sending RTCP packets
	    DatagramSocket VideoRTCPsocket;          //UDP socket for sending RTCP packets
	    private int AudioRTCP_RCV_PORT;   //port where the client will receive the RTP packets
	    private int VideoRTCP_RCV_PORT;   //port where the client will receive the RTP packets
	    private int AudioServerRTCP_RCV_PORT;   //port where the client will receive the RTP packets
	    private int VideoServerRTCP_RCV_PORT;   //port where the client will receive the RTP packets
	    static int RTCP_PERIOD = 1000;       //How often to send RTCP packets
	    RTCPSender rtcpSender;
	    String SourceName;
	
	    
	  public RTSPControl(String serverHost, int serverPort, String fileName,String ControllerHost, String port,String Location,String APIKEY,String name) throws Exception {
		  cb = new ContextBuilder();
		  cb.hardware("RTSPControl").location(Location).software("RTSP")

			.script("RTSPengine " + System.currentTimeMillis());
		
			client = DataExchangeAPIClientFactory.newClient("http://"+ControllerHost+":"+port+"/DataExchange/v1/Service.svc/", cb.build(), APIKEY);
			
	    if (fileName == null)
	      throw new IllegalArgumentException("RTSPTest: Filename must not be null");

	    if (serverHost == null)
	      throw new IllegalArgumentException("RTSPTest: Server host must not be null");

	    if (serverPort < -1 || serverPort > 65535)
	      throw new IllegalArgumentException("RTSPTest: Port must be between 0 and 65535");

	    // Figure out the server port
	    if (serverPort == -1) // default port
	      this.serverPort = 554;
	    else
	      this.serverPort = serverPort;

	    SourceName=name;
	    // Store host name
	    hostName = new String(serverHost);
	    // Store file name
	    videoFile = new String(fileName);

	    // Set to false then set up
	    isSetup = false;
	    setUpConnectionAndParameters();
	  }
	  public RTSPControl(String serverHost, int serverPort, String fileName,String name)
	  {
		cb=null;
		client= null;
	    if (fileName == null)
	      throw new IllegalArgumentException("RTSPTest: Filename must not be null");

	    if (serverHost == null)
	      throw new IllegalArgumentException("RTSPTest: Server host must not be null");

	    if (serverPort < -1 || serverPort > 65535)
	      throw new IllegalArgumentException("RTSPTest: Port must be between 0 and 65535");

	    // Figure out the server port
	    if (serverPort == -1) // default port
	      this.serverPort = 554;
	    else
	      this.serverPort = serverPort;
	    SourceName=name;
	    // Store host name
	    hostName = new String(serverHost);
	    // Store file name
	    videoFile = new String(fileName);

	    // Set to false then set up
	    isSetup = false;
	    setUpConnectionAndParameters();
	  }
	  
	  public RTSPControl(String rtspURL,String ControllerHost, String port,String Location,String APIKEY,String name) throws Exception {
			cb = new ContextBuilder();

			cb.hardware("RTSPControl").location(Location).software("RTSP")

				.script("RTSPengine " + System.currentTimeMillis());
			   try
			   {
				   if(APIKEY == null)
					   client = DataExchangeAPIClientFactory.newClient("http://"+ControllerHost+":"+port+"/DataExchange/v1/Service.svc/", cb.build());
				   else
					   client = DataExchangeAPIClientFactory.newClient("http://"+ControllerHost+":"+port+"/DataExchange/v1/Service.svc/", cb.build(), APIKEY);
			   }
			   catch(Exception e)
			   {
				   throw e;
			   }
				// Parse out just the URL by itself (and port if applicable)
		        String temp = "rtsp://";
		        SourceName=name;
		        int locOfRtsp = rtspURL.indexOf(temp);

		        // Throw exception if rtsp:// is not accompanied with the URL
		        if (locOfRtsp == -1)
		          throw new IllegalArgumentException("Must give URL that begins with rtsp://");

		        // Obtain a string excluding the "rtsp://" bit
		        String parsedURL = rtspURL.substring(locOfRtsp + temp.length());

		        // Extract the IP with the port address if possible
		        // You also need to make sure that there is a **video file** we need to play
		        // This means that there should be a slash that follows it.
		        int indexOfSlash = parsedURL.indexOf("/");
		        String hostnameTemp;
		        if (indexOfSlash != -1)
		          hostnameTemp = parsedURL.substring(0, indexOfSlash);
		        else
		          throw new IllegalArgumentException("RTSP URL must end with a slash (/)");

		        // Check to see if there is a port specified.  If there isn't,
		        // assume default part
		        int indexOfColon = hostnameTemp.indexOf(':');
		        if (indexOfColon != -1) {
		          // Get the IP / DNS without the ':'
		          this.hostName = hostnameTemp.substring(0, indexOfColon);

		          // Get the port number that is after the colon
		          try {
		            this.serverPort = Integer.parseInt(hostnameTemp.substring(hostnameTemp.indexOf(':') + 1));
		          } catch (NumberFormatException nfe) {
		            throw new IllegalArgumentException("Error: Port number is not a number");
		          }
		        }
		        else {
		          this.hostName = hostnameTemp;
		          this.serverPort = 554;
		        }

		        // Get the video file name now
		        // If none is provided, this is null
		        if (indexOfSlash + 1 > parsedURL.length())
		          videoFile = null;
		        else
		          videoFile = parsedURL.substring(indexOfSlash + 1);

		        if(videoFile.substring(videoFile.length()-1, videoFile.length()).equals("/"))
		        	videoFile=videoFile.substring(0,videoFile.length()-1);
		        
		        isSetup = false;
		        setUpConnectionAndParameters();
			
		  }
	  // Constructor if given string URL
	  public RTSPControl(String rtspURL,String name) {
		  client= null;
		  cb=null;
	    // Parse out just the URL by itself (and port if applicable)
	        String temp = "rtsp://";
	        int locOfRtsp = rtspURL.indexOf(temp);

	        // Throw exception if rtsp:// is not accompanied with the URL
	        if (locOfRtsp == -1)
	          throw new IllegalArgumentException("Must give URL that begins with rtsp://");

	        // Obtain a string excluding the "rtsp://" bit
	        String parsedURL = rtspURL.substring(locOfRtsp + temp.length());

	        // Extract the IP with the port address if possible
	        // You also need to make sure that there is a **video file** we need to play
	        // This means that there should be a slash that follows it.
	        int indexOfSlash = parsedURL.indexOf("/");
	        String hostnameTemp;
	        if (indexOfSlash != -1)
	          hostnameTemp = parsedURL.substring(0, indexOfSlash);
	        else
	          throw new IllegalArgumentException("RTSP URL must end with a slash (/)");

	        // Check to see if there is a port specified.  If there isn't,
	        // assume default part
	        int indexOfColon = hostnameTemp.indexOf(':');
	        SourceName=name;
	        if (indexOfColon != -1) {
	          // Get the IP / DNS without the ':'
	          this.hostName = hostnameTemp.substring(0, indexOfColon);

	          // Get the port number that is after the colon
	          try {
	            this.serverPort = Integer.parseInt(hostnameTemp.substring(hostnameTemp.indexOf(':') + 1));
	          } catch (NumberFormatException nfe) {
	            throw new IllegalArgumentException("Error: Port number is not a number");
	          }
	        }
	        else {
	          this.hostName = hostnameTemp;
	          this.serverPort = 554;
	        }

	        // Get the video file name now
	        // If none is provided, this is null
	        if (indexOfSlash + 1 > parsedURL.length())
	          videoFile = null;
	        else
	          videoFile = parsedURL.substring(indexOfSlash + 1);

	        if(videoFile.substring(videoFile.length()-1, videoFile.length()).equals("/"))
	        	videoFile=videoFile.substring(0,videoFile.length()-1);
	        
	    isSetup = false;
	    setUpConnectionAndParameters();
	  }

	  public String getRTSPURL() {
	    return new String(hostName);
	  }

	  public int getServerPort() {
	    return this.serverPort;
	  }

	  public String getVideoFilename() {
	    if (this.videoFile != null)
	      return new String(videoFile);
	    else
	      return null;
	  }


	  // Initialize TCP connection with server to exchange RTSP messages
	  private void setUpConnectionAndParameters() {
	    try {
	      //System.out.println("Establishing TCP Connection to: " + hostName + " at Port: " + serverPort);
	      InetAddress ServerIPAddr = InetAddress.getByName(hostName);
	      RTSPSocket = new Socket(ServerIPAddr, serverPort);
	      bufInStream=   RTSPSocket.getInputStream();
	      bufoutStream= RTSPSocket.getOutputStream();
	      
	    } catch (UnknownHostException e) {
	      System.out.println("Could not find host");
	      e.printStackTrace();
	    } catch (IOException e) {
	      System.out.println("Could not establish socket");
	      e.printStackTrace();
	    }

	    //System.out.println("Set up read buffer");
	      RTSPBufferedReader = new BufferedReader(new InputStreamReader(bufInStream));

	    //System.out.println("Set up write buffer");
	      RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(bufoutStream));

	    // Initialize state
	    state = RTSPState.INIT;

	    // Initialize buffer to capture enough bytes from the server
	    buf = new byte[300000];

	    // Initialize parameters
	    audioTrackID = -1;
	    videoTrackID = -1;
	    audioPT = -1;
	    videoPT = -1;
	    RTSPSessionID = null;
	    numberOfTracks = 0;
	    isSetup = true;
	    AudioRTPsocket = null;
	    VideoRTPsocket= null;
	    videoSetupDone = false;
	    videoSetupDone = false;
	    AudioRTCPsocket =null;
	    VideoRTCPsocket =null;
	    AudioRTCP_RCV_PORT =0;
	    VideoRTCP_RCV_PORT =0;
	    AudioServerRTCP_RCV_PORT =0;
	    VideoServerRTCP_RCV_PORT =0;
	    RTPClientAudioPort = 0;
	    RTPServerAudioPort = 0;
	    RTPClientvideoPort= 0;
	    RTPServervideoPort = 0;
	    
	    timerOptions = new Timer();
	    // Start an Options timer that incrementally sends a request every so often
	    // This prevents the server from disconnecting
	    // Delay by two seconds to ensure no conflicts in re-establishing connection
	    timerOptions.scheduleAtFixedRate(new RTSPOptionsTimerTask(), TIMEROPTIONSDELAY,
	        TIMEROPTIONSFREQUENCY);
	  }

	  // Integer code - 0 for no more setup calls required
	  // >= 1 - An additional setup is required
	  // -1 - Invalid server response
	  public String RTSPSetup() throws UnknownHostException,SocketException,InterruptedException,Exception{
		String LocaIp=null;
		StringBuilder outpustring =new StringBuilder();
		InetSocketAddress address;
		int responsecode;
		boolean AudioUDP=true;
		boolean VideoUDP=true;
		
	    if (state != RTSPState.INIT)
	    {
	      System.out.println("Client is already set up or is playing content");
	      outpustring.append("Client is already set up or is playing content");
	      return "0"; // We have already set up or are playing
	    }

	    if (numberOfTracks == 0)
	    {
	      System.out.println("No tracks to set up!");
	      outpustring.append("No tracks to set up!");
	      return "0";
	    }

	    // Cancel the options timer request while we do this
	    if (timerOptions != null)
	      timerOptions.cancel();

	    
	    
	    // Also establish our socket connection to the server
        if (AudioRTPsocket == null && VideoRTPsocket == null)
        {
          int[] ports = new int[2];
          LocaIp= NetworkInterfaceUtil.IsStringNullOrEmptyPredicate.getlocaladress();
           
          AudioRTPsocket=new DatagramSocket();
          RTPClientAudioPort=AudioRTPsocket.getLocalPort();
          AudioRTPsocket.close();
		
          AudioRTPsocket=new DatagramSocket(null);
          address = new InetSocketAddress(LocaIp, RTPClientAudioPort);
          AudioRTPsocket.bind(address);
		 
          if(LocaIp!=null)
          {
			  ports= GetAvailablePorts(AudioRTPsocket,LocaIp);
			  if(ports[1]!=0)
			  {
				RTPClientAudioPort=ports[0];
				AudioRTCP_RCV_PORT=ports[1];
			  }
			  //UDP socket for sending QoS RTCP packets
		      // Send SETUP message to server---audio
			  // Increment RTSP sequence number
			  RTSPSeqNb++;
	
		      outpustring.append(sendRTSPRequest(RTSPRequest.SETUP));
		      responsecode=parseServerResponse(outpustring);
		      // Wait for response code from server
		      if (responsecode != 200)
		      {
		    	  if(responsecode ==461)
		    	  {
		    		  audioSetupDone=false;
			    	  //-----the udptransport is not suported---
			    	  streamingProtocolAudio=streamingProtocolAudio+"/TCP";
			    	  streamingProtocolVideo=streamingProtocolVideo+"/TCP";
			    	  ISUDP=false;
			    	  AudioUDP=false;
			    	  AudioRTPsocket.close();
				      AudioRTPsocket.disconnect(); 
				      AudioRTPsocket=null;
			    	  // Increment RTSP sequence number
					  RTSPSeqNb++;
					  numberOfTracks++;
					  ForceAudioSession=true;
					 //---setup video---
					 outpustring.append(sendRTSPRequest(RTSPRequest.SETUP));
					 responsecode=parseServerResponse(outpustring);
		    	  }
		    	  else
		    	  {
				      System.out.println("Invalid Server Response");
				      outpustring.append("Invalid Server Response");
				      return outpustring.toString();
		    	  }
		     }
		     if (responsecode != 200)
		     {
		    	 System.out.println("Invalid Server Response");
			      outpustring.append("Invalid Server Response");
			      return outpustring.toString();
		     }
		    
		   	//----set the audio channel---------------
	    	if(RTPServerAudio ==null)
	    		RTPServerAudio=this.hostName;
		    	
		    	
		    		
				
			VideoRTPsocket=new DatagramSocket(null);
			address = new InetSocketAddress(LocaIp, RTPClientvideoPort);
			VideoRTPsocket.bind(address);
			ports= GetAvailablePorts(VideoRTPsocket,LocaIp);
			if(ports[1]!=0)
			 {
				 RTPClientvideoPort=ports[0];
				 VideoRTCP_RCV_PORT=ports[1];
			 }
		   
		 
		    
			 // Increment RTSP sequence number
			 RTSPSeqNb++;
			 //---setup video---
			 outpustring.append(sendRTSPRequest(RTSPRequest.SETUP));
			 responsecode=parseServerResponse(outpustring);
			 // Wait for response code from server
			 if ( responsecode != 200)
			 {
				 if(responsecode ==461)
			      {
					 videoSetupDone=false;
					 numberOfTracks++;
			    	  //-----the udptransport is not suported---
					 streamingProtocolVideo=streamingProtocolVideo+"/TCP";
					// Increment RTSP sequence number
					 ISUDP=false;
					 VideoUDP=false;
					 RTSPSeqNb++;
					 //---setup video---
					 outpustring.append(sendRTSPRequest(RTSPRequest.SETUP));
					 responsecode=parseServerResponse(outpustring);
			      }
				 else
				 {
					 System.out.println("Invalid Server Response");
					 outpustring.append("Invalid Server Response");
					 return outpustring.toString();
				 }
			 }
			 if (responsecode != 200)
			 {
		    	 System.out.println("Invalid Server Response");
			      outpustring.append("Invalid Server Response");
			      return outpustring.toString();
		     }
		    
			if(numberOfTracks>0 && applicationTrackID!=-1)
			{
				//-----send application setup request
				 RTSPSeqNb++;
				 //---setup video---
				 outpustring.append(sendRTSPRequest(RTSPRequest.SETUP));
				 responsecode=parseServerResponse(outpustring);
				 // Wait for response code from server
				 if ( responsecode != 200)
				 {
					 System.out.println("Invalid Server Response");
				      outpustring.append("Invalid Server Response");
				      return outpustring.toString();
				 }
				//---------------------------------------
			}
				
			 if(RTPServerVideo==null)
				 RTPServerVideo=this.hostName;
				 
			 if (numberOfTracks == 0)
			 {
				 if(!ISUDP)
				 {
					 if(!VideoUDP && AudioUDP)
					 {
						 audioSetupDone=false;
				    	  //-----the udptransport is not suported---
				    	  streamingProtocolAudio=streamingProtocolAudio+"/TCP";
				    	  streamingProtocolVideo=streamingProtocolVideo+"/TCP";
				    	  ISUDP=false;
				    	  AudioUDP=false;
				    	  AudioRTPsocket.close();
					      AudioRTPsocket.disconnect(); 
					      AudioRTPsocket=null;
				    	  // Increment RTSP sequence number
						  RTSPSeqNb++;
						  numberOfTracks++;
						  ForceAudioSession=true;
						 //---setup video---
						 outpustring.append(sendRTSPRequest(RTSPRequest.SETUP));
						 responsecode=parseServerResponse(outpustring);
					 }
				 }
				 // Change current RTSP state to READY
		        System.out.println("Client is ready");
		        outpustring.append("Client is ready\n");
		        state = RTSPState.READY;
		        iniRTPChannel(LocaIp);
			 }
			
	
          }
		     
        }
		return outpustring.toString();
	  }

	  private void iniRTPChannel(String LocaIp) throws Exception
	  {
		  if(!ISUDP)
		  {
		  	//-----TCP----- 
			  if(AudioRTPsocket!=null && VideoRTPsocket!=null)
			  {
				  AudioRTPsocket.close();
				  AudioRTPsocket.disconnect(); 
				  AudioRTPsocket=null;
				  VideoRTPsocket.close();
				  VideoRTPsocket.disconnect(); 
				  VideoRTPsocket=null;
			  }
			  rtp_stat=new RTP_STAT();
			  rtp_data=new RTP_DATA();
			  TCPRTP=new RTP_TCP(RTSPSocket, bufInStream,bufoutStream,SourceName,rtp_stat,rtp_data);
			
		  }
		  else
		  {
			  //----first setup the audio settings------
			  initsock(AudioRTPsocket, InetAddress.getByName(RTPServerAudio), RTPServerAudioPort);
		      audio_rtsp_stat=new RTPSTATS();
			  AudioPlayer= new RTPReceiver(RTPServerAudio, RTPServerAudioPort, RTPClientAudioPort,LocaIp,audio_rtsp_stat);
		      AudioRTPsocket.close();
		      AudioRTPsocket.disconnect(); 
		      AudioRTPsocket=null;
		    	 
		      
		      AudioPlayer.initialize();
			  //----------------------------------------
		  
		  
			  //----video settings--
			  initsock(VideoRTPsocket, InetAddress.getByName(RTPServerVideo), RTPServervideoPort);
		    	
			   video_rtsp_stat=new RTPSTATS();
			  
		        VideoPlayer = new RTPReceiver(RTPServerVideo, RTPServervideoPort, RTPClientvideoPort,LocaIp,video_rtsp_stat);
			    VideoRTPsocket.close();
			    VideoRTPsocket.disconnect(); 
			    VideoRTPsocket=null;
			    
			    Thread.sleep(1500);
			    VideoPlayer.initialize(); 
			    
			    // Restart Options timer
			    timerOptions = new Timer();
			    timerOptions.scheduleAtFixedRate(new RTSPOptionsTimerTask(), TIMEROPTIONSDELAY,
			    TIMEROPTIONSFREQUENCY);
			  //------------------
		  }
	  }
	  
	  private void initsock(DatagramSocket soc, InetAddress IPAddr,int receivedport) throws Exception
	  {
		  String test="Version 3";
		  	RTPpacket rtp_packet = new RTPpacket(0,0,0,test.getBytes(),46);
		    int packet_length = rtp_packet.getlength();
		    byte[] packet_bits = new byte[packet_length];
		    rtp_packet.getpacket(packet_bits);
		
		    DatagramPacket dp = new DatagramPacket(packet_bits, packet_length, IPAddr, receivedport);
		        soc.send(dp);
		   
	  }
	  
	  public String RTSPPlay() {
		  StringBuilder outputstring = new StringBuilder();
		  
	    if (state != RTSPState.READY) {
	      System.out.println("Client has not sent Setup Request yet");
	      outputstring.append("Client has not sent Setup Request yet");
	      return outputstring.toString();
	    }

	    // Cancel Options timer while we pull the response from the server
	    if (timerOptions != null)
	      timerOptions.cancel();

	    // Increase the RTSP sequence number - This is the
	    // next command to issue
	    RTSPSeqNb++;
	    
	 
	    // Send PLAY message to server
	    outputstring.append(sendRTSPRequest(RTSPRequest.PLAY));

	    // Wait for response
	    if (parseServerResponse(outputstring) != 200)
	    {
	      System.out.println("Invalid Server Response");
	      outputstring.append("Invalid Server Response");
	    }
	    else {
	      System.out.println("Starting playback - Starting Timer event");
	      outputstring.append("Starting playback - Starting Timer event");
	      // Set to play state
	      state = RTSPState.PLAYING;           
	      
	    }

	    // Restart Options timer event
	    // Even if we are playing content, we still need to keep our
	    // connection alive
	    if(ISUDP)
	    {
		    timerOptions = new Timer();
		    Rtp_statagregage=new Timer();
		    Rtp_aggregate=new Timer();
		    
		    if(KeepAlive_GetParameter)
		    	timerOptions.scheduleAtFixedRate(new RTSPGETPARAMERTimerTask(), TIMEROPTIONSDELAY,
		    			TIMEROPTIONSFREQUENCY);
		    else
		    	timerOptions.scheduleAtFixedRate(new RTSPOptionsTimerTask(), TIMEROPTIONSDELAY,
		    			TIMEROPTIONSFREQUENCY);
		    
		    Rtp_statagregage.scheduleAtFixedRate(new StatisticAggregator(audio_rtsp_stat, client), 3000, 3000);
		    Rtp_aggregate.scheduleAtFixedRate(new StatisticAggregator(video_rtsp_stat, client), 3100, 3100);
	    }
	    else
	    {
	    	 TCPRTP.UntervealedStartTimer();
	    	 
	    	 timerOptions = new Timer();
	    	 timerOptions.scheduleAtFixedRate(new RTSPGETPARAMERTimerTask(), TIMEROPTIONSDELAY,
		    			TIMEROPTIONSFREQUENCY);
	    	 
	    	 Rtp_aggregate=new Timer();
	    	 Rtp_aggregate.scheduleAtFixedRate(new Aggregator(rtp_data, client),3000,3000);
	    	 
	    	 Rtp_statagregage=new Timer();
	    	 Rtp_statagregage.scheduleAtFixedRate(new StatAgregate(rtp_stat, client), 3100, 3100);
	    }
	    return outputstring.toString();
	  }
	 
	  private int[] GetAvailablePorts(DatagramSocket tmpsock,String address ) 
	  {
		  int[] result = null;
		  int port1=0;
		  int port2=0;
		  InetSocketAddress add;
		  
		
		   while(port2==0)
			  {
			   	if( tmpsock==null)
			   	{
			   		try {
			   			tmpsock = new DatagramSocket(null);
			   		     add= new InetSocketAddress(address, port1);
			   		    tmpsock.bind(add);
			   		 	} catch (SocketException e) {
			   			
			   			// TODO Auto-generated catch block
			   			e.printStackTrace();
			   		}
			   	}
			     if(tmpsock!=null)
			     {
			    	 port1 =tmpsock.getLocalPort();		       
			    	 if(!NetworkUtils.isPortInUse(port1+1,address))
			    		 port2=port1+1;
			    	 else
			    	 {
			    		 tmpsock.close();
			    		 tmpsock.disconnect();
			    		 tmpsock=null;
			    		 port1=port1+2;
			    	 }
			     }
			  }
		    result = new int[2];
		    result[0]=port1;
		    result[1]=port2;
		  
		    return result;
	  }
	  public String RTSPPause() {
		  
		StringBuilder outputstring = new StringBuilder();
		
	    if (state != RTSPState.PLAYING) {
	    	
	      System.out.println("Client is not playing content right now");
	      outputstring.append("Client is not playing content right now");
	      return outputstring.toString();
	    }

	    // Increase the RTSP sequence number
	    RTSPSeqNb++;
	    
	   
	    
		   RTSPSeqNb++;
   	
		if(!ISUDP)
		{
		  	  TCPRTP.StopTimer();
		  	  Rtp_aggregate.cancel();
		  	  Rtp_statagregage.cancel();
		}
		else
		{
			  Rtp_aggregate.cancel();
		  	  Rtp_statagregage.cancel();
		}
	    // Send PAUSE message to server
	    outputstring.append(sendRTSPRequest(RTSPRequest.PAUSE));

	    // Wait for response
	    if (parseServerResponse(outputstring) != 200)
	    {
	      System.out.println("Invalid Server Response");
	      outputstring.append("Invalid Server Response");
	    }
	    else {
	      state = RTSPState.READY;
	      System.out.println("Pausing playback - Cancelling Timer event");
	      outputstring.append("Pausing playback - Cancelling Timer event");
	      
	   
	    }

	    // Restart Options timer while we are paused
	    // Notice that we don't cancel this timer as this was already done in
	    // PLAY
	    timerOptions = new Timer();
	    timerOptions.scheduleAtFixedRate(new RTSPGETPARAMERTimerTask(), TIMEROPTIONSDELAY,
	        TIMEROPTIONSFREQUENCY);
	    
	    return outputstring.toString();
	  }

	  public String RTSPTeardown() {
	    // You can only call TEARDOWN after the connections have been set up,
	    // or if you are playing content
		StringBuilder outputstring = new StringBuilder();
		
	    if (state == RTSPState.INIT) {
	      System.out.println("Client is in initialize stage - No need to teardown");
	      outputstring.append("Client is in initialize stage - No need to teardown");
	      return outputstring.toString();
	    }

	    // Increase RTSP Sequence number
	    if(!ISUDP)
	    {
	    	TCPRTP.StopTimer();
	    }
	    RTSPSeqNb++;
    	
	    // Send TEARDOWN message to the server
	    outputstring.append(sendRTSPRequest(RTSPRequest.TEARDOWN));

	    // Wait for server response
	    if (parseServerResponse(outputstring) != 200)
	    {
	      System.out.println("Invalid Server Response");
	      outputstring.append("Invalid Server Response");
	    }
	    else {
	      System.out.println("Teardown - Changing Client state back to INIT");
	      outputstring.append("Teardown - Changing Client state back to INIT");
	     
	      // Reset all parameters
	      resetParameters();
	    }
	    
	    return outputstring.toString();
	  }

	  public void resetParameters() {
	    state = RTSPState.INIT;

	    // Reset sequence number
	    RTSPSeqNb = 0;

	    // Reset all other parameters
	    audioTrackID = -1;
	    videoTrackID = -1;
	    audioPT = -1;
	    videoPT = -1;
	    RTSPSessionID = null;
	    numberOfTracks = 0;
	    isSetup = false;
	    buf = null;
	    videoSetupDone = false;
	    audioSetupDone = false;
	    AudioRTCP_RCV_PORT =0;
	    VideoRTCP_RCV_PORT =0;
	    AudioServerRTCP_RCV_PORT =0;
	    VideoServerRTCP_RCV_PORT =0;
	    RTPClientAudioPort = 0;
	    RTPServerAudioPort = 0;
	    RTPClientvideoPort= 0;
	    RTPServervideoPort = 0;
	    // Cancel all timing events if Teardown option is executed
	   
	    if (timerOptions != null) {
	      timerOptions.cancel();
	      timerOptions = null;
	    }
	   
	    // Close connection as well
	    if (AudioRTPsocket != null && !AudioRTPsocket.isClosed()) {
	    	AudioRTPsocket.close();
	    	AudioRTPsocket = null;
	    }
	    
	    if (VideoRTPsocket!= null && !VideoRTPsocket.isClosed()) {
	    	VideoRTPsocket.close();
	    	VideoRTPsocket = null;
	    }
	    
	    if (AudioRTCPsocket != null && !AudioRTCPsocket.isClosed()) {
	    	AudioRTCPsocket.close();
	    	AudioRTCPsocket = null;
		    }
	    
	    if (VideoRTCPsocket != null && !VideoRTCPsocket.isClosed()) {
	    	VideoRTCPsocket.close();
	    	VideoRTCPsocket = null;
		    }
	    
	    RTPServerAudio = null;
	    RTPServerVideo = null;
	    if(VideoPlayer!=null)
	    {
	    	VideoPlayer.close();
	    	VideoPlayer=null;
	    }
	   
	    
	    if(AudioPlayer!=null)
	    {
	    	AudioPlayer.close();
	    	AudioPlayer=null;
	    }
	   
	    ForceAudioSession=false;
	    if(!ISUDP)
	     {
	    	 
	    	  TCPRTP=null;
	    	  ISUDP=true;
	    	  rtp_data=null;
	    	  rtp_stat=null;
	    	  Rtp_statagregage.cancel();
	    	  Rtp_aggregate.cancel();
	    	  Rtp_aggregate=null;
	    	  Rtp_statagregage=null;
	    	  
	      }
	    else
	    {
	    	 Rtp_statagregage.cancel();
	    	 Rtp_aggregate.cancel();
	    	 Rtp_aggregate=null;
	    	 Rtp_statagregage=null;
	    	 audio_rtsp_stat=null;
	    	 video_rtsp_stat=null;
	   
	    }
	    try {
			RTSPBufferedReader.close();
			RTSPBufferedWriter.close();
			bufInStream.close();
			bufoutStream.close();
			RTSPSocket.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
	  }

	  // Send a request to see what the options are
	  public String RTSPOptions() {
	    // Remove because we would like to send an OPTIONS request no matter what
	    // state we are in
	    //if (state != RTSPState.INIT) {
	    //  System.out.println("Must be in INIT stage before requesting Options");
	    //  return;
	    //}
		StringBuilder outputstring = new StringBuilder();
	    if (!isSetup)
	      setUpConnectionAndParameters();

	    // Increase RTSP Sequence Number
	    RTSPSeqNb++;

	    // Send OPTIONS message to the server
	    outputstring.append(sendRTSPRequest(RTSPRequest.OPTIONS));

	    // Wait for server response
	    if (parseServerResponse(outputstring) != 200)
	    {
	      System.out.println("Invalid Server Response");
	      outputstring.append("Invalid Server Response");
	    }
	    else {
	      System.out.println("Options Request succeeded");
	      outputstring.append("Options Request succeeded");
	      // We don't need to change the state
	    }
	   
	    return outputstring.toString();
	  }
	  
	// Send a request to see what the options are
		  public String RTSPGET_PARAMETER() {
		    // Remove because we would like to send an OPTIONS request no matter what
		    // state we are in
		    //if (state != RTSPState.INIT) {
		    //  System.out.println("Must be in INIT stage before requesting Options");
		    //  return;
		    //}
			StringBuilder outputstring = new StringBuilder();
		    if (!isSetup)
		      setUpConnectionAndParameters();

		    // Increase RTSP Sequence Number
		    RTSPSeqNb++;

		    // Send OPTIONS message to the server
		    outputstring.append(sendRTSPRequest(RTSPRequest.GET_PARAMETER));

		    // Wait for server response
		    if (parseServerResponse(outputstring) != 200)
		    {
		      System.out.println("Invalid Server Response");
		      outputstring.append("Invalid Server Response");
		    }
		    else {
		      System.out.println("GET_PARAMETER Request succeeded");
		      outputstring.append("GET_PARAMETER Request succeeded");
		      // We don't need to change the state
		    }
		   
		    return outputstring.toString();
		  }
	  // Send a request for DESCRIBING what is available
	  public String RTSPDescribe() {
		StringBuilder outputstring=new StringBuilder();
	    if (state != RTSPState.INIT) {
	      System.out.println("Must be in INIT stage before requesting DESCRIBE");
	      outputstring.append("Must be in INIT stage before requesting DESCRIBE\n");
	      return outputstring.toString();
	    }

	    if (!isSetup)
	      setUpConnectionAndParameters();

	    // Cancel Options timer while we pull this information
	    if (timerOptions != null)
	      timerOptions.cancel();

	    // Increase RTSP Sequence Number
	    RTSPSeqNb++;

	    // Send OPTIONS message to the server
	    outputstring.append(sendRTSPRequest(RTSPRequest.DESCRIBE));

	    // Wait for server response
	    if (parseServerResponse(outputstring) != 200)
	    {
	      System.out.println("Invalid Server Response");
	       outputstring.append("Invalid Server Response\n");
	    }
	    else {
	      System.out.println("Describe Request succeeded");
	      outputstring.append("Describe Request succeeded\n");
	      // We don't need to change the state
	    }

	    if(packetpair)
	    {
	    	outputstring.append(RTSPGET_PARAMETER());
	    }
	    // Restart Options timer
	    timerOptions = new Timer();
	    timerOptions.scheduleAtFixedRate(new RTSPOptionsTimerTask(), TIMEROPTIONSDELAY,
	        TIMEROPTIONSFREQUENCY);
	    
	    return outputstring.toString();
	  }

	  // Goes through the received string from the server
	  // Also allows us to parse through and see if there is track
	  // information and the type of transport this server supports
	  public int parseServerResponse(StringBuilder out) {
	    int replyCode = 0;
	    int tmpport=0;
	    try {
	      // Read first line - Status line.  If all goes well
	      // this should give us: RTSP/1.0 200 OK
	      String statusLine = RTSPBufferedReader.readLine();

	      if (statusLine == null) {
	        System.out.println("Could not communiate with server");
	        out.append("Could not communiate with server");
	        return -1;
	      }

	      out.append(statusLine+"\n");
	      // Tokenize the string and grab the next token, which is
	      // the status code
	      StringTokenizer tokens = new StringTokenizer(statusLine, " \t\n\r\f");
	      String t=tokens.nextToken();
	      while(!t.equalsIgnoreCase("RTSP/1.0")&& tokens.hasMoreTokens())
	      {
	    	  t=tokens.nextToken();// Gives us RTSP/1.0
	    	  statusLine = RTSPBufferedReader.readLine();
	    	  tokens = new StringTokenizer(statusLine, " \t\n\r\f");
	    	  if(tokens.hasMoreTokens())
	    		  t=tokens.nextToken();
	      }
	      if(tokens.hasMoreTokens())
	      {
		   	  // Give us reply code
		      replyCode = Integer.parseInt(tokens.nextToken());
		      out.append("*** Reply Code: " + replyCode+"\n");
		      // If the reply code is 200, then we are solid
		      if (replyCode == 200)
		      { // begin if
		        // Cycle through the rest of the lines, delimited by \n
		        // and print to the screen.  Also grab the relevant information
		        // we need
		        //for (String line = RTSPBufferedReader.readLine(); line != null;
		        //    line = RTSPBufferedReader.readLine()) { // begin for
	
		        // NEW: First we need to wait to see if we are ready to read
		    	  String line;
		    	  try {
		    		  while (!RTSPBufferedReader.ready())
		    			  continue;
		    	  }
		    	  catch(IOException e) {
		    		  System.out.println("Could not read from read buffer");
		    		  //e.printStackTrace();
		    		  return -1;
		    	  }

	        // If we are, while there is still data in the buffer...
		    	  while (RTSPBufferedReader.ready()) { // begin for
		    		  line = RTSPBufferedReader.readLine();
		    		  out.append(line+"\n");
		    		  if (line == null) // Also check to see if we have reached the end
		    			  break;      // of the stream

		    		  System.out.println(line);
		          // Tokenize
		          // Also includes semi-colons
		          tokens = new StringTokenizer(line, " \t\n\r\f;:");

	          // Now go through each token
	          while (tokens.hasMoreTokens()) 
	          { // begin while1
	            // Grab token
	            String part = tokens.nextToken();

	            // When we are using DESCRIBE - We are looking for the
	            // track numbers - This will allow us to request
	            // that particular stream of data so we can pipe it
	            // into our decoder buffers
	            if( part.equals("Public"))
	            {
	            	
	            	while(tokens.hasMoreTokens())
	            	{
	            		 part = tokens.nextToken();
	            		StringTokenizer publicmethod = new StringTokenizer(part,",");
	            		while(publicmethod.hasMoreTokens())
	            		{
	            			String meth =publicmethod.nextToken();
	            			if(meth.equals("GET_PARAMETER"))
	            			{
	            				KeepAlive_GetParameter=true;
	            			}
	            		}
	            	}
	            
	            }
	            
	            if( part.equals("Supported:"))
	            {
	            	///---test if packedpair is supported----

	            	while(tokens.hasMoreTokens())
	            	{
	            		 part = tokens.nextToken();
	            		StringTokenizer publicmethod = new StringTokenizer(part,",");
	            		while(publicmethod.hasMoreTokens())
	            		{
	            			String meth =publicmethod.nextToken();
	            			if(meth.equals("com.microsoft.wm.packetpairssrc"))
	            			{
	            				packetpair=true;
	            			}
	            		}
	            	}
	            	//-------------------------------
	            }
	            if( part.equals("X-Playlist-Gen-Id"))
	            {
	            	///---Extract the X-Playlist-Gen-Id----
	            	XPlaylistGenId=Integer.parseInt(tokens.nextToken());
	            	//-------------------------------
	            }
	            
	            if( part.equals("X-Broadcast-Id"))
	            {
	            	///---Extract the X-Playlist-Gen-Id----
	            	XBroadcastid=Integer.parseInt(tokens.nextToken());
	            	//-------------------------------
	            }
	            // Usually looks like this:
	            // m=audio num RTP/AVP PT
	            // or
	            // m=video num RTP/AVP PT
	            // PT is the payload type or the type of media that is represented
	            // for the audio or video (i.e. audio: MP4, MP3, etc., video:
	            // H264, MPEG1, etc.)
	            if(part.equals("Transport"))
	            {
	            	
	            	while(tokens.hasMoreTokens())
	            	{
	            		part=tokens.nextToken();
			            if( part.substring(0, 6).equals("server"))
			            {
			            	StringTokenizer streamingPort = new StringTokenizer(part,"=");
			            	if(RTPServerAudioPort ==0)
			            	{
			            		
			            		if(streamingPort.hasMoreTokens())
			            		{
			            			streamingPort.nextToken();
			            			StringTokenizer ports=new StringTokenizer(streamingPort.nextToken(),"-");
			            			if(ports.hasMoreTokens())
			            			{
			            				RTPServerAudioPort=Integer.parseInt(ports.nextToken());
			            				if(ports.hasMoreTokens())
			            					AudioServerRTCP_RCV_PORT=Integer.parseInt(ports.nextToken());
			            			}
			            			else
			            			{
			            				RTPServerAudioPort=Integer.parseInt(streamingPort.nextToken());
			            			}
			            		}
			            	}
			            	else if(RTPServervideoPort ==0)
			            	{
			            		if(streamingPort.hasMoreTokens())
			            		{
			            			streamingPort.nextToken();
			            			StringTokenizer ports=new StringTokenizer(streamingPort.nextToken(),"-");
			            			if(ports.hasMoreTokens())
			            			{
			            				tmpport=Integer.parseInt(ports.nextToken());
			            				RTPServervideoPort=tmpport;
			            				if(ports.hasMoreTokens())
			            					VideoServerRTCP_RCV_PORT=Integer.parseInt(ports.nextToken());
			            			}
			            			else
			            				RTPServervideoPort=tmpport;
			            			
			            		}
			            	}
			          }
	            	  if( part.substring(0, 6).equals("source"))
			            {
			            	StringTokenizer ServerIP = new StringTokenizer(part,"=");
			            	if(RTPServerAudio ==null)
			            	{
			            		
			            		if(ServerIP.hasMoreTokens())
			            		{
			            			ServerIP.nextToken();
			            			RTPServerAudio=ServerIP.nextToken();
			            		}
			            	}
			            	else if(RTPServerVideo ==null)
			            	{
			            		if(ServerIP.hasMoreTokens())
			            		{
			            			ServerIP.nextToken();
			            			RTPServerVideo=ServerIP.nextToken();
			            		}
			            	}
			            }
			            	  
			            
			          
	            	}
	            }
	            
	            if(part.equals("a=range")&& StrStartTime== null & StrRangeMethod==null)
	            {
	            	  String range=tokens.nextToken();
	            	  StringTokenizer strarttime= new StringTokenizer(range,"=-");
	            	  StrRangeMethod=strarttime.nextToken();
	            	  StrStartTime=  strarttime.nextToken();
	            }
	            
	            if (part.equals("m=application") && applicationTrackID == -1) { // begin if2
	            		tokens.nextToken();

	  	              // Obtain the streaming protocol for the Audio
	  	              // Usually RTP/AVP
	            	  streamingProtocolapplication = new String(tokens.nextToken());

	            	  while (true) { // begin while2
		  	                line = RTSPBufferedReader.readLine();
		  	                System.out.println(line);
		  	                if (line.indexOf("a=rtpmap") != -1 || line == null)
		  	                  break;
		  	              } // e
	            	  
	            	  if (line == null) { // begin if3
		  	                System.out.println("Could not find a=rtpmap String");
		  	                return replyCode;
		  	              } 
	            	  
	            	  StringTokenizer controlTokens = new StringTokenizer(line, " \t\n\r\f;:");
	  	              // Skip over a=control
	            	  controlTokens.nextToken();
	            	  controlTokens.nextToken();
	  	              // This should now contain our trackID
	  	              String trackID = controlTokens.nextToken();
	  	            if (trackID.indexOf("x-asf-pf") != -1) {
	  	            	//----appplicaiton track to stream----
	  	            	 // Now advance each line until we hit "a=control"
		  	              while (true) { // begin while2
		  	                line = RTSPBufferedReader.readLine();
		  	                System.out.println(line);
		  	                if (line.indexOf("a=control") != -1 || line == null)
		  	                  break;
		  	              } // end while2

		  	              if (line == null) { // begin if3
		  	                System.out.println("Could not find a=control String");
		  	                return replyCode;
		  	              } // end if3

		  	              // Once we hit "a=control", get the track number
		  	              controlTokens = new StringTokenizer(line, " \t\n\r\f;:");
		  	              // Skip over a=control
		  	              controlTokens.nextToken();
		  	              // This should now contain our trackID
		  	              trackID = controlTokens.nextToken();

		  	              // Look for the key trackID or stream and adjust accordingly
		  	              if (trackID.indexOf("trackID") != -1) {
		  	                this.applicationTrackID = Integer.parseInt(trackID.substring(8, 9));
		  	                this.trackIDOrStream = true;
		  	                this.galbarmFlag = false;
		  	              }
		  	              ///// Fix thanks to galbarm
		  	              else if (trackID.indexOf("track") != -1) {
		  	                this.applicationTrackID = Integer.parseInt(trackID.substring((trackID.indexOf("track")+5),trackID.length()));// 5, here is length of track i.e, "track".length()
		  	                this.trackIDOrStream = true;
		  	                this.galbarmFlag = true;
		  	              }
		  	              else if (trackID.indexOf("stream") != -1) {
		  	                this.applicationTrackID = Integer.parseInt(trackID.substring(7, 8));
		  	                this.trackIDOrStream = false;
		  	              }
		  	             
		  	              System.out.println("*** Application Track: " + audioTrackID);
		  	              numberOfTracks++;

		  	              // Break out of this loop and continue reading the other lines
		  	              break;
	  	            	
	  	            	
	  	            }
	  	             
	            	
	            	
	            }
	            
	            if (part.equals("m=audio") && audioTrackID == -1) { // begin if2
	              // Skip next token
	              tokens.nextToken();

	              // Obtain the streaming protocol for the Audio
	              // Usually RTP/AVP
	              streamingProtocolAudio = new String(tokens.nextToken());

	              // Next token should contain our payload type
	              audioPT = Integer.parseInt(tokens.nextToken());
	              System.out.println("*** Audio PT: " + audioPT);

	              // Now advance each line until we hit "a=control"
	              while (true) { // begin while2
	                line = RTSPBufferedReader.readLine();
	                System.out.println(line);
	                if (line.indexOf("a=control") != -1 || line == null)
	                  break;
	              } // end while2

	              if (line == null) { // begin if3
	                System.out.println("Could not find a=control String");
	                return replyCode;
	              } // end if3

	              // Once we hit "a=control", get the track number
	              StringTokenizer controlTokens = new StringTokenizer(line, " \t\n\r\f;:");
	              // Skip over a=control
	              controlTokens.nextToken();
	              // This should now contain our trackID
	              String trackID = controlTokens.nextToken();

	              // Look for the key trackID or stream and adjust accordingly
	              if (trackID.indexOf("trackID") != -1) {
	                this.audioTrackID = Integer.parseInt(trackID.substring(8, 9));
	                this.trackIDOrStream = true;
	                this.galbarmFlag = false;
	              }
	              ///// Fix thanks to galbarm
	              else if (trackID.indexOf("track") != -1) {
	                this.audioTrackID = Integer.parseInt(trackID.substring((trackID.indexOf("track")+5),trackID.length()));// 5, here is length of track i.e, "track".length()
	                this.trackIDOrStream = true;
	                this.galbarmFlag = true;
	              }
	              else if (trackID.indexOf("stream") != -1) {
	                this.audioTrackID = Integer.parseInt(trackID.substring(7, 8));
	                this.trackIDOrStream = false;
	              }
	              else if (trackID.indexOf("audio") != -1) {
		                this.audioTrack ="audio";
		                this.audioTrackID=1;
		                this.trackIDOrStream = false;
		              }
	              System.out.println("*** Audio Track: " + audioTrackID);
	              numberOfTracks++;

	              // Break out of this loop and continue reading the other lines
	              break;
	            } // end if2
	            else if (part.equals("m=video") && videoTrackID == -1) { // begin if2
	              // Skip next token
	              tokens.nextToken();

	              // Obtain the streaming protocol for the Audio
	              // Usually RTP/AVP
	              streamingProtocolVideo = new String(tokens.nextToken());

	              // Next token should contain our payload type
	              videoPT = Integer.parseInt(tokens.nextToken());
	              System.out.println("*** Video PT: " + videoPT);

	              // Now advance each line until we hit "a=control"
	              while (true) { // begin while2
	                line = RTSPBufferedReader.readLine();
	                System.out.println(line);
	                if (line.indexOf("a=control") != -1 || line == null)
	                  break;
	              } // end while2

	              if (line == null) { // begin if3
	                System.out.println("Could not find a=control String");
	                return replyCode;
	              } // end if3

	              // Once we hit "a=control", get the track number
	              StringTokenizer controlTokens = new StringTokenizer(line, " \t\n\r\f;:");
	              // Skip over a=control
	              controlTokens.nextToken();
	              // This should now contain our trackID
	              String trackID = controlTokens.nextToken();

	              // Look for the key trackID or stream and adjust accordingly
	              if (trackID.indexOf("trackID") != -1) {
	                this.videoTrackID = Integer.parseInt(trackID.substring(8, 9));
	                this.galbarmFlag = false;
	                this.trackIDOrStream = true;
	              }
	              ///// Fix thanks to galbarm
	              else if (trackID.indexOf("track") != -1) {
	                this.videoTrackID = Integer.parseInt(trackID.substring((trackID.indexOf("track")+5),trackID.length()));// 5, here is length of track i.e, "track".length()
	                this.galbarmFlag = true;
	                this.trackIDOrStream = true;
	              }

	              else if (trackID.indexOf("stream") != -1) {
	                this.videoTrackID = Integer.parseInt(trackID.substring(7, 8));
	                this.trackIDOrStream = false;
	              }
	              else if (trackID.indexOf("video") != -1) {
		                this.VideoTrack ="video";
		                this.videoTrackID=1;
		                this.trackIDOrStream = false;
		              }
	              System.out.println("*** Video Track: " + videoTrackID);
	              numberOfTracks++;

	              // Break out of this loop and continue reading other lines
	              break;
	            } // end if2

	            // Extract the Session ID for subsequent setups
	            else if (part.equals("Session") && RTSPSessionID == null) { // begin if2
	              this.RTSPSessionID = tokens.nextToken();
	              System.out.println("*** Session ID: " + RTSPSessionID);
	              // Break out of this loop and continue reading other lines
	              break;
	            } // end if2
	          } // end while1
	        } // end for
	      } // end if
	      else
	      {
	    	  //----case of code different from 200
	    	  while(RTSPBufferedReader.ready())
	    		  RTSPBufferedReader.readLine();
	    	   //----------------------------  
	      }
	     }
	    } catch (IOException e) {
	      System.out.println("Could not read in string from buffer");
	      e.printStackTrace();
	    }
	    return replyCode;
	  }

	  public String sendRTSPRequest(RTSPRequest request) {
		  int temptranckid=-1;
		  // Set up base String
	    String requestType;
	    StringBuilder stringToSend = new StringBuilder();
	    StringBuilder outputString =new StringBuilder();
	    switch (request) {
	    case SETUP:
	      requestType = new String("SETUP");
	      break;
	    case DESCRIBE:
	      requestType = new String("DESCRIBE");
	      break;
	    case PLAY:
	      requestType = new String("PLAY");
	      break;
	    case TEARDOWN:
	      requestType = new String("TEARDOWN");
	      break;
	    case PAUSE:
	      requestType = new String("PAUSE");
	      break;
	    case OPTIONS:
	      requestType = new String("OPTIONS");
	      break;
	    case GET_PARAMETER:
		      requestType = new String("GET_PARAMETER");
		      break;
	    default:
	      throw new IllegalArgumentException("Invalid request type");
	    }

	    // This handles actual strings we are going to send to the server
	    switch(request) {
	    case SETUP:
	      if (videoSetupDone && audioSetupDone && applicationsetupdone)
	      {
	    	  System.out.println("Setup already established");
	    	  outputString.append("Setup already established\n");
	      }
	       
	      else {
	        try {
	        	//----reorder
	        	 if (audioTrackID != -1 && videoTrackID!=-1)
	        	 {
	        		 if(audioTrackID>videoTrackID)
	        		 {
	        			 temptranckid=audioTrackID;
	        			 audioTrackID=videoTrackID;
	        			 videoTrackID=temptranckid;
	        		 }
	        	 }
	        	   // set up the audio track
		           if (audioTrackID != -1 && !audioSetupDone)
		          {
		            System.out.println("*** Setting up Audio Track: ");
		            outputString.append("*** Setting up Audio Track: \n");
		            if (this.trackIDOrStream) {
		              if (videoFile != null) {
		                if (this.galbarmFlag)
		                  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
		                     "/" + videoFile + "/track=" + audioTrackID + " RTSP/1.0" + CRLF);
		                else
		                  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
		                     "/" + videoFile + "/trackID=" + audioTrackID + " RTSP/1.0" + CRLF);
		              }
		              else {
		                if (this.galbarmFlag)
		                  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
		                       "/track=" + audioTrackID + " RTSP/1.0" + CRLF);
		                else
		                  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
		                       "/trackID=" + audioTrackID + " RTSP/1.0" + CRLF);
		                }
		            }
		            else {
		              
		              if (videoFile != null)
		              {
		            	if(this.audioTrack !=null)
		            	{
		            		stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
				                    "/" + videoFile + "/" + this.audioTrack + " RTSP/1.0" + CRLF);
		            	}
		            	else
		            		stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
		                    "/" + videoFile + "/stream=" + audioTrackID + " RTSP/1.0" + CRLF);
		              }
		               else
		                stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
		                    "/stream=" + audioTrackID + " RTSP/1.0" + CRLF);

		            }

		            stringToSend.append("CSeq: " + RTSPSeqNb + CRLF);
		            stringToSend.append("User-Agent: NeoLoad/5.2.4 (Streaming Media v1.0.1)" + CRLF);
			         
		          /*  if (RTSPSessionID != null)
		              stringToSend.append("Session: " + RTSPSessionID + CRLF);*/
		            if(XPlaylistGenId!=0)
		            {
		            	stringToSend.append("X-Playlist-Gen-Id: " + XPlaylistGenId  + CRLF );
		            }
		            
		            
		            if(ISUDP)
		            	stringToSend.append("Transport: " + streamingProtocolAudio + ";unicast;client_port=" +
		            			RTPClientAudioPort + "-" + AudioRTCP_RCV_PORT + CRLF );
		            else
		            	stringToSend.append("Transport: " + streamingProtocolAudio + ";unicast;interleaved=2-3" + CRLF );
		            
		            if(ForceAudioSession)
		            {
		            	 if (RTSPSessionID != null)
				              stringToSend.append("Session: " + RTSPSessionID + CRLF);
		            }
		        
		            stringToSend.append( CRLF);
		            
		            RTSPBufferedWriter.write(stringToSend.toString());
		            System.out.println(stringToSend.toString());
		            outputString.append(stringToSend.toString());
		            RTSPBufferedWriter.flush();
		            numberOfTracks--;
		            audioSetupDone = true;
		          }	
	          // Set up video track if we haven't done it yet
		           else if (videoTrackID != -1 && !videoSetupDone) 
	          {
	            System.out.println("*** Setting up Video Track: ");
	            outputString.append("*** Setting up Video Track: \n");
	            if (this.trackIDOrStream) {
	              if (videoFile != null) {
	                //////// Fix thanks to galbarm
	                if (this.galbarmFlag)
	                  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
	                     "/" + videoFile + "/track=" + videoTrackID + " RTSP/1.0" + CRLF);
	                else
	                  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
	                     "/" + videoFile + "/trackID=" + videoTrackID + " RTSP/1.0" + CRLF);
	              }
	              else {
	                if (this.galbarmFlag)
	                  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
	                     "/track=" + videoTrackID + " RTSP/1.0" + CRLF);
	                else
	                  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
	                     "/trackID=" + videoTrackID + " RTSP/1.0" + CRLF);
	              }
	            }
	            else {
	              if (videoFile != null)
	              {
	            	  if(this.VideoTrack!=null)
    	            	  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
  	    	                    "/" + videoFile + "/" + this.VideoTrack + " RTSP/1.0" + CRLF);

	            		  else
    	            	  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
  	    	                    "/" + videoFile + "/stream=" + videoTrackID + " RTSP/1.0" + CRLF);
	              }            
	              else
	                stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
	                     "/stream=" + videoTrackID + " RTSP/1.0" + CRLF);
	            }

	            stringToSend.append("CSeq: " + RTSPSeqNb + CRLF);
	            stringToSend.append("User-Agent: NeoLoad/5.2.4 (Streaming Media v1.0.1)" + CRLF);
		         
	            if(XPlaylistGenId!=0)
	            {
	            	stringToSend.append("X-Playlist-Gen-Id: " + XPlaylistGenId  + CRLF );
	            }
	            
	            if(ISUDP)
	            	stringToSend.append("Transport: " + streamingProtocolVideo + ";unicast;client_port=" +
	            			RTPClientvideoPort + "-" + VideoRTCP_RCV_PORT + CRLF );
	            else
	            	stringToSend.append("Transport: " + streamingProtocolVideo + ";unicast;interleaved=0-1"  + CRLF );
	          
	            if (RTSPSessionID != null)
	              stringToSend.append("Session: " + RTSPSessionID + CRLF);

	            stringToSend.append( CRLF);
	            RTSPBufferedWriter.write(stringToSend.toString());
	            System.out.println(stringToSend.toString());
	            outputString.append(stringToSend.toString());
	            RTSPBufferedWriter.flush();
	            numberOfTracks--;
	            videoSetupDone = true;
	          }
		           else if (applicationTrackID != -1 && !applicationsetupdone) 
			          {
			            System.out.println("*** Setting up application Track: ");
			            outputString.append("*** Setting up application Track: \n");
			            if (this.trackIDOrStream) {
			              if (videoFile != null) {
			                //////// Fix thanks to galbarm
			                if (this.galbarmFlag)
			                  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
			                     "/" + videoFile + "/track=" + applicationTrackID + " RTSP/1.0" + CRLF);
			                else
			                  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
			                     "/" + videoFile + "/trackID=" + applicationTrackID + " RTSP/1.0" + CRLF);
			              }
			              else {
			                if (this.galbarmFlag)
			                  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
			                     "/track=" + applicationTrackID + " RTSP/1.0" + CRLF);
			                else
			                  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
			                     "/trackID=" + applicationTrackID + " RTSP/1.0" + CRLF);
			              }
			            }
			            else {
			              if (videoFile != null)
			              {
			            	 
		    	            	  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
		  	    	                    "/" + videoFile + "/stream=" + applicationTrackID + " RTSP/1.0" + CRLF);
			              }            
			              else
			                stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
			                     "/stream=" + videoTrackID + " RTSP/1.0" + CRLF);
			            }

			            stringToSend.append("CSeq: " + RTSPSeqNb + CRLF);
			            
			            if(XPlaylistGenId!=0)
			            {
			            	stringToSend.append("X-Playlist-Gen-Id: " + XPlaylistGenId  + CRLF );
			            }
			            stringToSend.append("User-Agent: NeoLoad/5.2.4 (Streaming Media v1.0.1)" + CRLF);
				         
			            if(ISUDP)
			            	stringToSend.append("Transport: " + streamingProtocolVideo + ";unicast;client_port=" +
			            			RTPClientvideoPort + "-" + VideoRTCP_RCV_PORT + CRLF );
			            else
			            	stringToSend.append("Transport: " + streamingProtocolVideo + ";unicast;interleaved=4-5"  + CRLF );
			          
			            if (RTSPSessionID != null)
			              stringToSend.append("Session: " + RTSPSessionID + CRLF);

			            stringToSend.append( CRLF);
			            RTSPBufferedWriter.write(stringToSend.toString());
			            System.out.println(stringToSend.toString());
			            outputString.append(stringToSend.toString());
			            RTSPBufferedWriter.flush();
			            numberOfTracks--;
			            applicationsetupdone = true;
			          }
	       
	        } catch(IOException e) {
	          System.out.println("Could not write to write buffer");
	          e.printStackTrace();
	        }
	      }
	      break;
	    case DESCRIBE: // Make sure we call DESCRIBE first
	    	try {
	 	        if (videoFile != null)
	 	          stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
	 	              "/" + videoFile  + " RTSP/1.0" + CRLF);
	 	        else
	 	          stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
	 	              " RTSP/1.0" + CRLF);

	 	        
	 	        // Send sequence number
	 	        // If there is no session ID, this is the last thing we send
	 	        if (RTSPSessionID == null)
	 	          stringToSend.append("CSeq: " + RTSPSeqNb + CRLF );	        	
	 	        // Send session number if applicable
	 	        else {
	 	          stringToSend.append("CSeq: " + RTSPSeqNb + CRLF);
	 	          stringToSend.append("Session: " + RTSPSessionID + CRLF );
	 	        }
	 	       stringToSend.append("User-Agent: NeoLoad/5.2.4 (Streaming Media v1.0.1)" + CRLF);
		         
	 	       stringToSend.append( CRLF);
	 	        RTSPBufferedWriter.write(stringToSend.toString());
	 	        System.out.println(stringToSend.toString());
	 	        outputString.append(stringToSend.toString());
	 	        RTSPBufferedWriter.flush();
	    	  }
		      catch (IOException e) {
		        System.out.println("Could not write to write buffer");
		        e.printStackTrace();
		      }
		      break;
	    case PLAY: // Case when we wish to issue a PLAY request
	    	 try
	    	 {
	    		 if (videoFile != null)
	    		 {
	    			 	  stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
		 	              "/" + videoFile + "/ RTSP/1.0" + CRLF);
	    			 
	    		 }
		 	          else
		 	          stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
		 	              "/ RTSP/1.0" + CRLF);

	            

	            stringToSend.append("CSeq: " + RTSPSeqNb + CRLF);
	            stringToSend.append("User-Agent: NeoLoad/5.2.4 (Streaming Media v1.0.1)" + CRLF);
	            if (RTSPSessionID != null)
	            	stringToSend.append("Session: " + RTSPSessionID + CRLF);
	            
	            if(XPlaylistGenId!=0)
	            {
	            	stringToSend.append("X-Playlist-Seek-Id: " + XPlaylistGenId  + CRLF );
	            }
	            
	            if(StrStartTime.compareToIgnoreCase("0")==0)
	            	StrStartTime="0.000";
	            
	            if(StrRangeMethod!=null && StrStartTime!=null)
	            {
	            	
	            	stringToSend.append("Range: " + StrRangeMethod + "="+ StrStartTime  + "-"+ CRLF);
	            }
	            
	            
	            stringToSend.append( CRLF);
	            RTSPBufferedWriter.write(stringToSend.toString());
	 	        System.out.println(stringToSend.toString());
	 	        outputString.append(stringToSend.toString());
	 	        RTSPBufferedWriter.flush();
	    	 }
	    	   catch (IOException e) {
			        System.out.println("Could not write to write buffer");
			        e.printStackTrace();
			      }
	    break;
	    case PAUSE: // Same for PAUSE
	    case TEARDOWN: // Also same for TEARDOWN
	    case GET_PARAMETER: // Also same for TEARDOWN
	    	 try {
	 	        if (videoFile != null)
	 	          stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
	 	              "/" + videoFile + "/" + " RTSP/1.0" + CRLF);
	 	        else
	 	          stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
	 	              "/ RTSP/1.0" + CRLF);

	 	        // Send sequence number
	 	        // If there is no session ID, this is the last thing we send
	 	        if (RTSPSessionID == null)
	 	          stringToSend.append("CSeq: " + RTSPSeqNb + CRLF );
	 	        // Send session number if applicable
	 	        else {
	 	          stringToSend.append("CSeq: " + RTSPSeqNb + CRLF);
	 	          stringToSend.append("Session: " + RTSPSessionID + CRLF );
	 	          
	 	        }
	 	       stringToSend.append("User-Agent: NeoLoad/5.2.4 (Streaming Media v1.0.1)" + CRLF);
		         
	 	        if(packetpair)
	 	        {
	 	        	 stringToSend.append("Content-Length: 0" + CRLF);
	 	        	 stringToSend.append("Content-Type: application/x-rtsp-packetpair"+ CRLF );
	 	        	 packetpair=false;
	 	        }
	 	       stringToSend.append( CRLF);
	 	        RTSPBufferedWriter.write(stringToSend.toString());
	 	        System.out.println(stringToSend.toString());
	 	        outputString.append(stringToSend.toString());
	 	        RTSPBufferedWriter.flush();
	    	  }
		      catch (IOException e) {
		        System.out.println("Could not write to write buffer");
		        e.printStackTrace();
		      }
		      break;
	    case OPTIONS: // Also same for OPTIONS
	      try {
	        if (videoFile != null)
	          stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
	              "/" + videoFile + "/" +" RTSP/1.0" + CRLF);
	        else
	          stringToSend.append(requestType + " rtsp://" + hostName + ":" + serverPort +
	              "/ RTSP/1.0" + CRLF);

	        // Send sequence number
	        // If there is no session ID, this is the last thing we send
	        if (RTSPSessionID == null)
	          stringToSend.append("CSeq: " + RTSPSeqNb + CRLF );
	        // Send session number if applicable
	        else {
	          stringToSend.append("CSeq: " + RTSPSeqNb + CRLF);
	          stringToSend.append("Session: " + RTSPSessionID + CRLF );
	        }
	        stringToSend.append("User-Agent: NeoLoad/5.2.4 (Streaming Media v1.0.1)" + CRLF);
	         
	        stringToSend.append( CRLF);
	        RTSPBufferedWriter.write(stringToSend.toString());
	        System.out.println(stringToSend.toString());
	        outputString.append(stringToSend.toString());
	        RTSPBufferedWriter.flush();
	      }
	      catch (IOException e) {
	        System.out.println("Could not write to write buffer");
	        e.printStackTrace();
	      }
	      break;
	    default:
	      throw new RuntimeException("Invalid Client State");
	    }
	    
	    return outputString.toString();
	  }

	  // This timer task gets invoked every so often to ensure that the connection
	  // is still alive and doesn't time out
	  private class RTSPOptionsTimerTask extends TimerTask {
	    @Override
	    public void run() {
	      RTSPOptions();
	    }
	  }
	  private class RTSPGETPARAMERTimerTask extends TimerTask {
		    @Override
		    public void run() {
		      RTSPGET_PARAMETER();
		    }
		  }
	  
	  public class RTCPSender extends TimerTask{
	

	      // Stats variables
	      private int numPktsExpected;    // Number of RTP packets expected since the last RTCP packet
	      private int numPktsLost;        // Number of RTP packets lost since the last RTCP packet
	      private int lastHighSeqNb;      // The last highest Seq number received
	      private int lastCumLost;        // The last cumulative packets lost
	      private float lastFractionLost; // The last fraction lost
	      InetAddress ServerIPAddr ;
	      DatagramSocket sock;
	      int receivedport;
	      
	      public RTCPSender(InetAddress IPAddr,DatagramSocket s,int intreceivedport) {
	            this.ServerIPAddr=IPAddr;
	            this.sock=s;
	            this.receivedport= intreceivedport;
	        }
	      
	      
		 public void run() {
		    // Calculate the stats for this period
		    numPktsExpected = statHighSeqNb - lastHighSeqNb;
		    numPktsLost = statCumLost - lastCumLost;
		    lastFractionLost = numPktsExpected == 0 ? 0f : (float)numPktsLost / numPktsExpected;
		    lastHighSeqNb = statHighSeqNb;
		    lastCumLost = statCumLost;
		
		    //To test lost feedback on lost packets
		    // lastFractionLost = randomGenerator.nextInt(10)/10.0f;
		
		    RTCPpacket rtcp_packet = new RTCPpacket(lastFractionLost, statCumLost, statHighSeqNb, (int)new Date().getTime());
		    int packet_length = rtcp_packet.getlength();
		    byte[] packet_bits = new byte[packet_length];
		    rtcp_packet.getpacket(packet_bits);
		
		    try {
	        DatagramPacket dp = new DatagramPacket(packet_bits, packet_length, ServerIPAddr, receivedport);
		        sock.send(dp);
		    } catch (InterruptedIOException iioe) {
		        System.out.println("Nothing to read");
		    } catch (IOException ioe) {
		        System.out.println("Exception caught: "+ioe);
		    }
		 }

	}
	

	  // Task that gets invoked after every 20 msec
	  private class RTSPTimerTask extends TimerTask {
		  DataExchangeAPIClient client;
		  EntryBuilder entry;
		  private int numPktsExpected;    // Number of RTP packets expected since the last RTCP packet
	      private int numPktsLost;        // Number of RTP packets lost since the last RTCP packet
	      private int lastHighSeqNb;      // The last highest Seq number received
	      private int lastCumLost;        // The last cumulative packets lost
	      private float lastFractionLost; // The last fraction lost
	      DatagramSocket sock;
	      
	    public RTSPTimerTask(DataExchangeAPIClient client,DatagramSocket s) {
			// TODO Auto-generated constructor stub
	    	this.client=client;
	    	this.sock=s;
		}

	    private void CreateEntry(String Cat,String metricname,double value,String unit) throws GeneralSecurityException, IOException, URISyntaxException, NeotysAPIException
	    {
	      	entry=new EntryBuilder(Arrays.asList("RTSP", Cat, metricname), System.currentTimeMillis());
	    	entry.unit(unit);
	    	entry.value(value);
	    	client.addEntry(entry.build());
	    }
		@Override
	    // Every 20 seconds, read from the socket connection
	    public void run() {
	      // Construct a DatagramPacket
	      rcvdp = new DatagramPacket(buf, buf.length);
	     
         
	      try {
	        // Receive the data
	        if (!sock.isClosed()) {
	        	sock.receive(rcvdp);

	          double curTime = System.currentTimeMillis();
              statTotalPlayTime += curTime - statStartTime; 
              statStartTime = curTime;

              //create an RTPpacket object from the DP
              RTPpacket rtp_packet = new RTPpacket(rcvdp.getData(), rcvdp.getLength());
              int seqNb = rtp_packet.getsequencenumber();

            //this is the highest seq num received
              CreateEntry("Network","Payloadtype",(double)rtp_packet.getpayloadtype(),"byte");
              
              //print important header fields of the RTP packet received: 
              System.out.println("Got RTP packet with SeqNum # " + seqNb
                                 + " TimeStamp " + rtp_packet.gettimestamp() + " ms, of type "
                                 + rtp_packet.getpayloadtype());

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

	          	         
	        }
	      } catch (SocketException se) { // We need to catch here if we decide to invoke TEARDOWN
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
		
		//------------------------------------
	    //Handler for timer
	    //------------------------------------
	    

	  }
	  
	  
	  
	
	  // Convenience method to convert a byte array into a string
	  // Source: http://stackoverflow.com/a/9855338/3250829
	  // Tests shown to be the fastest conversion routine available
	  // beating all other built-in ones in the Java API

	  // For our byte to hex conversion routine
	  final char[] hexArray = "0123456789ABCDEF".toCharArray();

	 
	  private String bytesToHex(byte[] bytes, int length) {
	      char[] hexChars = new char[length * 2];
	      for ( int j = 0; j < length; j++ ) {
	          int v = bytes[j] & 0xFF;
	          hexChars[j * 2] = hexArray[v >>> 4];
	          hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	      }
	      return new String(hexChars);
	  }

	  //private String bytesToHex(byte[] bytes) {
	  //  return bytesToHex(bytes, bytes.length);
	  //}

}
