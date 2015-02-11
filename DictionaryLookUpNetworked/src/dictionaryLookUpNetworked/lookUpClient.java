package dictionaryLookUpNetworked;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class lookUpClient {

	private int ack = 0;
	private MSG_TYPE msgNum = MSG_TYPE.WELCOME;

	private class datagramObject {

		public int ack = 0;
		public int hlen = 0;
		public int len = 0;
		public int msgNum = 0;
		public String hash = "";
		public String msg = "";

	}

	private final static int BLOCK_SIZE = 1024;
	public static enum MSG_TYPE {

		WELCOME(0x00),
		CONFIRM(0x01),
		READY(0x02),
		NORMAL(0x03),
		TERMINATE(0x04);

		private final int value;
		private MSG_TYPE(int value) {
			this.value = value;
		}

		public int getValue() {

			return this.value;

		}

	}

	public static void main(String[] args) {

		try {

			DatagramSocket clientSocket = new DatagramSocket();
			InetAddress IPAddress = InetAddress.getByName("localhost");
			int port = 4444;
			if (args.length > 0) {

				if (args.length > 1) {

					IPAddress = InetAddress.getByName(args[0]);
					port = Integer.parseInt(args[1]);

				}
				else {

					if (args[0].contains("."))
						IPAddress = InetAddress.getByName(args[0]);
					else
						port = Integer.parseInt(args[0]);

				}

			}

			lookUpClient luc = new lookUpClient();
			luc.runClient(clientSocket, IPAddress, port);

		}
		catch (IOException e) {
            e.printStackTrace();
		}


	}

	public datagramObject receiveData(DatagramSocket clientSocket, InetAddress IPAddress, int port, int ack, MSG_TYPE msgNum, String word) throws IOException {

		int numTries = 20;
		datagramObject dataObject = null;
    	while (numTries > 0) {

      	    byte[] receiveData = new byte[BLOCK_SIZE];
      	    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      	    try {
      	    	clientSocket.setSoTimeout(500);
      	    	clientSocket.receive(receivePacket);
      	    }
      	    catch (SocketTimeoutException e) {

      	    	numTries--;
      	    	continue;

      	    }
      	    dataObject = parseDatagram(receivePacket.getData());
      	    if (dataObject != null && dataObject.msgNum == MSG_TYPE.TERMINATE.getValue())
      	    	return dataObject;
      	    
      	    if (dataObject == null ||
        	    !receivePacket.getAddress().toString().split("/")[1].equals(IPAddress.toString().split("/")[1]) ||
        	    receivePacket.getPort() != port ||
                ((dataObject.ack != ack ||
                  dataObject.msgNum != msgNum.getValue()) &&
                 receivePacket.getAddress().toString().split("/")[1].equals(IPAddress.toString().split("/")[1]) &&
                 receivePacket.getPort() == port)) {

        	    if (receivePacket.getAddress().toString().split("/")[1].equals(IPAddress.toString().split("/")[1]) &&
             	   	receivePacket.getPort() == port)
        	        numTries--;

//      	    	System.out.println("Error in received packet");
//      	    	System.out.println("client address: " + IPAddress.toString().split("/")[1] + " receivedAddress: " + receivePacket.getAddress().toString().split("/")[1]);
//      	    	System.out.println("client port: " + port + " receivedPort: " + receivePacket.getPort());
//      	    	if (dataObject == null)
//      	    		System.out.println("Dataobject is null");
//      	    	else {
//      	    		System.out.println("My ack: " + ack + " Their ack: " + dataObject.ack);
//      	    		System.out.println("My msgNum: " + msgNum.getValue() + " Their msgNum: " + dataObject.msgNum);
//      	    	}
        	    
        	    if (dataObject.ack < ack) {

        	    	MSG_TYPE newMsgType = MSG_TYPE.TERMINATE;
        	    	switch (dataObject.msgNum) {
        	    	
        	    		case 0:
        	    			newMsgType = MSG_TYPE.TERMINATE;
        	    		    break;
        	    		case 1:
        	    			newMsgType = MSG_TYPE.READY;
        	    		    break;
        	    		case 2:
        	    		case 3:
        	    			newMsgType = MSG_TYPE.NORMAL;
        	    		    break;
        	    		default:
        	    			newMsgType = MSG_TYPE.TERMINATE;

        	    	}
        	    	byte[] sendData = constructDatagram(dataObject.ack + 1, newMsgType, word);
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
					clientSocket.send(sendPacket);

        	    }

      	    	continue;

      	    }
      	    break;

    	}
    	if (numTries <= 0)
    		return null;

    	return dataObject;

	}

	public void runClient(DatagramSocket clientSocket, InetAddress IPAddress, int port) {

		try {

			do {
	
				ack = 0;
				msgNum = MSG_TYPE.WELCOME;

				clientSocket = new DatagramSocket();
				String word = getWord();
	
				byte[] sendData = constructDatagram(ack, msgNum, "Hi");
				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
				clientSocket.send(sendPacket);

				msgNum = MSG_TYPE.CONFIRM;
				int numTries = 20;
				datagramObject dataObject = null;
				byte[] receiveData = null;
				DatagramPacket receivePacket = null;
				while (numTries > 0) {

					receiveData = new byte[BLOCK_SIZE];
					receivePacket = new DatagramPacket(receiveData, receiveData.length, IPAddress, port);
					try {
		      	    	clientSocket.setSoTimeout(500);
		      	    	clientSocket.receive(receivePacket);
		      	    }
		      	    catch (SocketTimeoutException e) {
	
		      	    	numTries--;
		      	    	continue;
	
		      	    }
					dataObject = parseDatagram(receivePacket.getData());
					if (dataObject == null ||
			        	!receivePacket.getAddress().toString().split("/")[1].equals(IPAddress.toString().split("/")[1]) ||
			        	receivePacket.getPort() != port ||
			            ((dataObject.ack != ack ||
			              dataObject.msgNum != msgNum.getValue()) &&
			             receivePacket.getAddress().toString().split("/")[1].equals(IPAddress.toString().split("/")[1]) &&
			             receivePacket.getPort() == port)) {

	              	    if (receivePacket.getAddress().toString().split("/")[1].equals(IPAddress.toString().split("/")[1]) &&
	 	              	   	receivePacket.getPort() == port)
	              	        numTries--;

	              	    continue;

	              	}
	              	break;

                }
                if (numTries <= 0)
                	continue;

                ack++;
                msgNum = MSG_TYPE.READY;
                //long startTime = System.currentTimeMillis();
				sendData = constructDatagram(ack, msgNum, word);
				sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
				clientSocket.send(sendPacket);
				msgNum = MSG_TYPE.NORMAL;

				while (true) {
	
					dataObject = receiveData(clientSocket, IPAddress, port, ack, msgNum, word);
					ack++;
					if (dataObject.msgNum == MSG_TYPE.TERMINATE.getValue())
						break;

					sendData = constructDatagram(ack, msgNum, word);
					sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
					clientSocket.send(sendPacket);

					String output = dataObject.msg;
					output = output.trim();
					System.out.print(output);

				}
				//System.out.println("Runtime: " + (System.currentTimeMillis() - startTime));

				sendData = constructDatagram(ack, MSG_TYPE.TERMINATE, "|||END|||");
				sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
				clientSocket.send(sendPacket);
	
				clientSocket.close();
	
			} while (userLoop());

		}
		catch (IOException e) {
            e.printStackTrace();
		}

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

	public static String getWord() {

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String word = "";

        do {

	        System.out.print("Enter a word: ");
		    try {
		        word = br.readLine();
		        System.out.println();
		    }
		    catch (IOException ioe) {
		        System.err.println("getWord() IO error trying to read word, because:" + ioe.getLocalizedMessage());
		        System.exit(1);
		    }

        } while (word.equals(""));

	    word = word.toUpperCase();
		return word;

	}

	public static boolean userLoop() {

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String word = "";

        do {
	        System.out.print("\n\nDo you wish to enter another word? [Y/n]: ");
		    try {
		        word = br.readLine();
		        System.out.println();
		    }
		    catch (IOException ioe) {
		        System.err.println("getWord() IO error trying to read word, because:" + ioe.getLocalizedMessage());
		        System.exit(1);
		    }
        } while (word.equals(""));

	    word = word.toUpperCase();
	    if (word.equals("Y") || word.contains("YES"))
	    	return true;
		return false;

	}

}