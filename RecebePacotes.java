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
                    //verificar se o pacote recebido é o que se pretende receber caso seja adicionar a lista de pacotes e o ACK passa a ser o mesmo
                    //Caso seja um antigo que falte receber adicionar a lista bem como todos os que estao a frente da lista e atualizar o ACK como maior offset que esteja correto
                    //Caso seja um que não estava a espera de receber adicionar a lista e enviar um ACK com o offset que esta em falta
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