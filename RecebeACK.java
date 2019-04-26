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
		int last = -1;
		int x = 0;
		while(this.estado.getNumero() == -1 || last <= this.estado.getNumero()){// Nao recebeu todos os ACK dos ficheiros vai continuar no ciclo
      		try{
				byte[] receiveData = new byte[1024];
      			Pacote p = new Pacote();
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      			this.socket.receive(receivePacket);
      			p.bytes2pacote(receivePacket.getData());
		      	if(p.pshAck() && p.getOffset() >= this.estado.getACK()){
		      		System.out.println("FROM: RecebeACK: " + p);
      				this.estado.setACK(p.getOffset());
      				last = p.getOffset();
		      	}
      			this.estado.acordaRecebe();
      		}
      		catch(IOException e){
      			System.out.println(e);
      		}
		}
        this.estado.setFase(3);
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