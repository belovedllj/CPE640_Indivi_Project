// warning, test computer can not use "ipv4" and "ipv6" connection together, it will cause error
/*----------------------------------------------------------------
 *  Author:        Peng Peng
 *  Written:       10/14/2013
 *  Last updated:  10/14/2013
 
 *  This application can be used for either servers or clients without confusion
 *  Execute the program several. multiple servers, multiple clients
 *  
 *   !!I don't maintain a list of observers, instead I maintain a list store all outputStream to 
 *  connected clients, clients can add or remove themselves form the list anytime, and server 
 *  always make right reaction to them.
 *  
 *  advanced properties : (1) Allowing client to reconnect
 *                        (2) Server side can get the information about client's disconnection
 *                            and server will minus 1 connected client at server's status
 *                            PS: Even client disconnected by directly close its UI, server will 
 *                            get the information and do the right reactions. 
 *                        (3) Vice versa, if server closed it UI or clicked "disconnect button", all clients will get 
 *                            information and do right reactions.                         
 *                        (4) For client side if clicked "connect button", but server is off
 *                            this connect is invalid  you don't need to click "disconnect button"
 *                            to disconnect
 *                        (5) Each client shows on server side has a different name number
 *  Using multithreading programming to handle Server's multiple accepting
 *  and Client's connection
 *----------------------------------------------------------------*/
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.List;

import javax.swing.*;


public class MultiClientsChatApp extends JFrame {

	// List for storing all OutputStreams to clients
	private List<DataOutputStream> allDataOutputStreamInServer = Collections.synchronizedList(new ArrayList<DataOutputStream>());
	private ServerSocket serverSocket;
	private Socket socketInClient;
	private int counterInServerForClientsNumber;
	private int sequenceClientNumber = 0;
	
	private JRadioButton hostTheChat = new JRadioButton("Host the Chat");
	private JRadioButton joinTheChat = new JRadioButton("Join the Chat");
	private JTextField IPAddress = new JTextField(22);
	private JTextField portNumber = new JTextField(10);
	private JButton connectStartButton = new JButton("Connect/Start");
	private JButton disconnectButton = new JButton("Disconnect");
	private JLabel status = new JLabel("Status: Disconnected");
	private JTextArea chatWindow = new JTextArea();
	private JTextField chatSendLine = new JTextField();
	private boolean isServerConnected = false; /////////////
	private boolean isHosted = true;
	private boolean AsServerAlreadyConnected = false;
	private boolean AsClientAlreadyConnected = false;	
	private DataOutputStream OutputStreamInClient;
	private DataInputStream InputStreamInClient;	
	private Thread ThreadHandlingInClient;
	
