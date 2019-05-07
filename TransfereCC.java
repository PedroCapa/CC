import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.*;
import java.util.Set;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Map;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;


import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class TransfereCC{

	private Estado estado;					//Estado para o cliente; útil porque o cliente está desenhado para ter apenas uma conexao e assim não é preciso estar sempre a ir buscar a conexao à lista de estados
	private AgenteUDP agente;
    private Buffer buffer;
    private Map<String,Estado> estados;         //Associa pares de ip e portas representadas por string a um estado
    private RecebeACK rack;
    private Temporizador temp;
    private RecebePacotes rp;

	TransfereCC(int port){
		this.agente = new AgenteUDP(port);
        this.estados = new HashMap<>();
	}

	TransfereCC(){
		this.estados = new HashMap<>();
		this.agente = new AgenteUDP();
	}

	public void connect(InetAddress IPAddress, int bufferSize) throws ConexaoNaoEstabelecidaException{
		Estado estado = null;
		int i;
		for (i = 0; i<10; i++) {
			//Envia o 1º syn que publica tamanho do buffer
			agente.send(new Pacote(false,true,false,false,false,new byte[0],bufferSize,0,0,7777,null,IPAddress));
			//Espera por uma confirmação durante algum tempo (em milissegundos) até reenviar
			Pacote p = agente.receive(100);
			if(p != null && p.check() && p.synAck()){
			    estado = new Estado(p.getIpOrigem(),p.getPortaOrigem(),bufferSize,p.getWindow());
				this.estados.put(p.getIntervenientes(),estado);

				//Envia um ACK ------ Isto é preciso se o servidor quiser enviar algo para o cliente, como o cliente envia sempre algo 1º, nao sei se é preciso
				break;
			}
			
		}
		if(i==10) throw new ConexaoNaoEstabelecidaException("Não foi possível etabelecer uma conexão a "+IPAddress+" depois de "+i+" tentativas");
		this.estado = estado;
		this.rp = new RecebePacotes(this.estados,this.agente,bufferSize);
		rp.start();
	}

	public byte[] read(int size){
		byte[] arr = null;
		boolean leu = false;
		while(!leu){					//Deve tentar sempre ler
			try{
				arr = estado.readBuffer(size);
				leu = true;
			}catch(DadosAindaNaoRecebidos e){		//Publica o tamanho de janela atual
				agente.send(new Pacote(true,false,false,false,false,new byte[0],estado.bufferAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino()));
			}
		}

		return arr;
	}

	/**Envia e indica que o ficheiro a transferir terminou*/
	public void writeFin(byte[] fileContent, int bytesLidos){
		estado.esperaWindow(bytesLidos);			//Espera caso nao tenha espaço na janela

    	Pacote pacote = new Pacote(false,false,true,true,false,Arrays.copyOf(fileContent,bytesLidos),estado.bufferAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino());
    	estado.addSeq(bytesLidos);
		
		estado.setFinalAck(estado.getSeq());
    	
    	agente.send(pacote);
        estado.enviou(pacote);

        try{
        	rack.join();
        	temp.join();
        }catch(InterruptedException exc){}
	}

	public void write(byte[] fileContent, int bytesLidos){


    	estado.esperaWindow(bytesLidos);			//Espera caso nao tenha espaço na janela

    	Pacote pacote = new Pacote(false,false,false,true,false,Arrays.copyOf(fileContent,bytesLidos),estado.bufferAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino());
    	estado.addSeq(bytesLidos);

    	estado.enviou(pacote);
	agente.send(pacote);
    

	}



	public void get(String filename)throws UnknownHostException{
		
		//Criação e envio do pedido


		String request = "GET " + filename;
		Pacote pedido = new Pacote(false,false,false,false,true,request.getBytes(),estado.bufferAvailableSpace(),0,0,estado.getPortaDestino(),null,estado.getDestino());
		boolean confirmado = false;
		while(!confirmado){
			agente.send(pedido);
			Pacote p = estado.receive(500);
			if(p!=null && p.check() && p.getAck() && p.getReq()){
				confirmado = true;
			}
		}
		GetClient gc = new GetClient(estado,agente);
		gc.start();
	}

	public void put(String filename)throws UnknownHostException{
		
		//Criação e envio do pedido


		String request = "PUT " + filename;
		Pacote pedido = new Pacote(false,false,false,false,true,request.getBytes(),estado.bufferAvailableSpace(),0,0,estado.getPortaDestino(),null,estado.getDestino());
		boolean confirmado = false;
		while(!confirmado){
			agente.send(pedido);
			Pacote p = estado.receive(500);
			if(p!=null && p.check() && p.getAck() && p.getReq()){
				confirmado = true;
			}
		}
		this.estado.reset();

        //criar thread para gerir acks
        rack = new RecebeACK(estado);
        rack.start();
        //criar thread para gerir timeouts
        temp = new Temporizador(estado,this.agente);
        temp.start();
	}

	public void disconnect(){
		estado.terminaConexao();
        int i = 0;
        Pacote p = new Pacote(false,false,true,false,false,new byte[0],this.estado.bufferAvailableSpace(),0,0,estado.getPortaDestino(),null,estado.getDestino());
        while(i<5){
            agente.send(p);						//Diz que pretende terminar conexao
            Pacote recebido = estado.receive(200);              //Se estiver mais de 1 segundo sem resposta, desiste e considera a conexao fechada
            if(recebido == null){
                i++;
            }else if(recebido.finAck()){
                p = new Pacote(true,false,true,false,false,new byte[0],this.estado.bufferAvailableSpace(),0,0,estado.getPortaDestino(),null,estado.getDestino());
                while(true){
                	agente.send(p);
                    recebido = estado.receive(500);
                    if(recebido == null) break;
                }
                break;
            }
        }
	}

	public void close(){
		this.rp.close();System.out.println("AHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHHH");
		this.agente.close();
		try{
			this.rp.join();
		}catch(InterruptedException e){
		}
	}


	public void iniciaServidor(){
		
		RecebePacotes rp = new RecebePacotes(estados,agente,4000);
		rp.start();
		try{
			rp.join();
		}catch(InterruptedException exc){}
	}


}

