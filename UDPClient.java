import java.io.*;
import java.net.*;


/*O agenteUDP so envia e recebe pacotes, o transporteUDP faz o resto
   TRansfereCC processa o pedido de conexao
   Enviar e receber datagramas e o agenteUDP
*/

// Para ja isto pode ser usado para o cliente indicar que ficheiro quer receber e se a conexão foi aceite
class UDPClient{

   public static void main(String args[]) throws Exception{

      BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
      DatagramSocket clientSocket = new DatagramSocket();
      InetAddress IPAddress = InetAddress.getByName("localhost");
      String myIp = InetAddress.getLocalHost().getHostAddress();

      byte [] data = new byte[] {70, 67, 80, 32, 118, 97, 105, 32, 103, 97, 110, 104, 97, 114, 32, 97, 32, 67, 104, 97, 109, 112, 105, 111, 110, 115};

      byte[] sendData = new byte[1024];
      byte[] receiveData = new byte[1024];
     
      //Incio da conexão
      //Um pacote SYN nao recebe dados
      Pacote inicio = new Pacote(false, true, false, false, new byte[0], -1, myIp, IPAddress.getHostAddress());
      System.out.println(inicio.toString());
      sendData = inicio.pacote2bytes();
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
      clientSocket.send(sendPacket);


      //Recebe o ACK
      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      clientSocket.receive(receivePacket);
      String modifiedSentence = new String(receivePacket.getData());
      Pacote p = new Pacote();
      p.bytes2pacote(receivePacket.getData());
      System.out.println("FROM SERVER:" + p);


      //Envia ACK
      inicio = new Pacote(true, true, false, false, data, -1, myIp, IPAddress.getHostAddress());
      System.out.println(inicio.toString());
      sendData = inicio.pacote2bytes();
      sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
      clientSocket.send(sendPacket);

      // Envio de dados



      // Fim da conexão

      Pacote fim = new Pacote(false, false, true, false, data, -1, myIp, IPAddress.getHostAddress());
      System.out.println(fim.toString());
      sendData = fim.pacote2bytes();
      sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
      clientSocket.send(sendPacket);

      receivePacket = new DatagramPacket(receiveData, receiveData.length);
      clientSocket.receive(receivePacket);
      modifiedSentence = new String(receivePacket.getData());
      p.bytes2pacote(receivePacket.getData());
      System.out.println(p);

      clientSocket.close();
   }
}