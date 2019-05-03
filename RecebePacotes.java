import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class RecebePacotes extends Thread{
	Estado estado;
    AgenteUDP agente;


    public RecebePacotes(Estado e, AgenteUDP a){
        this.estado = e;
        this.agente = a;
    }


    public void run(){
        //Inicio da conexao


        while(estado.transferir()){
            Pacote pacote = agente.receive();
            if(/*Verifica integridade*/true){
                if(pacote.getAck()){
                    estado.setLastAck(pacote);
                }
                if(pacote.getPsh()){

                }
            }
        }






        //Fim da conexao
    }

























    /*
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
                Pacote psh = recebeDados();                
                if(psh.getPsh()){
                    System.out.println("FROM: RecebePacotes: Recebi " + psh.toString());
                    //verificar se o pacote recebido é o que se pretende receber caso seja adicionar a lista de pacotes e o ACK passa a ser o mesmo
                    if((psh.getOffset() == 0 && this.estado.getAdiantados().size() == 0) || (this.estado.getPacotes().size() > 0 && this.estado.seguinte(psh.getOffset()) && this.estado.getAdiantados().size() == 0)){
                        this.estado.addPacote(psh);
                        this.estado.atualizaACK();
                    }
                    //Caso seja um antigo que falte receber adicionar a lista bem como todos os que estao a frente da lista e atualizar o ACK como maior offset que esteja correto
                    else if((psh.getOffset() == 0 && this.estado.getAdiantados().size() != 0) 
                    || (this.estado.getPacotes().size() > 0 && this.estado.seguinte(psh.getOffset()) && this.estado.getAdiantados().size() != 0)){
                        this.estado.addPacote(psh);
                        this.estado.atualizaAdiantados();
                        this.estado.atualizaACK();
                    }
                    //Caso seja repetido
                    else if (this.estado.eRepetido(psh.getOffset()));
                    //Caso seja um que não estava a espera de receber adicionar a lista e enviar um ACK com o offset que esta em falta
                    else{
                        this.estado.addAdiantados(psh);
                    }
                    this.estado.acordaRecebe();
                }
                if(psh.pshFin() && this.estado.getAdiantados().size() == 0){
                    System.out.println("Recebi ultimo");
                    this.estado.setFase(3);
                }
            }
            while(this.estado.getFase() == 2);
        }
        catch(Exception e){
        }
                    System.out.println("Recebi tudo");
        this.estado.acordaRecebe();
	}

    public Pacote recebeDados(){
        Pacote psh = new Pacote();
        try{
            byte[] receiveData = new byte[1024];
            this.receivePacket = new DatagramPacket(receiveData, receiveData.length);
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