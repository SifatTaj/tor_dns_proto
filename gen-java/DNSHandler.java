import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Properties;


public class DNSHandler implements HttpHandler {

  private final Properties prop;

  DNSHandler(Properties prop) {
    this.prop = prop;
  }

  private static String getDomainName(String[] nodeInfo, String key) throws TException {
    // create client connection
    System.out.printf("Sending Get(%s) to %s\n", key, String.join(",", nodeInfo));
    TTransport transport = new TSocket(nodeInfo[0], Integer.valueOf(nodeInfo[1]));
    transport.open();
    TProtocol protocol = new TBinaryProtocol(transport);
    NodeService.Client client = new NodeService.Client(protocol);
    GetResponse getResponse = client.get_(key);
    if (getResponse.address == null) {
      System.out.printf("Get(%s) Failed - KeyNotFoundError!\nTrail: %s\n", key, getResponse.trail);
      return "Address not found!";
    } else {
      System.out.printf("Get(%s) Successful!\nOnion Address: %s\nTrail: %s\n",
              key, getResponse.address, getResponse.trail);
    }
    transport.close();
    return getResponse.address;
  }

  private static String domainDesc(String[] nodeInfo) throws TException {
    System.out.printf("Sending Desc() to %s\n", String.join(",", nodeInfo));
    TTransport transport = new TSocket(nodeInfo[0], Integer.valueOf(nodeInfo[1]));
    transport.open();
    TProtocol protocol = new TBinaryProtocol(transport);
    NodeService.Client client = new NodeService.Client(protocol);
    List<String> detailList = client.desc(Integer.valueOf(nodeInfo[2]));

    String domainList = "";
    for(String nodeDetails: detailList){
      domainList = domainList + nodeDetails + "\n";
    }
    return domainList;
  }

  private static String setDomainName(String[] nodeInfo, String key, String value) throws TException {
    // create client connection
    System.out.printf("Sending Set(%s, %s) to %s\n", key, value, String.join(",", nodeInfo));
    TTransport transport = new TSocket(nodeInfo[0], Integer.valueOf(nodeInfo[1]));
    transport.open();
    TProtocol protocol = new TBinaryProtocol(transport);
    NodeService.Client client = new NodeService.Client(protocol);
    String trail = client.set_(key, value);
    System.out.printf("Set(%s, %s) Successful!\nTrail: %s\n", key, value, trail);
    transport.close();
    return "Registration Successful!";
  }

  private static String[] getNode(Properties prop) {
    // create client connection
    try {
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

  @Override
  public void handle(HttpExchange t) throws IOException {
    String uri = t.getRequestURI().toString();

    String service = uri.split("/")[2];

    if (service.equals("get")) {
      String key = uri.split("/")[3];

      System.out.println("Received request for " + key);

      String response = "DHT not reachable";

      // Retrieve node info from the DHT!
      String[] nodeInfo = getNode(prop);
      if (nodeInfo[0].equals("NACK")) {
        System.out.println("DHT isn't ready yet, try again later.\n");
      } else {
        try {
          response = getDomainName(nodeInfo, key);
          t.sendResponseHeaders(200, response.length());
          OutputStream os = t.getResponseBody();
          os.write(response.getBytes());
          os.close();
        } catch (TException e) {
          e.printStackTrace();
        }
      }

    // Domain Name Registration Handler
    } else if (service.equals("reg")) {

      String key = uri.split("/")[3];
      String url = uri.split("/")[4];

      System.out.println("Received reg request for " + key);
      System.out.println("with url " + url);

      String response = "DHT not reachable";

      // Retrieve node info from the DHT!
      String[] nodeInfo = getNode(prop);
      if (nodeInfo[0].equals("NACK")) {
        System.out.println("DHT isn't ready yet, try again later.\n");
      } else {
        try {
          response = getDomainName(nodeInfo, key);

          if (response.equals("Address not found!")) {
            response = setDomainName(nodeInfo, key, url);
          } else {
            response = "The address already exists\n" + response;
          }

          t.sendResponseHeaders(200, response.length());
          OutputStream os = t.getResponseBody();
          os.write(response.getBytes());
          os.close();
        } catch (TException e) {
          e.printStackTrace();
        }
      }
    } else if (service.equals("list")) {
      // Retrieve node info from the DHT!

      String response = "DHT not reachable";

      String[] nodeInfo = getNode(prop);
      if (nodeInfo[0].equals("NACK")) {
        System.out.println("DHT isn't ready yet, try again later.\n");
      } else {
        try {
          response = domainDesc(nodeInfo);
          t.sendResponseHeaders(200, response.length());
          OutputStream os = t.getResponseBody();
          os.write(response.getBytes());
          os.close();
        } catch (TException e) {
          e.printStackTrace();
        }
      }
    }
  }
}