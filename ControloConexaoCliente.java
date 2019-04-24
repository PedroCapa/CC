import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class ControloConexaoCliente extends Thread{
	Estado estado;
	DatagramSocket clientSocket;

	public void run(){
		byte[] receiveData = new byte[1024];

		//Receber o ACK do incio de conexão
	    try{
	    	while(this.estado.getFase() != 2){
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    			this.clientSocket.receive(receivePacket);
      			Pacote incio = new Pacote();
      			incio.bytes2pacote(receivePacket.getData());
      			System.out.println("FROM ControloConexaoCliente: Recebi " + incio);
      			if(incio.synAck()){
					this.estado.setFase(2);
      			}
	    	}
	    }
	    catch(IOException e){}

		//Colocar thread a dormir para recebePacotes o acordar
		this.estado.esperaControlo();

        this.estado.setFase(3);
        this.estado.acordaRecebe();
		try{
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        	this.clientSocket.receive(receivePacket);
        	Pacote fim = new Pacote();
        	fim.bytes2pacote(receivePacket.getData());
        	if(fim.finAck()){
        		System.out.println("FROM ControloConexaoCliente: Recebi " + fim.toString());
            	this.estado.setFase(4);
				this.estado.acordaRecebe();
        	}
        		
		}
		catch(IOException e){}
	}

	ControloConexaoCliente(){
		this.estado = new Estado(); 
	}

	ControloConexaoCliente(Estado e, DatagramSocket dp){
		this.estado = e;
		this.clientSocket = dp;
	}

}