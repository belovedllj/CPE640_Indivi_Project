// warning, test computer can not use "ipv4" and "ipv6" connection together, it will cause error
/*----------------------------------------------------------------
 *  Author:        Peng Peng
 *  Written:       10/13/2013
 *  Last updated:  10/14/2013
 
 *  This application can be used for either server or client without confusion
 *  Execute the program twice. one for server, one for client
 * 
 *  advanced properties : (1) Allowing client to reconnect
 *                        (2) Server side can get the information about client's disconnection
 *                            (clicked "disconnect button" or directly clost client's UI)
 *                             so server will change its connect's status and  preparing for a second connection
 *                        (3) Vice versa, if server closed it UI or clicked "disconnect button", client will get 
 *                            information and do right reactions.
 *                        (4) For client side if clicked "connect button", but server is off
 *                            this connect is invalid  you don't need to click "disconnect button"
 *                            to disconnect
 *  Using multithreading programming to handle Server's accepting
 *  and Client's connection
 *----------------------------------------------------------------*/
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;


public class ChatApp extends JFrame {
	private ServerSocket serverSocket;
	private Socket socket;
	
	private JRadioButton hostTheChat = new JRadioButton("Host the Chat");
	private JRadioButton joinTheChat = new JRadioButton("Join the Chat");
	private JTextField IPAddress = new JTextField(22);
	private JTextField portNumber = new JTextField(10);
	private JButton connectStartButton = new JButton("Connect/Start");
	private JButton disconnectButton = new JButton("Disconnect");
	private JLabel status = new JLabel("Status: Disconnected");
	private JTextArea chatWindow = new JTextArea();
	private JTextField chatSendLine = new JTextField();
	private boolean isServerConnected = false;
	private boolean isHosted = true;
	private boolean AsServerAlreadyConnected = false;
	private boolean AsClientAlreadyConnected = false;
	private DataInputStream inputFromClientOrServer;
	private DataOutputStream outputToClientOrServer;
	private Thread ThreadHandlingAcceptInServer;
	private Thread ThreadHandlingInClient;
	
	public ChatApp() {
		
		
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
				if (AsServerAlreadyConnected || AsClientAlreadyConnected) {
					try {
						outputToClientOrServer.writeUTF(message); // sent message
						outputToClientOrServer.flush();  // clean up the Output stream
					} catch (IOException e1) {					
						e1.printStackTrace();
					}
				}			
			}
		});
	}
	
	public static void main(String[] args) {
		new ChatApp();
	}
	
	private void performServerConnection() throws UnknownHostException {
	
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
		socket = null;
		try {					
			serverSocket = new ServerSocket(port);		
			chatWindow.append("Server started at " + new Date() + "\n");
			socketAcceptThreadAsServer accept = new socketAcceptThreadAsServer();
			ThreadHandlingAcceptInServer = new Thread(accept); 
			ThreadHandlingAcceptInServer.start();
			// start a new thread to handle waiting due to no letting button stuck at pressed
		}
		catch(IOException ex) {
			System.err.println(ex);
		}
	}
	private void performClientConnection() {		
		serverSocket = null;
		socket = null;		
		socketAcceptThreadAsClient clientThread = new socketAcceptThreadAsClient();
		ThreadHandlingInClient = new Thread(clientThread);
		ThreadHandlingInClient.start();
		// start a new thread to handle waiting due to no letting button stuck at pressed
	}
	private void cancelServerConnection() throws IOException {
		outputToClientOrServer.writeUTF("#$Closed");
		outputToClientOrServer.flush();
		ThreadHandlingAcceptInServer.interrupt(); 
		isServerConnected = false;
		AsServerAlreadyConnected = false;   //  couldn't send message to others now
		inputFromClientOrServer = null;
		outputToClientOrServer = null;
		serverSocket.close();
		socket.close();
		serverSocket = null;
		socket = null;
		status.setText("Status: Disconnected");		
	}
	private void cancelClientConnection() throws IOException {
		outputToClientOrServer.flush();		
		AsClientAlreadyConnected = false;  // couldn't send message to others
		inputFromClientOrServer = null;
		outputToClientOrServer = null;
		socket.close();
		socket = null;
		status.setText("Status: Disconnected");
		ThreadHandlingInClient.interrupt(); // stop the socket thread on client 
	}
	
	private class socketAcceptThreadAsServer implements Runnable {
		@Override
		public void run(){	
			while (true) {  // using a loop for receiving the same client to reconnect
				try {
					socket = serverSocket.accept(); //wait for client's connection
					chatWindow.append("One client has connected the server\n");
					AsServerAlreadyConnected = true; //Server can sent message to client
					status.setText("Status: Connected"); // change status automatically
					inputFromClientOrServer = new DataInputStream(socket.getInputStream());
					outputToClientOrServer = new DataOutputStream(socket.getOutputStream());
					while (true) {
						String inputChat = inputFromClientOrServer.readUTF();  // read message from client
						chatWindow.append("C1: " + inputChat + "\n");					
					}				
				} catch(SocketException ex) {
					status.setText("Status: Disconnected"); // Change server's status
					chatWindow.append("Client has left server.\n");
					AsServerAlreadyConnected = false;
				} catch(EOFException ex2) {
					status.setText("Status: Disconnected"); // Change server's status
					chatWindow.append("Client has left server.\n");
					AsServerAlreadyConnected = false;
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
				socket = new Socket(hostAddress, port);
				// if Server and Client at same computer, we can use next statement instead
				//socket = new Socket("localhost", port); 
				AsClientAlreadyConnected = true;
				chatWindow.append("Successfully connected with server\n");
				status.setText("Status: Connected");
				inputFromClientOrServer = new DataInputStream(socket.getInputStream());
				outputToClientOrServer = new DataOutputStream(socket.getOutputStream());
				while (true) {
					String inputChat = inputFromClientOrServer.readUTF();  // read message from client
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
			} catch(SocketException ex1) {
				status.setText("Status: Disconnected"); // Change client's status
				chatWindow.append("Server is offline.\n");
				AsClientAlreadyConnected = false;  // couldn't send message to others	
			} catch (IOException ex) {
				System.err.println(ex);
			}			
		}		
	}	
}
