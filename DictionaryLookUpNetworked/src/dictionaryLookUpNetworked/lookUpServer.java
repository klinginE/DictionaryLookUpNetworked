package dictionaryLookUpNetworked;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class lookUpServer {

	private class datagramObject {

		public int ack = 0;
		public int hlen = 0;
		public int len = 0;
		public int msgNum = 0;
		public String hash = "";
		public String msg = "";

	}
	
	private final static int BLOCK_SIZE = 1024;
	private final static int PORT = 4444;

	public static enum MSG_TYPE {

		WELCOME(0x00000000),
		CONFIRM(0x00000001),
		READY(0x00000002),
		NORMAL(0x00000003),
		TERMINATE(0x00000004);

		private final int value;
		private MSG_TYPE(int value) {
			this.value = value;
		}

		public int getValue() {

			return this.value;

		}

	}

	private int ack = 0;
	private MSG_TYPE msgNum = MSG_TYPE.WELCOME;
	DatagramSocket serverSocket = null;

	public static void main(String[] args) {

    	int len = args.length;
		Path path = null;
		String pwd = "";
		File dictFile = null;
		if (len < 1) {
			try {
	            pwd = new java.io.File(".").getCanonicalPath();
			}
		    catch (IOException e) {
				e.printStackTrace();
			}
			path = Paths.get(pwd, "dictionary.txt");
			dictFile = path.toFile();
		}
		else {
			dictFile = new File(args[0]);
		}

		if (dictFile.exists() && !dictFile.isDirectory() && dictFile.isFile() && dictFile.canRead()) {
			lookUpServer lus = new lookUpServer();
			lus.runServer(dictFile);
		}
		else {

			System.err.println("Error: " + dictFile.getPath() + " does not exists, it is not a regular file, or it cannont be read.");
			System.exit(1);

		}

	}

	public int sendData(InetAddress IPAddress, int port, int ack, MSG_TYPE msgNum, String msg) throws IOException {

		int numTries = 100;
		int currentAck = ack;
		byte[] data = null;
    	while (numTries > 0) {

    		ack = currentAck;
        	data = constructDatagram(ack, msgNum, msg);
        	DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, port);
      	    serverSocket.send(sendPacket);

      	    ack++;

      	    byte[] receiveData = new byte[BLOCK_SIZE];
      	    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      	    try {
      	    	serverSocket.setSoTimeout(500);
      	    	serverSocket.receive(receivePacket);
      	    }
      	    catch (SocketTimeoutException e) {

      	    	numTries--;
      	    	continue;

      	    }
      	    datagramObject dataObject = parseDatagram(receivePacket.getData());
      	    if (dataObject == null ||
        	    !receivePacket.getAddress().toString().equals(IPAddress.toString()) ||
        	    receivePacket.getPort() != port ||
                ((dataObject.ack != ack ||
                  dataObject.msgNum != msgNum.getValue()) &&
                  receivePacket.getAddress().toString().equals(IPAddress.toString()) &&
                  receivePacket.getPort() == port)) {

        	    if (receivePacket.getAddress() == IPAddress &&
             	   	receivePacket.getPort() == port)
        	        numTries--;

      	    	continue;

      	    }
      	    break;

    	}
    	if (numTries <= 0)
    		return 0;

    	if (data == null)
    		return 0;

    	return parseDatagram(data).len;

	}

	public void runServer(File dictFile) {

		try {

			serverSocket = new DatagramSocket(PORT);
            boolean serverIsRunning = true;

            while (serverIsRunning) {

            	ack = 0;
            	msgNum = MSG_TYPE.WELCOME;
            	byte[] receiveData = new byte[BLOCK_SIZE];
            	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            	serverSocket.setSoTimeout(0);
				serverSocket.receive(receivePacket);

                datagramObject dataObject = parseDatagram(receivePacket.getData());
                if (dataObject == null)
                	continue;

                if (dataObject.ack == ack && dataObject.msgNum == msgNum.getValue()) {

                	InetAddress IPAddress = receivePacket.getAddress();
                    int port = receivePacket.getPort();
                	int numTries = 100;
                	while (numTries > 0) {

                		ack = 0;
	                	msgNum = MSG_TYPE.CONFIRM;
	                	byte[] data = constructDatagram(ack, msgNum, "Hi");
	                	DatagramPacket sendPacket = new DatagramPacket(data, data.length, IPAddress, port);
	              	    serverSocket.send(sendPacket);
	
	              	    ack++;
	              	    msgNum = MSG_TYPE.READY;

	              	    receiveData = new byte[BLOCK_SIZE];
	              	    receivePacket = new DatagramPacket(receiveData, receiveData.length);
	              	    try {
	              	    	serverSocket.setSoTimeout(500);
	              	    	serverSocket.receive(receivePacket);
	              	    }
	              	    catch (SocketTimeoutException e) {

	              	    	//System.out.println("Timeout!");
	              	    	numTries--;
	              	    	continue;

	              	    }
	              	    dataObject = parseDatagram(receivePacket.getData());
	              	    if (dataObject == null ||
	              	    	!receivePacket.getAddress().toString().equals(IPAddress.toString()) ||
	              	    	receivePacket.getPort() != port ||
                            ((dataObject.ack != ack ||
                              dataObject.msgNum != msgNum.getValue()) &&
                              receivePacket.getAddress().toString().equals(IPAddress.toString()) &&
                             receivePacket.getPort() == port)) {

	              	    	if (receivePacket.getAddress() == IPAddress &&
	 	              	    	receivePacket.getPort() == port)
	              	    	    numTries--;

//	              	    	System.out.println("Error in received packet");
//	              	    	System.out.println("client address: " + IPAddress.toString() + " receivedAddress: " + receivePacket.getAddress().toString());
//	              	    	System.out.println("client port: " + port + " receivedPort: " + receivePacket.getPort());
//	              	    	if (dataObject == null)
//	              	    		System.out.println("Dataobject is null");
//	              	    	else {
//	              	    		System.out.println("My ack: " + ack + " Their ack: " + dataObject.ack);
//	              	    		System.out.println("My msgNum: " + msgNum.getValue() + " Their msgNum: " + dataObject.msgNum);
//	              	    	}

	              	    	continue;

	              	    }
	              	    break;

                	}
                	if (numTries <= 0)
                		continue;

                	msgNum = MSG_TYPE.NORMAL;
                	String word = dataObject.msg;
                	word = word.toUpperCase();
                	word = word.trim();

                	String output = processWord(dictFile, word);
                	int beginIndex = 0;
                	boolean finished = true;
                	while (beginIndex < output.length()) {

                		int amountSent = sendData(IPAddress, port, ack++, msgNum, output.substring(beginIndex));
                		if (amountSent == 0) {

                			finished = false;
                			break;

                		}
                		beginIndex += amountSent;

                	}
                	if (finished) {
                		msgNum = MSG_TYPE.TERMINATE;
                		sendData(IPAddress, port, ack++, msgNum, "END");
                	}

                }

            }
            serverSocket.close();

		}
		catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static String processWord(File dictFile, String word) {

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(dictFile));
		}
		catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		String line = "";
		String output = "";
		try {

			boolean wordFound = false;
			while ((line = br.readLine()) != null) {

				if (line.equals(word)) {

					wordFound = true;
					output += line;
					output += "\n";
					while((line = br.readLine()) != null) {

						if (line.matches("([A-Z])+") && !line.equals(word) && !line.equals(""))
							break;
						output += line;
						output += "\n";

					}
					if (line == null)
						break;

				}

			}
			if (!wordFound) {

				output += word;
			    output += " not found. Perhaps you misspelled it.\n";

			}

		}
		catch (IOException e) {
			e.printStackTrace();
		}

		try {
			br.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		return output;

	}

	private String byteToString(int[] bytes, int len, int offset, boolean castToChar) {

		String str = "";
		for (int i = 0; i < len; i++) {
			int temp = bytes[i + offset];
			if (castToChar)
				str += (char)(temp & 0x000000ff);
			else
				str += (temp & 0x000000ff);
		}
		return str;

	}

	private int byteToInt(int[] bytes, int len, int offset) {

		int num = 0x00000000;
		for (int i = 0; i < len; i++) {

			int temp = bytes[i + offset];
			temp <<= (8 * (len - 1 - i));
			num |= (temp & 0x000000ff);

		}
		return num;

	}

	private int[] intToByte (int num, int size) {

		int[] bytes = new int[size];
		if (num >= (0x00000001 << (size * 8)) || num < 0x00000000)
			return null;

		for (int i = 0; i < size; i++)
			bytes[i] = (int)(((num & (0x000000ff << ((size - 1 - i) * 8))) >> ((size - 1 - i) * 8)) & 0x000000ff);

		return bytes;

	}

	public datagramObject parseDatagram(byte[] byteDatagram) {
		
		int[] datagram = new int[byteDatagram.length];
		for (int i = 0; i < byteDatagram.length; i++)
			datagram[i] = (byteDatagram[i] & 0x000000ff);

		datagramObject dataObject = new datagramObject();
		dataObject.ack = byteToInt(datagram, 2, 0);
		//System.out.println(ack);

		dataObject.hlen = byteToInt(datagram, 2, 2);
		//System.out.println(hlen);

		dataObject.len = byteToInt(datagram, 2, 4);
		//System.out.println(len);

		dataObject.msgNum = byteToInt(datagram, 1, 6);
		//System.out.println(msgNum);

		dataObject.hash = byteToString(datagram, dataObject.hlen, 7, false);
		//System.out.println(hash);

		dataObject.msg = byteToString(datagram, dataObject.len, 7 + dataObject.hlen, true);
		//System.out.println(msg);

		int[] myByteHash = hashString(dataObject.msg);
		String myStrHash = byteToString(myByteHash, myByteHash.length, 0, false);
		if (!myStrHash.equals(dataObject.hash))
			return null;

		return dataObject;

	}

	public int[] hashString(String str) {

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		md.update(str.getBytes());
		byte[] byteHash = md.digest();
		int[] intHash = new int[byteHash.length];
		for (int i = 0; i < byteHash.length; i++)
			intHash[i] = (byteHash[i] & 0x000000ff);
		return intHash;

	}

	public byte[] constructDatagram(int ack, MSG_TYPE msgNum, String msg) {

		byte[] datagram = new byte[BLOCK_SIZE];
		for (int i = 0; i < BLOCK_SIZE; i++)
			datagram[i] = (0x00000000);

		int[] bytes = intToByte(ack, 2);
		if (bytes == null)
			return null;
		datagram[0] = (byte)bytes[0];
		datagram[1] = (byte)bytes[1];

		int hlen = 32;
		int[] hash = null;
		String temp = "";
		do {

			if (hash != null)
				hlen = hash.length;

			int maxLength = 128 - 7 - hlen;
			temp = "";
			if (msg.length() > maxLength)
				temp = msg.substring(0, maxLength - 1);
			else
				temp = msg;
			hash = hashString(temp);

		} while (hash.length > hlen);
		msg = temp;
		hlen = hash.length;

	    //System.out.println("construct hlen: " + hlen);
	    bytes = intToByte(hlen, 2);
	    if (bytes == null)
	    	return null;
		datagram[2] = (byte)bytes[0];
		datagram[3] = (byte)bytes[1];

		assert(hlen == 32);

		bytes = intToByte(msg.length(), 2);
		if (bytes == null)
			return null;
		datagram[4] = (byte)bytes[0];
		datagram[5] = (byte)bytes[1];

		bytes = intToByte(msgNum.getValue(), 1);
		if (bytes == null)
			return null;
		datagram[6] = (byte)bytes[0];

		//System.out.print("hash: ");
		for (int i = 0; i < hlen; i++) {
			datagram[7 + i] = (byte)(hash[i] & 0x000000ff);
			//System.out.print(datagram[7 + i]);
		}
		//System.out.println();

		for (int i = 0; i < msg.length(); i++)
			datagram[7 + hlen + i] = (byte)(msg.charAt(i) & 0x000000ff);

		return datagram;

	}

}