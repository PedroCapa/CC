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
    private InetAddress IPAddress;
    private byte[] buffer;
    private int bCursor;
    private int bRemaining;
    private Lock lock;

	TransfereCC(){}

	TransfereCC(InetAddress IPAddress, int bufferSize){
		this.IPAddress = IPAddress;
		this.buffer = new byte[bufferSize];
		this.bCursor = 0;
		this.bRemaining = 0;
		this.lock = new ReentrantLock();
	}

	public int getAvailableSpaceInBuffer(){
		lock.lock();
		int ret = buffer.length - bRemaining;
		lock.unlock();
		return ret;
	}
	public byte[] read(int size){
		lock.lock();

		int bytesLidos = Math.min(bRemaining,size);		//Qt de bytes que deve ler
		byte[] arr = new byte[bytesLidos];
		int tamanhoRestante = buffer.length - bCursor;			//bytes até ao final do array do buffer
		if(bytesLidos < tamanhoRestante){
			System.arraycopy(buffer,bCursor,arr,0,bytesLidos);
			bCursor += bytesLidos;
			bRemaining -= bytesLidos;
		}
		else{
			System.arraycopy(buffer,bCursor,arr,0,tamanhoRestante);
			System.arraycopy(buffer,0,arr,tamanhoRestante,bytesLidos - tamanhoRestante);
			bCursor = bytesLidos - tamanhoRestante;
			bRemaining -= bytesLidos;
		}

		lock.unlock();
		return arr;
	}

	public void write(byte[] arr){
		lock.lock();

		int cursorEscrita = (bCursor + bRemaining) % buffer.length; //Determina onde devem ser escritos os bytes
		int tamanhoRestante = buffer.length - cursorEscrita;		//bytes disponiveis até ao final do array do buffer
		if(arr.length < tamanhoRestante){
			System.arraycopy(arr,0,buffer,cursorEscrita,arr.length);
			bRemaining += arr.length;
		}
		else{
			System.arraycopy(arr,0,buffer,cursorEscrita,tamanhoRestante);
			System.arraycopy(arr,tamanhoRestante,buffer,0,arr.length - tamanhoRestante);
			bRemaining += arr.length;
		}

		lock.unlock();
	}



	public void get(String filename)throws UnknownHostException{
		Estado estado = new Estado(InetAddress.getLocalHost(),IPAddress,0,7777);
		this.agente = new AgenteUDP(estado);
		
		//Criação e envio do pedido
		String request = "GET " + filename;
		Pacote pedido = new Pacote(false,false,false,false,true,request.getBytes(),getAvailableSpaceInBuffer(),0,"lol","lol");
		boolean confirmado = false;
		while(!confirmado){
			agente.send(pedido);
			confirmado =true;// estado.esperaConfirmacao(100);			//Substituir por agente.receive() e fazer aqui a confirmaçao
		}

		int seq = 0;
		Pacote ack = new Pacote(true,false,false,false,false,new byte[0],getAvailableSpaceInBuffer(),seq,"lol","lol");
		//Começa a receber o ficheiro
		TreeSet<Pacote> pacBuffer = new TreeSet<>((Pacote p1, Pacote p2) -> p1.getOffset()-p2.getOffset());
		boolean terminado = false;
		while(!terminado){System.out.println(seq);
			//Pacote recebido = estado.getPacote();
			Pacote recebido = agente.receive();
			if(/*Verificação de integridade*/true && recebido.getPsh() && seq+buffer.length>=recebido.getOffset()+recebido.tamanhoDados()){ //Verifica se esta dentro da janela
				Pacote escrever = recebido;
				while(escrever != null && seq == escrever.getOffset()){
					write(escrever.getDados());										//Extração e entrega à aplicação
					seq += recebido.tamanhoDados();
					escrever = pacBuffer.pollFirst();
				}if(escrever != null){
					pacBuffer.add(escrever);
				}
				ack = new Pacote(true,false,false,false,false,new byte[0],getAvailableSpaceInBuffer(),seq,"lol","lol");
			}
			if(/*integridade*/true && recebido.getFin()){
				break;
			}
			agente.send(ack);
		}


		//Término da conexao
	}



	public void iniciaServidor() throws UnknownHostException, IOException{
		//Aceita conexoes, o seguinte deve ser numa thread a parte



		Estado estado = new Estado(InetAddress.getLocalHost(),null,7777,0);
		this.agente = new AgenteUDP(estado);
		Pacote pedido = agente.accept();

		//Pacote pedido = estado.esperaPedido();
        String [] filename = (new String(pedido.getDados())).split(" ");
		//Verifica se é GET
		estado.setFlowWindow(pedido.getWindow());		//bytes disponiveis no buffer do recetor

        FileInputStream fis = new FileInputStream("Teste/Teste.txt");
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
        while(bytesLidos != -1){System.out.println(bytesLidos+"  "+pedido.getWindow());
        	estado.esperaWindow(seq+bytesLidos);			//Espera caso nao tenha espaço na janela
        	Pacote pacote = new Pacote(false,false,false,true,false,Arrays.copyOf(fileContent,bytesLidos),getAvailableSpaceInBuffer(),seq,"lol","lol");
        	seq += bytesLidos;
        	//listPac.add(pacote);
        	agente.send(pacote);
            estado.enviou(pacote);
        	bytesLidos = fis.read(fileContent);
        }


        fis.close();


        agente.send(new Pacote(false,false,true,false,false,fileContent,getAvailableSpaceInBuffer(),seq,"lol","lol"));

	}