	public MultiClientsChatApp() {
		chatWindow.setLineWrap(true);
		chatWindow.setWrapStyleWord(true);
		chatWindow.setEditable(false);    
		// chatWindow only display receiving messages, could not be directly edited.
		ButtonGroup group = new ButtonGroup();
		group.add(hostTheChat);
		group.add(joinTheChat);
		hostTheChat.setSelected(true);
		JPanel northPanelWithButtons1 = new JPanel();
		JPanel northPanelWithButtons2 = new JPanel();
		northPanelWithButtons1.setLayout(new FlowLayout(FlowLayout.LEFT));
		northPanelWithButtons1.add(hostTheChat);
		northPanelWithButtons1.add(joinTheChat);
		northPanelWithButtons1.add(IPAddress);
		northPanelWithButtons2.setLayout(new FlowLayout(FlowLayout.LEFT));
		northPanelWithButtons2.add(portNumber);
		northPanelWithButtons2.add(connectStartButton);
		northPanelWithButtons2.add(disconnectButton);
		northPanelWithButtons2.add(status);
		JPanel allNorthButtons = new JPanel(new GridLayout(2, 1));
		allNorthButtons.add(northPanelWithButtons1);
		allNorthButtons.add(northPanelWithButtons2);
		setLayout(new BorderLayout());
		add(allNorthButtons, BorderLayout.NORTH);
		add(new JScrollPane(chatWindow), BorderLayout.CENTER);
		add(chatSendLine,BorderLayout.SOUTH);
		setTitle("Simple TCP Chat Application");
		setSize(500, 500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		
		connectStartButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				// server host has not been established and has not connected to server as a client
				if(!isServerConnected && !AsClientAlreadyConnected) {   
					if(isHosted) {  // if host the chat
						isServerConnected = true;
						try {
							performServerConnection();
						} catch (UnknownHostException e1) {
							e1.printStackTrace();
						}
					}
					else if(!isHosted) {
						performClientConnection();
					}
				}								
			}			
		});
		disconnectButton.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				if(isServerConnected) { //only used when server has been established
					isServerConnected = false;
					try {
						cancelServerConnection();
					} catch (IOException e1) {
						e1.printStackTrace();
					}						
				}
				if(AsClientAlreadyConnected) { // only when client has connected to server
					AsClientAlreadyConnected = false;
					try {
						cancelClientConnection();
					} catch (IOException e1) {
						e1.printStackTrace();
					}					
				}
			}			
		});
		hostTheChat.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {			
				if(!isHosted) {
					isHosted = true;
				}
			}			
		});
		joinTheChat.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {			
				if(isHosted) {
					isHosted = false;
				}
			}			
		});
		IPAddress.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				if(!AsClientAlreadyConnected) {  // connection has not been established
					if(!isHosted) {
						performClientConnection();
					}
				}
			}
		});
		chatSendLine.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e){
				String message = chatSendLine.getText();
				chatSendLine.setText("");
				chatWindow.append("Me: " + message + "\n");
				if (AsServerAlreadyConnected) {
					try {
						synchronized (allDataOutputStreamInServer) {
							Iterator<DataOutputStream> it = allDataOutputStreamInServer.iterator();
							while (it.hasNext()) {
								DataOutputStream ouputStreamForEachClient = it.next();
								ouputStreamForEachClient.writeUTF(message); 
								//send message to each client
								ouputStreamForEachClient.flush();
								// clean up the Output stream
							}
						}
					} catch (IOException e1) {					
						e1.printStackTrace();
					}
				}	
				else if (AsClientAlreadyConnected) {
					try {
						OutputStreamInClient.writeUTF(message); //send message to server
						OutputStreamInClient.flush(); // clean up the Output stream
					} catch (IOException e1) {
						e1.printStackTrace();
					} 
				}
			}
		});
	}
	
	public static void main(String[] args) {
		new MultiClientsChatApp();
	}
	
	private void performServerConnection() throws UnknownHostException {
		counterInServerForClientsNumber = 0;            // Initialize counter
		status.setText(counterInServerForClientsNumber +" client(s) connected");
		InetAddress objAddr=InetAddress.getLocalHost(); // get local IP address
		String hostAddress=objAddr.getHostAddress();
		byte[] bAddr=objAddr.getAddress();  
		if(bAddr.length==4)
			System.out.println("IP address version is IPv4");
		else if(bAddr.length==16)
			System.out.println("IP address version is IPv6");
		int port;                                   // Initialize port number
		if (portNumber.getText().equals("")) {
			port = 8001;                            // default port number
		}				
		else {
			port = Integer.parseInt(portNumber.getText());
		}
		chatWindow.append("Host the chat at IP:" + hostAddress + " Port at: " + port + "\n");
		IPAddress.setText(hostAddress);	  // display local IP address automatically
		
		serverSocket = null;	
		try {					
			serverSocket = new ServerSocket(port);		
			chatWindow.append("Server started at " + new Date() + "\n");
			socketAcceptThreadAsServer accept = new socketAcceptThreadAsServer();
			new Thread(accept).start();
			// start a new thread to handle waiting due to no letting button stuck at pressed
		}
		catch(IOException ex) {
			System.err.println(ex);
		}
	}
	private void performClientConnection() {		
		serverSocket = null;
		socketInClient = null;		
		socketAcceptThreadAsClient clientThread = new socketAcceptThreadAsClient();
		ThreadHandlingInClient = new Thread(clientThread);
		ThreadHandlingInClient.start();
		// start a new thread to handle waiting due to no letting button stuck at pressed
	}
	
	private void cancelServerConnection() throws IOException {
		synchronized (allDataOutputStreamInServer) {
			Iterator<DataOutputStream> it = allDataOutputStreamInServer.iterator();
			while (it.hasNext()) {
				DataOutputStream ouputStreamForEachClient = it.next();
				ouputStreamForEachClient.writeUTF("#$Closed");
			    //System.out.println("I told" + allDataOutputStreamInServer.size() + "to close");
				// send "close" message to each client
				ouputStreamForEachClient.flush();
				// clean up the Output stream
			}
		}
		allDataOutputStreamInServer.clear();   // clear all saved DataOutputStream
		counterInServerForClientsNumber = 0;		 
		isServerConnected = false;
		AsServerAlreadyConnected = false;   //couldn't send message to others now		
		serverSocket.close();
		serverSocket = null;		
		status.setText("Disconnect");	
	}
	private void cancelClientConnection() throws IOException {
		//OutputStreamInClient.writeUTF("#$Closed");  // tell server client is off
		OutputStreamInClient.flush();		
		AsClientAlreadyConnected = false;  // couldn't send message to others
		InputStreamInClient = null;
		OutputStreamInClient = null;
		socketInClient.close();
		socketInClient = null;
		status.setText("Status: Disconnected");
		ThreadHandlingInClient.interrupt(); // stop the socket thread on client 
	}
	
	
	
	private class socketAcceptThreadAsServer implements Runnable {
		@Override
		public void run(){	
			while (true) {  // using a loop for receiving the multiple client to reconnect
				try {
					Socket eachSocket = serverSocket.accept(); //wait for client's connection
					AsServerAlreadyConnected = true; //Server can sent message to client					
					counterInServerForClientsNumber++;
					sequenceClientNumber++;
					chatWindow.append("One client C" + sequenceClientNumber + " has connected the server\n");
					status.setText(counterInServerForClientsNumber +" client(s) connected"); 
					// change status automatically
					HandleAClient task = new HandleAClient(eachSocket);
					new Thread(task).start();									
				} catch (IOException e) {
					e.printStackTrace();
				}
			}							
		}		
	}
	
	private class socketAcceptThreadAsClient implements Runnable {
		@Override
		public void run() {
			String hostAddress = IPAddress.getText().trim();  // delete the spaces
			int port = Integer.parseInt(portNumber.getText());
			chatWindow.append("Try to join the chat at IP: " + hostAddress + " Port at: " + port + "\n");
			try {	
				socketInClient = new Socket(hostAddress, port);
				// if Server and Client at same computer, we can use next statement instead
				//socketInClient = new Socket("localhost", port); 
				AsClientAlreadyConnected = true;
				chatWindow.append("Successfully connected with server\n");
				status.setText("Status: Connected");
				InputStreamInClient = new DataInputStream(socketInClient.getInputStream());
				OutputStreamInClient = new DataOutputStream(socketInClient.getOutputStream());
				while (true) {
					String inputChat = InputStreamInClient.readUTF();  // read message from client
					if (inputChat.equals("#$Closed")){ // server tell client I am close
						status.setText("Status: Disconnected"); // Change client's status
						chatWindow.append("Server is offline.\n");
						AsClientAlreadyConnected = false;  // couldn't send message to others						
						break;    // finish the thread
					}
					else {
						chatWindow.append("Server: " + inputChat + "\n");
					}
					
				}
			} catch(SocketException ex1) {  // Server close its UI to turn off server
				status.setText("Status: Disconnected"); // Change client's status
				AsClientAlreadyConnected = false;  // couldn't send message to others
			} catch (IOException ex) {
				System.err.println(ex);
			}			
		}		
	}
	
	private class HandleAClient implements Runnable {
		private Socket eachSocket;
		private DataOutputStream OuputStreamInServer;
		public HandleAClient(Socket eachSocket) {
			this.eachSocket = eachSocket;
		}
		@Override
		public void run() {	
			int mySequence = sequenceClientNumber;
			try {
				DataInputStream InputStreamInServer = new DataInputStream(eachSocket.getInputStream());
				OuputStreamInServer = new DataOutputStream(eachSocket.getOutputStream());
				allDataOutputStreamInServer.add(OuputStreamInServer);
				while (true) {
					String inputChat = InputStreamInServer.readUTF();  // read message from client
					chatWindow.append("C"+ mySequence + ": " + inputChat + "\n");					
				}
			} catch(SocketException ex1) {  // one client close UI to disconnect
				chatWindow.append("C"+ mySequence + " has left the server.\n");
				if (--counterInServerForClientsNumber == 0) {
					AsServerAlreadyConnected = false; // no client has connected
				}
				status.setText(counterInServerForClientsNumber +" client(s) connected");  // Change server's status
				allDataOutputStreamInServer.remove(OuputStreamInServer); // remove outputStream for this client from List
			} catch(EOFException ex2) {    // one client clicked "disconnect button"
				chatWindow.append("C"+ mySequence + " has left the server.\n");
				if (--counterInServerForClientsNumber == 0) {   
					AsServerAlreadyConnected = false; // no client has connected
				}				
				status.setText(counterInServerForClientsNumber +" client(s) connected");  // Change server's status
				allDataOutputStreamInServer.remove(OuputStreamInServer); // remove outputStream for this client from List
			}
			catch (IOException ex) {
				System.err.println(ex);
			}	
		}		
	}
}
