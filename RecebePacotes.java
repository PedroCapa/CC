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
		try{
			Pacote syn = new Pacote();
			this.receivePacket = new DatagramPacket(receiveData, receiveData.length);
        	this.serverSocket.receive(receivePacket);
        	syn.bytes2pacote(receivePacket.getData()); // Verificar se a conexão foi estabelecida

        	System.out.println("FROM: RecebePacotes: Recebi Syn ACK ACK" + syn.toString() + 2);
        	this.estado.setFase(2);
        	this.estado.acorda();
		}
		catch(IOException e){}
		//verificar o pacote ACk esta correto

        //Após a conexão
/*
        Pacote psh = new Pacote();
        do{
            receivePacket = new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            psh.bytes2pacote(receivePacket.getData());
            serverSocket.send(sendPacket);
            //Adicionar ao estado dos pacotes recebidos e mandar ACK no caso de ser psh

            System.out.println(psh.toString());
        }
        while(psh.getPsh() == true);
*/		
        this.estado.setFase(3);
        this.estado.acorda();
        try{
        	receivePacket = new DatagramPacket(receiveData, receiveData.length);
        	serverSocket.receive(receivePacket);
        	Pacote fin = new Pacote();
        	fin.bytes2pacote(receivePacket.getData());
        	System.out.println("FROM RecebePacotes: Recebi Fin" + fin.toString() + 3);
        	this.estado.setFase(4);
            this.estado.acorda();
        }
        catch(IOException e){}
	}
}