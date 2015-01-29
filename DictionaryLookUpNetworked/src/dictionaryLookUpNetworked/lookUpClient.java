package dictionaryLookUpNetworked;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class lookUpClient {

	private final static int BLOCK_SIZE = 1024;
	public enum MSG_TYPE {

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

		lookUpClient luc = new lookUpClient();
		
		int[] data = luc.constructDatagram(19, MSG_TYPE.TERMINATE, "Yes");
		int ack = 0x00000000;
		for (int i = 0; i < 2; i++) {
			int temp = data[i];
			temp <<= (8 * (2 - 1 - i));
			ack |= (temp & 0x000000ff);
		}
		System.out.println(ack);

		int hlen = 0x00000000;
		for (int i = 0; i < 2; i++) {
			int temp = data[i + 2];
			temp <<= (8 * (2 - 1 - i));
			hlen |= (temp & 0x000000ff);
		}
		System.out.println(hlen);

		int len = 0x00000000;
		for (int i = 0; i < 2; i++) {
			int temp = data[i + 4];
			temp <<= (8 * (2 - 1 - i));
			len |= (temp & 0x000000ff);
		}
		System.out.println(len);

		int msgNum = 0x00000000;
		for (int i = 0; i < 1; i++) {
			int temp = data[i + 6];
			temp <<= (8 * (1 - 1 - i));
			msgNum |= (temp & 0x000000ff);
		}
		System.out.println(msgNum);

		String hash = "";
		for (int i = 0; i < hlen; i++) {
			int temp = data[i + 7];
			hash += (temp & 0x000000ff);
		}
		System.out.println(hash);

		String msg = "";
		for (int i = 0; i < len; i++) {
			int temp = data[i + 7 + hlen];
			msg += (char)(temp & 0x000000ff);
		}
		System.out.println(msg);
//		try {
//
//			DatagramSocket clientSocket = new DatagramSocket();
//			InetAddress IPAddress = InetAddress.getByName("localhost");
//			int port = 4444;
//			if (args.length > 0) {
//
//				if (args.length > 1) {
//
//					IPAddress = InetAddress.getByName(args[0]);
//					port = Integer.parseInt(args[1]);
//
//				}
//				else {
//
//					if (args[0].contains("."))
//						IPAddress = InetAddress.getByName(args[0]);
//					else
//						port = Integer.parseInt(args[0]);
//
//				}
//
//			}
//
//			do {
//
//				String word = getWord();
//				byte[] sendData = new byte[word.getBytes().length];
//				sendData = word.getBytes();
//				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
//				clientSocket.send(sendPacket);
//
//				while (true) {
//	
//					byte[] receiveData = new byte[BLOCK_SIZE];
//
//					
//					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length, IPAddress, port);
//					clientSocket.receive(receivePacket);
//					String output = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
//					output = output.trim();
//					if (output.equals("|||END|||"))
//						break;
//					System.out.print(output);
//	
//				}
//
//			} while (userLoop());
//			clientSocket.close();
//
//		}
//		catch (IOException e) {
//            e.printStackTrace();
//		}

	}

	private int[] intToByte (int num, int size) {

		int[] bytes = new int[size];
		if (num >= (0x00000001 << (size * 8)) || num < 0x00000000)
			return null;

		for (int i = 0; i < size; i++)
			bytes[i] = (int)(((num & (0x000000ff << ((size - 1 - i) * 8))) >> ((size - 1 - i) * 8)) & 0x000000ff);

		return bytes;

	}
	public int[] constructDatagram(int ack, MSG_TYPE msgNum, String msg) {

		int[] datagram = new int[BLOCK_SIZE];
		for (int i = 0; i < BLOCK_SIZE; i++)
			datagram[i] = (0x00000000);
		int[] bytes = intToByte(ack, 2);
		if (bytes == null)
			return null;
		datagram[0] = bytes[0];
		datagram[1] = bytes[1];

		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}

		md.update(msg.getBytes());
		byte[] hash = md.digest();
	    int hlen = hash.length;
	    System.out.println("construct hlen: " + hlen);
	    bytes = intToByte(hlen, 2);
	    if (bytes == null)
	    	return null;
		datagram[2] = bytes[0];
		datagram[3] = bytes[1];
		
		int maxLength = 128 - 7 - hlen;
		if (msg.length() > maxLength)
			msg = msg.substring(0, maxLength - 1);

		bytes = intToByte(msg.length(), 2);
		if (bytes == null)
			return null;
		datagram[4] = bytes[0];
		datagram[5] = bytes[1];

		bytes = intToByte(msgNum.getValue(), 1);
		if (bytes == null)
			return null;
		datagram[6] = bytes[0];

		System.out.print("hash: ");
		for (int i = 0; i < hlen; i++) {
			datagram[7 + i] = (hash[i] & 0x000000ff);
			System.out.print(datagram[7 + i]);
		}
		System.out.println();
		
		byte[] messageBytes = msg.getBytes();
		for (int i = 0; i < messageBytes.length; i++)
			datagram[7 + hlen + i] = (messageBytes[i] & 0x000000ff);

		return datagram;

	}

	public static String getWord() {

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String word = null;

        System.out.print("Enter a word: ");
	    try {
	        word = br.readLine();
	        System.out.println();
	    }
	    catch (IOException ioe) {
	        System.err.println("getWord() IO error trying to read word, because:" + ioe.getLocalizedMessage());
	        System.exit(1);
	    }

	    word = word.toUpperCase();
		return word;

	}

	public static boolean userLoop() {

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String word = null;

        System.out.print("\n\nDo you wish to enter another word? [Y/n]: ");
	    try {
	        word = br.readLine();
	        System.out.println();
	    }
	    catch (IOException ioe) {
	        System.err.println("getWord() IO error trying to read word, because:" + ioe.getLocalizedMessage());
	        System.exit(1);
	    }

	    word = word.toUpperCase();
	    if (word.equals("Y") || word.contains("YES"))
	    	return true;
		return false;

	}

}