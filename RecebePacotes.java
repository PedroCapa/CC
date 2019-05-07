import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicBoolean;

class RecebePacotes extends Thread{
    private Map<String,Estado> estados;
    private AgenteUDP agente;
    private int bufferSize;
    private AtomicBoolean trabalhar;


    public RecebePacotes(Map<String,Estado> e, AgenteUDP a, int bs){
        this.estados = e;
        this.agente = a;
        this.bufferSize = bs;
        this.trabalhar = new AtomicBoolean(true);
    }

    public void close(){
        this.trabalhar.set(false);
    }

    public void run(){
        Pacote pedido = null;
        while(true){
            pedido = agente.receive();
            if(!trabalhar.get()) {break;}
            String intervenientes = pedido.getIntervenientes();
            if(pedido.check()){
                if(pedido.getSyn() && (!estados.containsKey(intervenientes) || estados.get(intervenientes).isFin())){
                    Estado e = new Estado(pedido.getIpOrigem(),pedido.getPortaOrigem(),this.bufferSize,pedido.getWindow());
                    estados.put(intervenientes,e);
                    agente.send(new Pacote(true,true,false,false,false,new byte[0],this.bufferSize,0,0,e.getPortaDestino(),null,e.getDestino()));
                    //cria thread
                    ClientHandler ch = new ClientHandler(e,agente);
                    ch.start();

                }
                else{
                    estados.get(intervenientes).redirecionaPacote(pedido);
                }
            }
        }

    }
}
