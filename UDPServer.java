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
        Condition controlo = lock.newCondition();
        byte[] receiveData = new byte[10024];
        byte[] sendData = new byte[1024];
 
        //Inicio da conexão
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        Pacote syn = new Pacote();
        syn.bytes2pacote(receivePacket.getData());
        System.out.println("FROM: UDPServer: Recebi " + syn.toString());
        //

        InetAddress IPAddress = receivePacket.getAddress();// substituir por syn.getOrigem ???
        int port = receivePacket.getPort();                 // Ver o q faz

        Estado estado = new Estado(new ArrayList<>(), syn.getDestino(), syn.getOrigem(), new ArrayList<>(), 1, 0, lock, espera, controlo);
        
        ControloConexaoServidor ccs = new ControloConexaoServidor(estado, receivePacket, serverSocket);
        ccs.start();

        while(estado.getFase() != 2){
            Pacote synAck = new Pacote(true, true, false, false, new byte[0], -1, syn.getDestino(), syn.getOrigem());
            sendData = synAck.pacote2bytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); // Ver o IPAddress e a port
            serverSocket.send(sendPacket);
            System.out.println("FROM: UDPServer: Enviei " + synAck.toString());
            Thread.sleep(200);
        }

        RecebePacotes rp = new RecebePacotes(estado, receivePacket, serverSocket);
        rp.start();

        //Enviar ACK
        while(estado.getFase() == 2){
            estado.esperaRecebe();
            Pacote pshAck = new Pacote(true, false, false, true, new byte[0], espera.getLastACK(), syn.getDestino(), syn.getOrigem());
            sendData = pshAck.pacote2bytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); // Ver o IPAddress e a port
            serverSocket.send(sendPacket);
            System.out.println("FROM: UDPServer: Enviei " + pshAck.toString());
        }

        rp.join();
        estado.acordaControlo();

        while (estado.getFase() != 4) {
            estado.esperaRecebe();
        }

        Pacote finAck = new Pacote(true, false, true, false, new byte[0], -1, syn.getDestino(), syn.getOrigem());
        sendData = finAck.pacote2bytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); // Ver o IPAddress e a port
        serverSocket.send(sendPacket);
        System.out.println("FROM: UDPServer: Enviei " + finAck.toString());

        ccs.join();
        serverSocket.close();
      }
}