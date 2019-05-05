import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class RecebePacotes extends Thread{
	private Map<String,Estado> estados;
    private AgenteUDP agente;
    private int bufferSize;


    public RecebePacotes(Map<String,Estado> e, AgenteUDP a, int bs){
        this.estados = e;
        this.agente = a;
        this.bufferSize = bs;
    }


    public void run(){
        Pacote pedido = null;
        while(true){
            pedido = agente.receive();
            String intervenientes = pedido.getIntervenientes();
            if(/*Integridade*/true){
                if(pedido.getSyn() && !estados.containsKey(intervenientes)){
                    Estado e = new Estado(pedido.getIpOrigem(),pedido.getPortaOrigem(),this.bufferSize,pedido.getWindow());
                    estados.put(intervenientes,e);
                    agente.send(new Pacote(true,true,false,false,false,new byte[0],this.bufferSize,0,0,e.getPortaDestino(),null,e.getDestino()));
                    //cria thread
                    ClientHandler ch = new ClientHandler(e,agente);
                    ch.start();
                }else{
                    estados.get(intervenientes).redirecionaPacote(pedido);
                }
            }
        }

    }
}