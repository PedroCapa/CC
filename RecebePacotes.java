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
            while(this.estado.getFase() != 2){
                Pacote syn = new Pacote();
			    this.receivePacket = new DatagramPacket(receiveData, receiveData.length);
                this.serverSocket.receive(receivePacket);
                syn.bytes2pacote(receivePacket.getData()); // Verificar se a conexão foi estabelecida

                System.out.println("FROM: RecebePacotes: Recebi Syn ACK ACK " + syn.toString());
                if(syn.synAck()){
                    this.estado.setFase(2);
                    this.estado.acorda();
                }
            }
		}
		catch(IOException e){}
		//verificar o pacote ACk esta correto
        try{
            do{
                Pacote psh = new Pacote();
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                psh.bytes2pacote(receivePacket.getData());
                if(psh.getPsh()){
                    this.estado.addPacote(psh);
                    System.out.println("FROM: RecebePacotes: Recebi PSH " + psh.toString());
                // Caso seja o ultimo if(){ 
                    this.estado.setFase(3);
                    this.estado.acorda();
                // Caso seja o ultimo}
                }
            }
            while(this.estado.getFase() == 2);
        }
        catch(Exception e){
        }
		
        this.estado.acorda();
        try{
            while(this.estado.getFase() != 4){
                receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                Pacote fin = new Pacote();
                fin.bytes2pacote(receivePacket.getData());
                if(fin.acabou()){
                    System.out.println("FROM RecebePacotes: Recebi Fin " + fin.toString());
                    this.estado.setFase(4);
                    this.estado.acorda();
                }
            }
        }
        catch(IOException e){}
	}
}