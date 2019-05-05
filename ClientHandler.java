import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.io.FileInputStream;

class ClientHandler extends Thread{
	private Estado estado;
	private AgenteUDP agente;


	ClientHandler(Estado e, AgenteUDP a){
		this.estado = e;
		this.agente = a;
	}

	public void run(){

        try{
        	Pacote pedido;
			while(true){
				pedido = this.estado.receive();
				if(pedido.getReq()){
	        		agente.send(new Pacote(true,false,false,false,true,new byte[0],estado.bufferAvailableSpace(),0,0,estado.getPortaDestino(),null,estado.getDestino()));

			        String [] filename = (new String(pedido.getDados())).split(" ");
					if(filename[0].equals("GET")){
						FileInputStream fis = new FileInputStream(filename[1]);
						BufferedInputStream bis = new BufferedInputStream(fis);
						get(bis);
					}else if(filename[0].equals("PUT")){
						FileOutputStream fos = new FileOutputStream("Teste/Recebi.txt");
						BufferedOutputStream bos = new BufferedOutputStream(fos);
						GetClient gc = new GetClient(estado,agente);
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
					agente.send(new Pacote(true,false,false,false,false,new byte[0],estado.bufferAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino()));
				}
			}


        }catch(Exception exc){exc.printStackTrace();}
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
	        	Pacote pacote = new Pacote(false,false,false,true,false,Arrays.copyOf(fileContent,bytesLidos),estado.bufferAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino());
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

	        agente.send(new Pacote(false,false,true,true,false,fileContent,estado.bufferAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino()));

		    temp.join();
		    rack.join();

	}
}
