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
            if(!trabalhar.get()) {System.out.println("desisto");break;}
            String intervenientes = pedido.getIntervenientes();
            if(/*Integridade*/true){
                if(pedido.getSyn() && (!estados.containsKey(intervenientes) || estados.get(intervenientes).isFin())){
                    Estado e = new Estado(pedido.getIpOrigem(),pedido.getPortaOrigem(),this.bufferSize,pedido.getWindow());
                    estados.put(intervenientes,e);
                    agente.send(new Pacote(true,true,false,false,false,new byte[0],this.bufferSize,0,0,e.getPortaDestino(),null,e.getDestino()));
                    //cria thread
                    ClientHandler ch = new ClientHandler(e,agente);
                    ch.start();
                }/*else if(pedido.getFin() && !pedido.getPsh() && estados.containsKey(intervenientes) && !estados.get(intervenientes).isFin()){ //Termina conexao deste lado quando receber fin do outro
                    FinalizaConexao fc = new FinalizaConexao(estados.get(intervenientes),agente,bufferSize);System.out.println("bamos");
                    fc.start();
                }*/
                else{
                    estados.get(intervenientes).redirecionaPacote(pedido);System.out.println("pisga");
                }
            }
        }

    }
}

class FinalizaConexao extends Thread{
    private Estado estado;
    private AgenteUDP agente;
    private int bufferSize;

    public FinalizaConexao(Estado e, AgenteUDP a, int bs){
        this.estado = e;
        this.agente = a;
        this.bufferSize = bs;
    }


    public void run(){
        //Envia finAck para informar que recebeu o aviso e que também terminou deste lado
        estado.terminaConexao();
        Pacote resposta = new Pacote(true,false,true,false,false,new byte[0],this.bufferSize,0,0,estado.getPortaDestino(),null,estado.getDestino());
        int i = 0;
        while(i<5){
            agente.send(resposta);
            Pacote p = estado.receive(200);System.out.println("XO:"+p);
            if(p!=null && p.finAck()){
                break;                                      //Conexao Terminada
            }
            if(p==null){
                i++;                        //Se nao receber pacotes durante 1 segundo, assume a conexao como terminada
            }
        }
    }
}


