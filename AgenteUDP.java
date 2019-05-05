import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;
import java.util.Arrays;

class AgenteUDP{


    private Estado estado;
    private DatagramSocket udpSocket;

    public AgenteUDP(Estado e,int porta){
        this.estado = e;
        try{
            this.udpSocket = new DatagramSocket(porta,null);
        }catch(SocketException ex){ex.printStackTrace();}
    }

    public AgenteUDP(Estado e){
        this.estado = e;
        try{
            this.udpSocket = new DatagramSocket();
        
        }catch(SocketException ex){ex.printStackTrace();}
    }

    public void setEstado(Estado e){
        this.estado = e;
    }

    public void send(Pacote p){
        try{System.out.println(p.toString());
            byte[] sendData = p.pacote2bytes();
            DatagramPacket udpPacket = new DatagramPacket(sendData, sendData.length, estado.getDestino(), estado.getPortaDestino());
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
            System.out.println(ret);
            return ret;
        }
        catch(IOException ex){ex.printStackTrace();}
        return null;
    }

    public Pacote receive(int ms){
        try{
            byte[] buf = new byte[4*1024];
            DatagramPacket udpPacket = new DatagramPacket(buf,buf.length);
            udpSocket.setSoTimeout(ms);
            udpSocket.receive(udpPacket);
            udpSocket.setSoTimeout(0);
            Pacote ret = new Pacote();System.out.println(udpPacket.getLength());
            ret.bytes2pacote(Arrays.copyOf(udpPacket.getData(),udpPacket.getLength()));
            System.out.println(ret);
            return ret;
        }catch(SocketException ex){return null;}
        catch(IOException ex){ex.printStackTrace();}
        finally{
        }
        return null;
    }

    /** Recebe uma mensagem e insere a porta e IP da origem no estado */
    public Pacote accept(){
        byte[] buf = new byte[4*1024];System.out.println(udpSocket.getLocalAddress()+"  "+udpSocket.getLocalPort());
        DatagramPacket udpPacket = new DatagramPacket(buf,buf.length);
        try{
            udpSocket.receive(udpPacket);
        }catch(IOException ex){ex.printStackTrace();}
        this.estado.setDestino(udpPacket.getAddress());
        this.estado.setPortaDestino(udpPacket.getPort());
        Pacote ret = new Pacote();
        ret.bytes2pacote(Arrays.copyOf(udpPacket.getData(),udpPacket.getLength()));
        return ret;
    }



}
