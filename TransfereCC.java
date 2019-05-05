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

	private AgenteUDP agente;
	private Estado estado;
    private Buffer buffer;
    private Map<String,Estado> estados;         //Associa pares de ip e portas representadas por string a um estado
    private RecebeACKDireto rack;
    private Temporizador temp;

	TransfereCC(int bufferSize){
		this.buffer = new Buffer(bufferSize);
		this.estado = new Estado();
		this.agente = new AgenteUDP(7777);
        this.estados = new HashMap<>();
	}

	TransfereCC(InetAddress IPAddress, int bufferSize){
		this.buffer = new Buffer(bufferSize);
		this.estado = new Estado(IPAddress);
		this.agente = new AgenteUDP();

		for (int i = 0; i<10; i++) {
			//Envia o 1º syn que publica tamanho do buffer
			agente.send(new Pacote(false,true,false,false,false,new byte[0],bufferSize,0,0,7777,null,IPAddress));
			//Espera por uma confirmação durante algum tempo (em milissegundos) até reenviar
			Pacote p = agente.receive(50);
			if(p != null && true/*Integridade*/&& p.synAck()){
				this.estado.setFlowWindow(p.getWindow());
			    this.estado.setDestino(p.getIpOrigem());
			    this.estado.setPortaDestino(p.getPortaOrigem());
				//Envia um ACK ------ Isto é preciso se o servidor quiser enviar algo para o cliente, como o cliente envia sempre algo 1º, nao sei se é preciso
				break;
			}
			
		}
	}

	public byte[] read(int size){
		byte[] arr = null;
		boolean leu = false;
		while(!leu){					//Deve tentar sempre ler
			try{
				arr = buffer.read(size);
				leu = true;
			}catch(DadosAindaNaoRecebidos e){		//Publica o tamanho de janela atual
				agente.send(new Pacote(true,false,false,false,false,new byte[0],buffer.getAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino()));
			}
		}

		return arr;
	}


	public void write(byte[] fileContent, int bytesLidos){

		if(bytesLidos==-1){
			estado.setFinalAck(estado.getSeq());
        	agente.send(new Pacote(false,false,true,true,false,fileContent,buffer.getAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino()));
        	return;
		}

    	estado.esperaWindow(bytesLidos);			//Espera caso nao tenha espaço na janela

    	Pacote pacote = new Pacote(false,false,false,true,false,Arrays.copyOf(fileContent,bytesLidos),buffer.getAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino());
    	estado.addSeq(bytesLidos);

    	agente.send(pacote);
        estado.enviou(pacote);
    

	}



	public void get(String filename)throws UnknownHostException{
		
		//Criação e envio do pedido


		String request = "GET " + filename;
		Pacote pedido = new Pacote(false,false,false,false,true,request.getBytes(),buffer.getAvailableSpace(),0,0,estado.getPortaDestino(),null,estado.getDestino());
		boolean confirmado = false;
		while(!confirmado){
			agente.send(pedido);
			Pacote p = agente.receive(500);
			if(p!=null && /*I*/true && p.getAck() && p.getReq()){
				confirmado = true;
			}
		}
		GetClient gc = new GetClient(buffer,estado,agente);
		gc.start();
	}

	public void put(String filename)throws UnknownHostException{
		
		//Criação e envio do pedido


		String request = "PUT " + filename;
		Pacote pedido = new Pacote(false,false,false,false,true,request.getBytes(),buffer.getAvailableSpace(),0,0,estado.getPortaDestino(),null,estado.getDestino());
		boolean confirmado = false;
		while(!confirmado){
			agente.send(pedido);
			Pacote p = agente.receive(500);
			if(p!=null && /*I*/true && p.getAck() && p.getReq()){
				confirmado = true;
			}
		}
        this.estado.setSeq(0);

        //criar thread para gerir acks
        rack = new RecebeACKDireto(estado,agente);
        rack.start();
        //criar thread para gerir timeouts
        temp = new Temporizador(estado,this.agente);
        temp.start();
	}




	public void iniciaServidor() throws UnknownHostException, IOException{
		//Aceita conexoes, o seguinte deve ser numa thread a parte

		Pacote pedido = null;
		while(true){
			pedido = agente.receive();
			String intervenientes = pedido.getIntervenientes();
			if(/*Integridade*/true){
				if(pedido.getSyn() && !estados.containsKey(intervenientes)){
					Estado e = new Estado();
					e.setFlowWindow(pedido.getWindow());		//bytes disponiveis no buffer do recetor
			        e.setDestino(pedido.getIpOrigem());
			        e.setPortaDestino(pedido.getPortaOrigem());
					estados.put(intervenientes,e);
					agente.send(new Pacote(true,true,false,false,false,new byte[0],4000,0,0,e.getPortaDestino(),null,e.getDestino()));
					//cria thread
					ClientHandler ch = new ClientHandler(new Buffer(4000),e,agente);
					ch.start();
				}else{
					estados.get(intervenientes).redirecionaPacote(pedido);
				}
			}
		}

	}

}

