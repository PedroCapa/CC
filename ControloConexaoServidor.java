import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class ControloConexaoServidor extends Thread{
	Estado estado;
	DatagramPacket receivePacket;
	DatagramSocket serverSocket;

	ControloConexaoServidor(){
		this.estado = new Estado();
	}

	ControloConexaoServidor(Estado e, DatagramPacket dp, DatagramSocket ds){
		this.estado = e;
		this.receivePacket = dp;
		this.serverSocket = ds;
	}

	public void run(){
		byte[] receiveData = new byte[10024];
		try{
            while(this.estado.getFase() != 2){
                Pacote syn = new Pacote();
			    this.receivePacket = new DatagramPacket(receiveData, receiveData.length);
                this.serverSocket.receive(receivePacket);
                syn.bytes2pacote(receivePacket.getData()); // Verificar se a conexão foi estabelecida

                if(syn.synAck()){
                    System.out.println("FROM: ControloConexaoServidor: Recebi " + syn.toString());
                    this.estado.setFase(2);
                }
            }
		}
		catch(IOException e){}
		
        //Adormecer Thread e esperar que seja acordada
        this.estado.esperaControlo();
        this.estado.acordaRecebe();
        try{
            while(this.estado.getFase() != 4){
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                Pacote fin = new Pacote();
                fin.bytes2pacote(receivePacket.getData());
                if(fin.acabou()){
                    System.out.println("FROM ControloConexaoServidor: Recebi " + fin.toString());
                    this.estado.setFase(4);
                    this.estado.acordaRecebe();
                }
            }
        }
        catch(IOException e){}
	}
}