class GetClient extends Thread{
	private Estado estado;
	private AgenteUDP agente;


	GetClient(Estado e, AgenteUDP a){
		this.estado = e;
		this.agente = a;
	}

	public void run(){

		estado.setSeq(0);
		Pacote ack = new Pacote(true,false,false,false,false,new byte[0],estado.bufferAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino());
		//Começa a receber o ficheiro
		TreeSet<Pacote> pacBuffer = new TreeSet<>((Pacote p1, Pacote p2) -> p1.getOffset()-p2.getOffset());
		boolean terminado = false;
		while(!terminado){
			//Pacote recebido = estado.getPacote();
			Pacote recebido = estado.receive();
			Pacote escrito = null;
			if(recebido.getPsh()){ //Verifica se esta dentro da janela
				
				while(recebido != null && estado.getSeq() == recebido.getOffset()){
					estado.writeBuffer(recebido.getDados());										//Extração e entrega à aplicação
					escrito = recebido;
					estado.addSeq(recebido.tamanhoDados());
					recebido = pacBuffer.pollFirst();
					System.out.println("prox: "+recebido);
				}if(recebido != null && estado.getSeq() < recebido.getOffset()){				//Caso haja receções fora de ordem
					pacBuffer.add(recebido);
					System.out.println("Pacote adicionado: "+recebido);
				}
				ack = new Pacote(true,false,false,false,false,new byte[0],estado.bufferAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino());
			}
			if(escrito!=null && escrito.pshFin()){
				terminado = true;
			}
			agente.send(ack);
		}


		//Término da conexao

		estado.closeBuffer();
	}
}