class GetClient extends Thread{
	private Estado estado;
	private AgenteUDP agente;
	private Buffer buffer;


	GetClient(Buffer b,Estado e, AgenteUDP a){
		this.buffer = b;
		this.estado = e;
		this.agente = a;
	}

	public void run(){

		estado.setSeq(0);
		Pacote ack = new Pacote(true,false,false,false,false,new byte[0],buffer.getAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino());
		//Começa a receber o ficheiro
		TreeSet<Pacote> pacBuffer = new TreeSet<>((Pacote p1, Pacote p2) -> p1.getOffset()-p2.getOffset());
		boolean terminado = false;
		while(!terminado){
			//Pacote recebido = estado.getPacote();
			Pacote recebido = estado.receive();
			Pacote escrito = recebido;
			if(/*Verificação de integridade*/true && recebido.getPsh()){ //Verifica se esta dentro da janela
				
				while(recebido != null && estado.getSeq() == recebido.getOffset()){
					buffer.write(recebido.getDados());										//Extração e entrega à aplicação
					escrito = recebido;
					estado.addSeq(recebido.tamanhoDados());
					recebido = pacBuffer.pollFirst();
					System.out.println("prox: "+recebido);
				}if(recebido != null && estado.getSeq() < recebido.getOffset()){
					pacBuffer.add(recebido);
					System.out.println("Pacote adicionado: "+recebido);
				}
				ack = new Pacote(true,false,false,false,false,new byte[0],buffer.getAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino());
			}
			if(/*integridade*/true && escrito.pshFin()){
				break;
			}
			agente.send(ack);
		}


		//Término da conexao

		buffer.close();
	}
}

class ClientHandler extends Thread{
	private Estado estado;
	private AgenteUDP agente;
	private Buffer buffer;


	ClientHandler(Buffer b,Estado e, AgenteUDP a){
		this.buffer = b;
		this.estado = e;
		this.agente = a;
	}

	public void run(){

        try{
        	Pacote pedido;
			while(true){
				pedido = this.estado.receive();
				if(pedido.getReq()){
	        		agente.send(new Pacote(true,false,false,false,true,new byte[0],buffer.getAvailableSpace(),0,0,estado.getPortaDestino(),null,estado.getDestino()));

			        String [] filename = (new String(pedido.getDados())).split(" ");
					if(filename[0].equals("GET")){
						FileInputStream fis = new FileInputStream(filename[1]);
						BufferedInputStream bis = new BufferedInputStream(fis);
						get(bis);
					}else if(filename[0].equals("PUT")){
						FileOutputStream fos = new FileOutputStream("Teste/Recebi.txt");
						BufferedOutputStream bos = new BufferedOutputStream(fos);
						GetClient gc = new GetClient(buffer,estado,agente);
						gc.start();

						byte[] lido;
						while((lido = read(1000))!=null){ //Para ler indica-se o maximo de bytes a ler e recebe-se uma array de bytes
				            bos.write(lido);   
						}
						gc.join();
						bos.flush();
				        fos.close();
					}
					

				}if(pedido.pshFin()){
					agente.send(new Pacote(true,false,false,false,false,new byte[0],buffer.getAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino()));
				}
			}


        }catch(Exception exc){exc.printStackTrace();}
	}

	public byte[] read(int size){
		byte[] arr = null;
		boolean leu = false;
		while(!leu){					//Deve tentar sempre ler
			try{
				arr = buffer.read(size);
				leu = true;
			}catch(DadosAindaNaoRecebidos e){		//Publica o tamanho de janela atual
				agente.send(new Pacote(true,false,false,false,false,new byte[0],buffer.getAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino()));
			}
		}

		return arr;
	}

	void get(BufferedInputStream fis) throws IOException, InterruptedException{

	        int bytesLidos;
	        this.estado.setSeq(0);
	        byte[] fileContent = new byte[1000];
	        //criar thread para gerir acks
	        RecebeACK rack = new RecebeACK(estado);
	        rack.start();
	        //criar thread para gerir timeouts
	        Temporizador temp = new Temporizador(estado,this.agente);
	        temp.start();
	        //List<Pacote> listPac = new ArrayList<Pacote>();				//Quando recebe uma ACK deve ser retirado o pacote correspondente, talvez por no estado
	        bytesLidos = fis.read(fileContent);
	        while(bytesLidos != -1){
	    	estado.esperaWindow(bytesLidos);			//Espera caso nao tenha espaço na janela
	        	Pacote pacote = new Pacote(false,false,false,true,false,Arrays.copyOf(fileContent,bytesLidos),buffer.getAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino());
	        	estado.addSeq(bytesLidos);
	        	//listPac.add(pacote);
	        	bytesLidos = fis.read(fileContent);
	        	if(bytesLidos == -1) {
			        estado.setFinalAck(estado.getSeq());
	        	}
	        	agente.send(pacote);
	            estado.enviou(pacote);
	        }


	        fis.close();

	        agente.send(new Pacote(false,false,true,true,false,fileContent,buffer.getAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino()));

		    temp.join();
		    rack.join();

	}
}
