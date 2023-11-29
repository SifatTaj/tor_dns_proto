import com.sun.net.httpserver.HttpServer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Properties;


public class DNSServer {

  //static ServerSocket variable
  private static ServerSocket server;

  //socket server port on which it will listen
  private static int port = 9876;

  private static String[] getNode(Properties prop) {
    // create client connection
    try{
      String superNodeAddress = prop.getProperty("supernode.address");
      Integer superNodePort = Integer.valueOf(prop.getProperty("supernode.port"));
      TTransport transport = new TSocket(superNodeAddress, superNodePort);
      transport.open();
      TProtocol protocol = new TBinaryProtocol(transport);
      SuperNodeService.Client client = new SuperNodeService.Client(protocol);
      String[] nodeInfo = client.GetNode().split("\\s*,\\s*");
      transport.close();
      System.out.println("Successfully retrieved nodeInfo!\n");
      return nodeInfo;
    } catch (TException e) {
      System.out.println("Error connecting to the SuperNode!\n");
      System.exit(-1);
      return new String[]{};
    }
  }

  private static void setDomainName(String[] nodeInfo, String key, String value) throws TException {
    // create client connection
    System.out.printf("Sending Set(%s, %s) to %s\n", key, value, String.join(",", nodeInfo));
    TTransport transport = new TSocket(nodeInfo[0], Integer.valueOf(nodeInfo[1]));
    transport.open();
    TProtocol protocol = new TBinaryProtocol(transport);
    NodeService.Client client = new NodeService.Client(protocol);
    String trail = client.set_(key, value);
    System.out.printf("Set(%s, %s) Successful!\nTrail: %s\n", key, value, trail);
    transport.close();
  }

  private static void bulkSet(String[] nodeInfo, String fileName) throws Exception {
    try(BufferedReader br = new BufferedReader(new FileReader(fileName))){
      String line;
      while ((line = br.readLine()) != null) {
        setDomainName(nodeInfo, line.split("\\s*,\\s*")[0], line.split("\\s*,\\s*")[1]);
      }
      System.out.printf("BulkSet from %s complete!\n", fileName);
    } catch (FileNotFoundException err) {
      System.out.printf("File %s doesn't exist.\n", fileName);
    }
  }

  public static void main(String args[]) {

    try {
      // Get Configs
      Properties prop = new Properties();
      InputStream is = new FileInputStream("/Users/sifattaj/IdeaProjects/tor_dns_proto/gen-java/chordht.cfg");
      prop.load(is);
      String[] nodeInfo;
      Client.Option option = Client.Option.set;
      Console console = System.console();

      if (args.length == 1) {
        // set all the entries in the file provided
        do {
          nodeInfo = getNode(prop);
          if (nodeInfo[0].equals("NACK")) {
            console.printf("DHT isn't ready yet, trying again.\n");
            Thread.sleep(Integer.valueOf(prop.getProperty("client.wait")));
          } else {
            bulkSet(nodeInfo, args[0]);
          }
        } while (nodeInfo[0].equals("NACK"));
      }

      // Start the server
      HttpServer server = HttpServer.create(new InetSocketAddress(9876), 0);
      server.createContext("/onion_dns", new DNSHandler(prop));
      server.setExecutor(null); // creates a default executor
      server.start();
      System.out.println("DNS Server is ready!");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}