/*


    List<Dados> file2Dados(String filename) throws IOException, FicheiroNaoExisteException{
        
        File file = new File(filename);
        byte[] fileContent = new byte[(int) file.length()];

        if(!file.exists())
            throw new FicheiroNaoExisteException("Ficheiro nao existe");
        try{
            FileInputStream fis = new FileInputStream(file);
            fis.read(fileContent);
            fis.close();
        }
        catch(IOException e){
            throw new IOException(e);
        }
        
        try{
            List<Dados> dados = new ArrayList<>();
            for(int i = 0; i < fileContent.length; i = i + 1000){
                byte [] copia;
                if(fileContent.length - i > 1000){
                    copia = new byte[1000];
                    System.arraycopy(fileContent, i, copia, 0, 1000);
                }
                else{
                    copia = new byte[(fileContent.length - i)];
                    System.arraycopy(fileContent, i, copia, 0, fileContent.length - i);
                }
                Dados d = new Dados(copia, i);
                dados.add(d);
            }
            return dados;
        }
        catch(Exception e){
            System.out.println(e);
        }
        return (new ArrayList<Dados>());
    }






	/*
	void upload(String filename) throws ConexaoNaoEstabelecidaException{
        try{

            AgenteUDP client = new AgenteUDP();
            List<Dados> dados = file2Dados(filename);
            client.upload(dados);
        }
        catch(ConexaoNaoEstabelecidaException e){
            throw new ConexaoNaoEstabelecidaException(e.getMessage());
        }
        catch(Exception e){
            System.out.println(e);
        }
	}

	void download(String filename) throws ConexaoNaoEstabelecidaException{

        try{    
  		    AgenteUDP client = new AgenteUDP();
      		List<Pacote> pacotes = client.download(filename); //client.download(filename);
            escreveFile(pacotes);
        }
        catch(ConexaoNaoEstabelecidaException e){
            throw new ConexaoNaoEstabelecidaException(e.getMessage());
        }  		
        catch(Exception e){
            System.out.println(e);
        }
	}

    void iniciaServidor(){
        try{
            List<Pacote> pacotes = new ArrayList<>();
            AgenteUDP udp = new AgenteUDP();
            pacotes = udp.servidor();
            escreveFile(pacotes);
        }
        catch(IOException e){
            System.out.println(e);
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    List<Dados> file2Dados(String filename) throws IOException, FicheiroNaoExisteException{
        
        File file = new File(filename);
        byte[] fileContent = new byte[(int) file.length()];

        if(!file.exists())
            throw new FicheiroNaoExisteException("Fichiero nao existe");
        try{
            FileInputStream fis = new FileInputStream(file);
            fis.read(fileContent);
            fis.close();
        }
        catch(IOException e){
            throw new IOException(e);
        }
        
        try{
            List<Dados> dados = new ArrayList<>();
            for(int i = 0; i < fileContent.length; i = i + 1008){
                byte [] copia;
                if(fileContent.length - i > 1008){
                    copia = new byte[1008];
                    System.arraycopy(fileContent, i, copia, 0, 1008);
                }
                else{
                    copia = new byte[(fileContent.length - i)];
                    System.arraycopy(fileContent, i, copia, 0, fileContent.length - i);
                }
                Dados d = new Dados(copia, i);
                dados.add(d);
            }
            return dados;
        }
        catch(Exception e){
            System.out.println(e);
        }
        return (new ArrayList<Dados>());
    }

    public void escreveFile(List<Pacote> pacotes){
        pacotes.sort(new CompareOffsetPacote());
        List<Byte> data = new ArrayList<>();
        for(Pacote p: pacotes){
            for(byte b: p.getDados())
                data.add(b);
        }

        byte[] bin_file = new byte[data.size()];

        int j = 0;

        for(Byte b: data)
            bin_file[j++] = b.byteValue();

        try {
            FileOutputStream fos = new FileOutputStream("Teste/Recebi.txt");
            fos.write(bin_file);
            fos.close();
        }
        catch(IOException exc){}
    }*/
}