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

      byte[] sendData = new byte[1024];
      byte[] receiveData = new byte[1024];
      //Incio da conexão
      Pacote inicio = new Pacote(false, true, false, false, new byte[1], 200000000, myIp, IPAddress.getHostAddress());

      sendData = inicio.pacote2bytes();
      DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
      clientSocket.send(sendPacket);

      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      clientSocket.receive(receivePacket);
      String modifiedSentence = new String(receivePacket.getData());


      // Envio de dados



      // Fim da conexão

      
      
      clientSocket.close();
   }
}