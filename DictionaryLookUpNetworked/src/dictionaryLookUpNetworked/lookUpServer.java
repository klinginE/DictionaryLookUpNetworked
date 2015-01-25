package dictionaryLookUpNetworked;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;

public class lookUpServer {

	private final static int BLOCK_SIZE = 1024;
	private final static int PORT = 4444;

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

		if (dictFile.exists() && !dictFile.isDirectory() && dictFile.isFile() && dictFile.canRead())
			runServer(dictFile);
		else {

			System.err.println("Error: " + dictFile.getPath() + " does not exists, it is not a regular file, or it cannont be read.");
			System.exit(1);

		}

	}

	public static void runServer(File dictFile) {

		try {

			DatagramSocket serverSocket = new DatagramSocket(PORT);
			byte[] receiveData = new byte[BLOCK_SIZE];
            boolean serverIsRunning = true;

            while (serverIsRunning) {

                  DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
				  serverSocket.receive(receivePacket);
				  String word = "";
                  word = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength());
          		  word = word.trim();
                  word = word.toUpperCase();

                  InetAddress IPAddress = receivePacket.getAddress();
                  int port = receivePacket.getPort();

                  String output = processWord(dictFile, word);
                  byte[] sendData = new byte[output.getBytes().length];
                  sendData = output.getBytes();
                  DatagramPacket sendPacket = null;
                  if (sendData.length <= BLOCK_SIZE) {
                	  sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                	  serverSocket.send(sendPacket);
                  }
                  else {
                	  byte[][] sendDatas = chunkArray(sendData, BLOCK_SIZE);
                	  for (int i = 0; i < sendDatas.length; i++) {
                		  sendPacket = new DatagramPacket(sendDatas[i], sendDatas[i].length, IPAddress, port);
                    	  serverSocket.send(sendPacket);
                	  }
                  }

                  sendData = "|||END|||".getBytes();
                  sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
                  serverSocket.send(sendPacket);

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

	public static byte[][] chunkArray(byte[] array, int chunkSize) {

        int numOfChunks = (int)Math.ceil((double)array.length / chunkSize);
        byte[][] output = new byte[numOfChunks][];
 
        for(int i = 0; i < numOfChunks; ++i) {
            int start = i * chunkSize;
            int length = Math.min(array.length - start, chunkSize);
 
            byte[] temp = new byte[length];
            System.arraycopy(array, start, temp, 0, length);
            output[i] = temp;

        }

        return output;

    }

}
