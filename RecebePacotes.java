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
	DatagramSocket socket;

	RecebePacotes(){
		this.estado = new Estado();
	}

	RecebePacotes(Estado e, DatagramPacket dp, DatagramSocket ds){
		this.estado = e;
		this.receivePacket = dp;
		this.socket = ds;
	}

	public void run(){
		//verificar o pacote ACk esta correto
        try{
            do{
        		byte[] receiveData = new byte[1024];
                Pacote psh = new Pacote();
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                socket.receive(receivePacket);
                psh.bytes2pacote(receivePacket.getData());
                if(psh.getPsh()){
                    System.out.println("FROM: RecebePacotes: Recebi " + psh.toString());
                    this.estado.addPacote(psh);
                    this.estado.addACK(psh.getOffset());
                    this.estado.acordaRecebe();
                }
                if(psh.pshFin()){
                    System.out.println("Recebi ultimo");
                    this.estado.setFase(3);
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