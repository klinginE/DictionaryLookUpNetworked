package dictionaryLookUpNetworked;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class lookUpClient {

	private final static int BLOCK_SIZE = 1024;

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
						port = Integer.parseInt(args[1]);

				}

			}

			byte[] sendData = new byte[BLOCK_SIZE];
			sendData = getWord().getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
			clientSocket.send(sendPacket);

			byte[] receiveData = new byte[BLOCK_SIZE];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length, IPAddress, port);

			clientSocket.close();

		}
		catch (IOException e) {
            e.printStackTrace();
		}

	}

	public static String getWord() {

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String word = null;

        System.out.print("Enter a word: ");
	    try {
	        word = br.readLine();
	    }
	    catch (IOException ioe) {
	        System.err.println("getWord() IO error trying to read word, because:" + ioe.getLocalizedMessage());
	        System.exit(1);
	    }

	    word = word.toUpperCase();
		return word;

	}

}