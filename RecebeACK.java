import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class RecebeACK extends Thread{
	Estado estado;
	DatagramSocket socket;

	public void run(){
		byte[] receiveData = new byte[1024];
		while(this.estado.getACK().size() < this.estado.getNumero()){// Nao recebeu todos os ACK dos ficheiros vai continuar no ciclo
      		try{
      			Pacote p = new Pacote();
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      			this.socket.receive(receivePacket);
      			p.bytes2pacote(receivePacket.getData());
		      	if(p.pshAck()){
			      	System.out.println("FROM RecebeACK: Receber " + p);
      				if(this.estado.getACK().size() > 0 && Integer.compare(this.estado.getACK().get(this.estado.getACK().size() - 1), p.getOffset()) == 0){ // Caso o ACK recebido seja repetido
      					this.estado.setFase(4);
      					this.estado.acordaRecebe();
      				}
      				else{
      					this.estado.addACK(p.getOffset());
      				}
		      	}
      		}
      		catch(IOException e){
      			System.out.println(e);

      		}
		}
        this.estado.setFase(2);
        this.estado.acordaRecebe();
	}

	RecebeACK(){
		this.estado = new Estado(); 
	}

	RecebeACK(Estado e, DatagramSocket dp){
		this.estado = e;
		this.socket = dp;
	}

}