import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.io.FileNotFoundException;
import java.io.PrintWriter;


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
	}

	public int getRemaining(){
		lock.lock();
		int ret = bRemaining;
		lock.unlock();
		return ret;
	}
	public int read(byte[] arr){
		lock.lock();

		int bytesLidos = Math.min(bRemaining,arr.length);		//Qt de bytes que deve ler
		int tamanhoRestante = buffer.length - bCursor;			//bytes até ao final do array do buffer
		if(bytesLidos < tamanhoRestante){
			System.arraycopy(buffer,bCursor,arr,0,bytesLidos);
			bCursor += bytesLidos;
			bRemaining -= bytesLidos;
		}
		else{
			System.arraycopy(buffer,bCursor,arr,0,tamanhoRestante);
			System.arraycopy(buffer,0,arr,tamanhoRestante,bytesLidos - tamanhoRestante);
			bCursor = bytes - tamanhoRestante;
			bRemaining -= bytesLidos;
		}

		lock.unlock();

		return bytesLidos;
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



	public void get(filename){
		Estado estado = new Estado();
		this.agente = new AgenteUDP(IPAddress,7777,estado);
		
		//Criação e envio do pedido
		String request = "GET " + filename;
		Pacote pedido = new Pacote(false,false,false,false,true,request.getBytes(),getRemaining(),0,"lol","lol");
		boolean confirmado = false;
		while(!confirmado){
			agente.send(pedido);
			confirmado = estado.esperaConfirmacao(100);			//Substituir por agente.receive() e fazer aqui a confirmaçao
		}


		int seq = 0;
		Pacote ack = new Pacote(true,false,false,false,false,new byte[0],getRemaining(),seq,"lol","lol");
		//Começa a receber o ficheiro
		boolean terminado = false;
		while(!terminado){
			Pacote recebido = estado.getPacote();
			if(/*Verificação de integridade*/ && recebido.isPSH() && recebido.getOffset() == seq){	//GBN por enquanto
				write(recebido.getDados);										//Extração e entrega à aplicação
				seq += 1000;
				ack = new Pacote(true,false,false,false,false,new byte[0],getRemaining(),seq,"lol","lol");
			}
			if(/*integridade*/ && recebido.isFIN()){
				break;
			}
			agente.send(ack);
		}


		//Término da conexao
	}



	public void iniciaServidor(){
		Estado estado = new Estado()
		//Aceita conexoes, o seguinte deve ser numa thread a parte


		Pacote pedido = estado.esperaPedido();
        String [] filename = str.split(" ");
		//Verifica se é GET
		int window = pedido.getWindow();		//bytes disponiveis no buffer do recetor

        File file = new File(filename[1]);

        FileInputStream fis = new FileInputStream(file);
        int bytesLidos,seq = 0;
        byte[] fileContent = new byte[1000];
        //criar thread para gerir acks
        List<Pacote> listPac = new ArrayList<Pacote>();				//Quando recebe uma ACK deve ser retirado o pacote correspondente, talvez por no estado
        bytesLidos = fis.read(fileContent);
        while(bytesLidos != -1){
        	estado.esperaWindow(seq+bytesLidos)					//Espera caso nao tenha espaço na janela; se voltar falso -> timeout
        	Pacote pacote = new Pacote(false,false,false,true,false,fileContent,getRemaining(),seq,"lol","lol");
        	seq += bytesLidos;
        	listPac.add(pacote);
        	agente.send(pacote);
        	bytesLidos = fis.read(fileContent);
        	}else{
        		agente.send(listPac.get(0));					//Envia pacote mais antigo nao ACK; FAZER ISTO NUMA THREAD QUE GERE OS TIMEOUTS; 
        	}
        }


        fis.close();
	}








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