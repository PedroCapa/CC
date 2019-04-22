import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class RecebePacotes extends Thread{
	Estado estado;
	DatagramPacket receivePacket;
	DatagramSocket serverSocket;

	RecebePacotes(){
		this.estado = new Estado();
	}

	RecebePacotes(Estado e, DatagramPacket dp, DatagramSocket ds){
		this.estado = e;
		this.receivePacket = dp;
		this.serverSocket = ds;
	}

	public void run(){
		byte[] receiveData = new byte[10024];
		
		//verificar o pacote ACk esta correto
        try{
            do{
                Pacote psh = new Pacote();
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                psh.bytes2pacote(receivePacket.getData());
                if(psh.getPsh()){
                    this.estado.addPacote(psh);
                    System.out.println("FROM: RecebePacotes: Recebi " + psh.toString());
                }
                if(psh.pshFin()){ 
                    this.estado.setFase(3);
                    this.estado.acordaRecebe();
                }
            }
            while(this.estado.getFase() == 2);
        }
        catch(Exception e){
        }
		
        //Acordar thread de controlo de conexao
        this.estado.acordaRecebe();
        
	}
}