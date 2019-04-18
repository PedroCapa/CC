import java.io.*;
import java.net.*;
import java.util.ArrayList;

class UDPServer
{
   public static void main(String args[]) throws Exception{
        DatagramSocket serverSocket = new DatagramSocket(9876);
        byte[] receiveData = new byte[10024];
        byte[] sendData = new byte[1024];
        
        //Inicio da conexão
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        Pacote syn = new Pacote();
        syn.bytes2pacote(receivePacket.getData());

        System.out.println(syn.toString());
        

        InetAddress IPAddress = receivePacket.getAddress();// substituir por syn.getOrigem ???
                  
        int port = receivePacket.getPort();                 // Ver o q faz

        Estado estado = new Estado(new ArrayList<>(), syn.getDestino(), syn.getOrigem(), new ArrayList<>(), 1, 0);
        
        Pacote synAck = new Pacote(true, true, false, false, new byte[0], -1, syn.getDestino(), syn.getOrigem());
        sendData = synAck.pacote2bytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); // Ver o IPAddress e a port
        serverSocket.send(sendPacket);

        System.out.println(synAck.toString());

        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        syn.bytes2pacote(receivePacket.getData()); // Verificar se a conexão foi estabelecida

        System.out.println(syn.toString());        

        //verificar o pacote ACk esta correto

        //Após a conexão
/*
        Pacote psh = new Pacote();
        do{
            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            psh.bytes2pacote(receivePacket.getData());
            serverSocket.send(sendPacket);
            //Adicionar ao estado dos pacotes recebidos e mandar ACK no caso de ser psh

            System.out.println(psh.toString());
        }
        while(psh.getPsh() == true);
*/

        receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        Pacote fin = new Pacote();
        fin.bytes2pacote(receivePacket.getData());

        System.out.println(fin.toString());
                
        Pacote finAck = new Pacote(true, false, true, false, new byte[0], -1, syn.getDestino(), syn.getOrigem());
        sendData = finAck.pacote2bytes();
        sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); // Ver o IPAddress e a port
        serverSocket.send(sendPacket);

        System.out.println(finAck.toString());
      }
}