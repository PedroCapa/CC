import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;
import java.util.Arrays;

class AgenteUDP{


    private DatagramSocket udpSocket;

    public void close(){
        this.udpSocket.close();
    }

    public AgenteUDP(int porta){
        try{
            this.udpSocket = new DatagramSocket(porta,null);
        }catch(SocketException ex){ex.printStackTrace();}
    }

    public AgenteUDP(){
        try{
            this.udpSocket = new DatagramSocket();
        }catch(SocketException ex){ex.printStackTrace();}
    }


    public void send(Pacote p){
        try{System.out.println("Enviou: " + p.toString() + p.getIpDestino() +" , "+ p.getPortaDestino());
            byte[] sendData = p.pacote2bytes();
            DatagramPacket udpPacket = new DatagramPacket(sendData, sendData.length, p.getIpDestino(), p.getPortaDestino());
            this.udpSocket.send(udpPacket);
        }catch(IOException ex){ex.printStackTrace();}
    }



    public Pacote receive(){
        try{
            byte[] buf = new byte[4*1024];
            DatagramPacket udpPacket = new DatagramPacket(buf,buf.length);
            udpSocket.receive(udpPacket);
            Pacote ret = new Pacote();System.out.println(udpPacket.getLength());
            ret.bytes2pacote(Arrays.copyOf(udpPacket.getData(),udpPacket.getLength()));
            System.out.println("Recebeu: "+ret);
            ret.setIntervenientes(udpPacket.getPort(),udpSocket.getLocalPort(),udpPacket.getAddress(),udpSocket.getLocalAddress());
            return ret;
        }
        catch(IOException ex){}
        return null;
    }

    public Pacote receive(int ms){
        try{
            byte[] buf = new byte[4*1024];
            DatagramPacket udpPacket = new DatagramPacket(buf,buf.length);
            udpSocket.setSoTimeout(ms);
            udpSocket.receive(udpPacket);
            udpSocket.setSoTimeout(0);
            Pacote ret = new Pacote();
            ret.bytes2pacote(Arrays.copyOf(udpPacket.getData(),udpPacket.getLength()));
            ret.setIntervenientes(udpPacket.getPort(),udpSocket.getLocalPort(),udpPacket.getAddress(),udpSocket.getLocalAddress());
            System.out.println("Recebeu: " + ret);
            return ret;
        }catch(SocketException ex){return null;}
        catch(java.net.SocketTimeoutException ex){return null;}
        catch(IOException ex){ex.printStackTrace();}
        finally{
        }
        return null;
    }



}
