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
            if(!trabalhar.get()) break;
            String intervenientes = pedido.getIntervenientes();
            if(/*Integridade*/true){
                if(pedido.getSyn() && (!estados.containsKey(intervenientes) || estados.get(intervenientes).isFin())){
                    Estado e = new Estado(pedido.getIpOrigem(),pedido.getPortaOrigem(),this.bufferSize,pedido.getWindow());
                    estados.put(intervenientes,e);
                    agente.send(new Pacote(true,true,false,false,false,new byte[0],this.bufferSize,0,0,e.getPortaDestino(),null,e.getDestino()));
                    //cria thread
                    ClientHandler ch = new ClientHandler(e,agente);
                    ch.start();
                }else if(pedido.getFin() && !pedido.getPsh() && estados.containsKey(intervenientes) && !estados.get(intervenientes).isFin()){ //Termina conexao deste lado quando receber fin do outro
                    FinalizaConexao fc = new FinalizaConexao(estados.get(intervenientes),agente,bufferSize);
                    fc.start();
                }
                else{
                    estados.get(intervenientes).redirecionaPacote(pedido);
                }
            }
        }

        //Quando o método close do TransfereCC for chamado

        //Mandar FINs para os existentes nos estados

        for (Map.Entry<String, Estado> entry : estados.entrySet()){
            Estado e = entry.getValue();
            e.terminaConexao();
            int i = 0;
            Pacote p = new Pacote(false,false,true,false,false,new byte[0],this.bufferSize,0,0,e.getPortaDestino(),null,e.getDestino());
            while(i<5){
                agente.send(p);
                Pacote recebido = agente.receive(200);              //Se estiver mais de 1 segundo sem resposta, desiste e considera fechada
                if(recebido == null){
                    i++;
                }else if(recebido.getIntervenientes().equals(entry.getKey()) && recebido.getFin() && recebido.getAck()){
                    p = new Pacote(true,false,true,false,false,new byte[0],this.bufferSize,0,0,e.getPortaDestino(),null,e.getDestino());
                    agente.send(p);
                    while(true){
                        recebido = agente.receive(500);
                        if(recebido == null) break;
                        if(recebido.getIntervenientes().equals(entry.getKey()) && recebido.getFin() && recebido.getAck()){
                            agente.send(p);                                         //Caso volte a receber finack, volta a enviar este
                        }
                    }
                    break;
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
            Pacote p = estado.receive(200);
            if(p!=null && p.getFin() && p.getAck()){
                break;                                      //Conexao Terminada
            }
            if(p==null){
                i++;                        //Se nao receber pacotes durante 1 segundo, assume a conexao como terminada
            }
        }
    }
}


