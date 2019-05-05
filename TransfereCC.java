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


import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class TransfereCC{

	private AgenteUDP agente;
	private Estado estado;
    private Buffer buffer;

	TransfereCC(int bufferSize){
		this.buffer = new Buffer(bufferSize);
		this.estado = new Estado();
		this.agente = new AgenteUDP(estado,7777);
	}

	TransfereCC(InetAddress IPAddress, int bufferSize){
		this.buffer = new Buffer(bufferSize);
		this.estado = new Estado(IPAddress);
		this.agente = new AgenteUDP(estado);

		for (int i = 0; i<10; i++) {
			//Envia o 1º syn que publica tamanho do buffer
			agente.send(new Pacote(false,true,false,false,false,new byte[0],bufferSize,0));
			//Espera por uma confirmação durante algum tempo (em milissegundos) até reenviar
			Pacote p = agente.receive(50);
			if(p != null && true/*Integridade*/&& p.synAck()){
				this.estado.setFlowWindow(p.getWindow());
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
				agente.send(new Pacote(true,false,false,false,false,new byte[0],buffer.getAvailableSpace(),estado.getLastAck()));
			}
		}

		return arr;
	}


	public void get(String filename)throws UnknownHostException{
		
		//Criação e envio do pedido


		String request = "GET " + filename;
		Pacote pedido = new Pacote(false,false,false,false,true,request.getBytes(),buffer.getAvailableSpace(),0);
		boolean confirmado = false;
		while(!confirmado){
			agente.send(pedido);
			confirmado =true;// estado.esperaConfirmacao(100);			//Substituir por agente.receive() e fazer aqui a confirmaçao
		}
		GetClient gc = new GetClient(buffer,estado,agente);
		gc.start();


	}



	public void iniciaServidor() throws UnknownHostException, IOException{
		//Aceita conexoes, o seguinte deve ser numa thread a parte

		Pacote pedido = null;
		while(true){
			pedido = agente.accept();
			if(/*Integridade*/true && pedido.getSyn()){
				estado.setFlowWindow(pedido.getWindow());		//bytes disponiveis no buffer do recetor
				agente.send(new Pacote(true,true,false,false,false,new byte[0],buffer.getAvailableSpace(),0));
				break;
			}
		}

		while(true){
			pedido = this.agente.receive();
			if(/*Inte*/ true && pedido.getReq()){
				break;
			}
		}


        String [] filename = (new String(pedido.getDados())).split(" ");
		//Verifica se é GET
		System.out.println("-"+filename[1]+"-"+filename[0]+"-");
        FileInputStream fis = new FileInputStream(filename[1]);
        int bytesLidos,seq = 0;
        byte[] fileContent = new byte[1000];
        //criar thread para gerir acks
        RecebeACK rack = new RecebeACK(estado,this.agente);
        rack.start();
        //criar thread para gerir timeouts
        Temporizador temp = new Temporizador(estado,this.agente);
        temp.start();
        //List<Pacote> listPac = new ArrayList<Pacote>();				//Quando recebe uma ACK deve ser retirado o pacote correspondente, talvez por no estado
        bytesLidos = fis.read(fileContent);
        while(bytesLidos != -1){
    	estado.esperaWindow(seq+bytesLidos);			//Espera caso nao tenha espaço na janela
        	Pacote pacote = new Pacote(false,false,false,true,false,Arrays.copyOf(fileContent,bytesLidos),buffer.getAvailableSpace(),seq);
        	seq += bytesLidos;
        	//listPac.add(pacote);
        	bytesLidos = fis.read(fileContent);
        	if(bytesLidos == -1) {
		        estado.setFinalAck(seq);
        	}
        	agente.send(pacote);
            estado.enviou(pacote);
        }


        fis.close();



        agente.send(new Pacote(false,false,true,false,false,fileContent,buffer.getAvailableSpace(),seq));

        try{
	        temp.join();
	        rack.join();
        }catch(InterruptedException exc){}
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

		int seq = 0;
		Pacote ack = new Pacote(true,false,false,false,false,new byte[0],buffer.getAvailableSpace(),seq);
		//Começa a receber o ficheiro
		TreeSet<Pacote> pacBuffer = new TreeSet<>((Pacote p1, Pacote p2) -> p1.getOffset()-p2.getOffset());
		boolean terminado = false;
		while(!terminado){
			//Pacote recebido = estado.getPacote();
			Pacote recebido = agente.receive();
			Pacote escrito = recebido;
			if(/*Verificação de integridade*/true && recebido.getPsh()){ //Verifica se esta dentro da janela
				
				while(recebido != null && seq == recebido.getOffset()){
					buffer.write(recebido.getDados());										//Extração e entrega à aplicação
					escrito = recebido;
					seq += recebido.tamanhoDados();
					recebido = pacBuffer.pollFirst();
					System.out.println("prox: "+recebido);
				}if(recebido != null && seq < recebido.getOffset()){
					pacBuffer.add(recebido);
					System.out.println("Pacote adicionado: "+recebido);
				}
				ack = new Pacote(true,false,false,false,false,new byte[0],buffer.getAvailableSpace(),seq);
			}
			if(/*integridade*/true && escrito.getFin()){
				break;
			}
			agente.send(ack);
		}


		//Término da conexao

		buffer.close();
	}
}
