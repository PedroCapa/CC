import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class RecebeACK extends Thread{
	Estado estado;
	Condition espera;
	DatagramSocket clientSocket;

	public void run(){
		byte[] receiveData = new byte[1024];

		//Receber o ACK do incio de conexão
	    try{
	    	DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
    		this.clientSocket.receive(receivePacket);
      		Pacote incio = new Pacote();
      		incio.bytes2pacote(receivePacket.getData());
      		System.out.println("FROM SERVER:" + incio);
			//Caso se tenha recebido acordar o UDPClient
			this.espera.signalAll();
	    }
	    catch(IOException e){

	    }

		while(this.estado.getACK().size() < this.estado.getNumero()){// Nao recebeu todos os ACK dos ficheiros vai continuar no ciclo
      		try{
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      			this.clientSocket.receive(receivePacket);
      			Pacote p = new Pacote();
      			p.bytes2pacote(receivePacket.getData());
      			if(Integer.compare(this.estado.getACK().get(this.estado.getACK().size() - 1), p.getOffset()) == 0){ // Caso o ACK recebido seja repetido
      				this.estado.setFase(4);
      			}
      			else{
      				this.estado.addACK(p.getOffset());
      			}
      		}
      		catch(IOException e){}
		}
		try{
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        	this.clientSocket.receive(receivePacket);
        	Pacote fim = new Pacote();
        	fim.bytes2pacote(receivePacket.getData());
        	System.out.println(fim);
			this.espera.signalAll();
		}
		catch(IOException e){}
	}

	RecebeACK(){
		this.estado = new Estado(); 
	}

	RecebeACK(Estado e, Condition c, DatagramSocket dp){
		this.estado = e;
		this.espera = c;
		this.clientSocket = dp;
	}

}