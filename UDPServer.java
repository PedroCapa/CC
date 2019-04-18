import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class UDPServer{

   public static void main(String args[]) throws Exception{
        DatagramSocket serverSocket = new DatagramSocket(9876);
        Lock lock = new ReentrantLock();
        Condition espera = lock.newCondition();
        byte[] receiveData = new byte[10024];
        byte[] sendData = new byte[1024];
 
        //Inicio da conexão
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        Pacote syn = new Pacote();
        syn.bytes2pacote(receivePacket.getData());
        System.out.println("FROM: UDPServer: Recebi Syn" + syn.toString() + 1);
        //

        InetAddress IPAddress = receivePacket.getAddress();// substituir por syn.getOrigem ???
        int port = receivePacket.getPort();                 // Ver o q faz

        Estado estado = new Estado(new ArrayList<>(), syn.getDestino(), syn.getOrigem(), new ArrayList<>(), 1, 0, lock, espera);
        
        RecebePacotes rp = new RecebePacotes(estado, receivePacket, serverSocket);
        rp.start();


        Pacote synAck = new Pacote(true, true, false, false, new byte[0], -1, syn.getDestino(), syn.getOrigem());
        sendData = synAck.pacote2bytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); // Ver o IPAddress e a port
        serverSocket.send(sendPacket);
        System.out.println("FROM: UDPServer: Enviei Syn ACK" + synAck.toString());

        //Enviar ACK
        /*
        while(estado.getFase() == 2){

        }
        //
        */
        while (estado.getFase() != 4) {
            estado.espera();
        }

        Pacote finAck = new Pacote(true, false, true, false, new byte[0], -1, syn.getDestino(), syn.getOrigem());
        sendData = finAck.pacote2bytes();
        sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); // Ver o IPAddress e a port
        serverSocket.send(sendPacket);

        System.out.println("FROM UDPServer: Enviei Fin ACK" + finAck.toString() + 4);
      }
}