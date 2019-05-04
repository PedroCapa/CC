import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class RecebeACK extends Thread{
	private Estado estado;
	private AgenteUDP agente;


	RecebeACK(Estado e, AgenteUDP a){
		this.estado = e;
		this.agente = a;
	}

	public void run(){
		while(estado.transferir()){				//Enquanto ainda se estiver na fase de transferencia
			Pacote pacote = agente.receive();
			if(/*Verifica integridade*/true){
				estado.setLastAck(pacote);
			}
		}
	}



























	/*
	DatagramSocket socket;

	public void run(){
		int last = -1;
		int x = 0;
		while(last <= this.estado.getNumero()){// Nao recebeu todos os ACK dos ficheiros vai continuar no ciclo
      		Pacote p = recebeDados();
		    if(p.pshAck() && p.getOffset() >= this.estado.getACK()){
		      	System.out.println("FROM: RecebeACK: " + p);
      			this.estado.setACK(p.getOffset());
      			last = p.getOffset();
		    }
      		this.estado.acordaRecebe();
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

	public Pacote recebeDados(){
        Pacote psh = new Pacote();
		try{
			byte[] receiveData = new byte[1024];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
      		this.socket.receive(receivePacket);
      		psh.bytes2pacote(receivePacket.getData());
		}
		catch(IOException e){
			System.out.println(e.getMessage());
		}finally{
			return psh;
		}
    }*/
}
