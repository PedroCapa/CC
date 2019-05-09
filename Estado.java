import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import java.util.TreeSet;

/*O agenteUDP so envia e recebe pacotes, o transporteUDP faz o resto
   TRansfereCC processa o pedido de conexao
   Enviar e receber datagramas e o agenteUDP
*/

// Para ja isto pode ser usado para o cliente indicar que ficheiro quer receber e se a conexão foi aceite
class Estado{


	private InetAddress destino;
	private int portaDest;
	private Lock lock;
	private Condition ackReceivedC;			
	private int flowWindow;					//Tamanho da janela
	private int lastAck;					//Maior Ack recebido
	private int finalAck;					//Ultimo ack a ser recebido para concluir a transferencia
	private long timeout;					//Tempo para receber um ACK até que reenvie (em ms)
	private List<Pacote> listPac;			//Lista de pacotes enviados por esta máquina ainda não confirmados
	private boolean transferir;				//Flag que indica se a transferencia ainda está a decorrer
	private Condition enviado;
	private List<Pacote> pacotes;			//Lista de pacotes recebidos ainda não tratados, enviados pela thread RecebePacotes a ler por outras threads
	private Condition recebePacote;
	private int seq;						//qt de bytes já lidos e escritos
	private Buffer buffer;					//Buffer de dados a escrever
	private boolean fin;					//Indica se este host já enviou o seu Fin e portanto não pode enviar dados
	private TreeSet<Pacote> pacBufferRec;	//Pacotes recebidos com um número de sequência maior que o esperado

	/**Construutor usado pelo servidor*/
	public Estado(){
		this.destino = null;
		this.portaDest = 0;

		this.lock = new ReentrantLock();

		this.flowWindow = 0;
		this.lastAck = 0;
		this.finalAck = -1;
		this.timeout = 300;
		this.listPac = new ArrayList<>();
		this.transferir = true;				
		this.enviado = lock.newCondition();
		this.ackReceivedC = lock.newCondition();
		this.recebePacote = lock.newCondition();
		this.pacotes = new ArrayList<>();
		this.buffer = new Buffer(32000); 
		this.fin = false;
		this.seq = 0;
		this.pacBufferRec = new TreeSet<>((Pacote p1, Pacote p2) -> p1.getOffset()-p2.getOffset());
   }

	/**Construtor utilizado pelos clientes*/   
	public Estado(InetAddress destino, int portaDest, int bs, int flowWindow){
		this.destino = destino;
		this.portaDest = portaDest;

		this.lock = new ReentrantLock();

		this.flowWindow = flowWindow;
		this.lastAck = 0;
		this.finalAck = -1;
		this.timeout = 100;
		this.listPac = new ArrayList<>();
		this.transferir = true;				
		this.enviado = lock.newCondition();
		this.ackReceivedC = lock.newCondition();
		this.recebePacote = lock.newCondition();
		this.pacotes = new ArrayList<>();
		this.buffer = new Buffer(bs); 
		this.fin = false;
		this.seq = 0;
		this.pacBufferRec = new TreeSet<>((Pacote p1, Pacote p2) -> p1.getOffset()-p2.getOffset());
   }

	/**
	*Método que recebe um pacote referente a esta conexão, desviado pelo RecebePacotes
	*
	*@return Pacote recebido
	*/
   	public Pacote receive(){
   		Pacote p = null;
   		lock.lock();
   		try{
	   		while(pacotes.isEmpty()){
	   			recebePacote.await();
	   		}
	   		p = pacotes.remove(0);
	   	}catch(InterruptedException e){}
	   	finally{
	   		lock.unlock();
	   	}
   		return p;
   	}

	/**
	*Método que recebe um pacote referente a esta conexão durante um certo tempo, desviado pelo RecebePacotes
	*
	*@param ms Milissegundos que espera até que volte sem pacote
	*@return Pacote recebido ou null caso o tempo tenha passado
	*/
   	public Pacote receive(int ms){
   		Pacote p = null;
   		lock.lock();
   		try{
	   		while(pacotes.isEmpty()){
	   			if(!recebePacote.await(ms,TimeUnit.MILLISECONDS)){
	   				return p;
	   			}
	   		}
	   		p = pacotes.remove(0);
	   	}catch(InterruptedException e){}
	   	finally{
	   		lock.unlock();
	   	}
   		return p;
   	}
   
	/**
	*Método que adiciona um pacote aos pacotes a receber
	*
	*@param p Pacote recebido
	*/
   	public void redirecionaPacote(Pacote p){
   		lock.lock();
   		pacotes.add(p);
   		recebePacote.signalAll();
   		lock.unlock();
   	}

	/**
	*Método que indica se existe alguma transferencia a decorrer
	*
	*@return true caso exista, false caso contrário
	*/
 	public boolean transferir(){
		lock.lock();
		boolean ret = transferir;
		lock.unlock();
		return ret;
	}

	/**
	*Método que indica que define que não existem transferências a decorrer
	*
	*/
	public void terminaTransferencia(){
		lock.lock();
		this.transferir = false;
		enviado.signalAll();
		lock.unlock();
	}

	public int getSeq(){
		lock.lock();
		int ret = seq;
		lock.unlock();
		return ret;
	}

	public void setSeq(int s){
		lock.lock();
		this.seq = s;
		lock.unlock();
	}

	public void addSeq(int s){
		lock.lock();
		this.seq += s;
		lock.unlock();
	}

	public int getPortaDestino(){
		return portaDest;
	}

	public void setPortaDestino(int p){
		this.portaDest = p;
	}

	public InetAddress getDestino(){
		return destino;
	}

	public void setDestino(InetAddress d){
		this.destino = d;
	}

	public void setFlowWindow(int w){
		this.flowWindow = w;
	}

	/**
	*Método que irá bloquear até que não hajam pacotes à espera de confirmação, ou até que algum tenha sofrido timeout
	*
	*@return Pacote que recebeu timeout ou null caso nenhuma mensagem tenha sofrido timeout
	*/
   	public Pacote timer(){
   		lock.lock();
   		try{
   			while(listPac.isEmpty() && transferir){
				enviado.await();								//esperar que algum timeout inicie
   			}
			while(!listPac.isEmpty()){
				if(!ackReceivedC.await(timeout,TimeUnit.MILLISECONDS) && !listPac.isEmpty()){		//começar a espera
					return listPac.get(0);								//Se levou timeout reenvia menor
				}
			}										
		}
		catch(InterruptedException e){}
		finally{
		 lock.unlock();
		}
		return null;
   	}

	/**
	*Método que indica que um pacote foi enviado para que este seja guardado caso haja necessidade de o reenviar
	*
	*@param p Pacote enviado
	*/
   	public void enviou(Pacote p){
   		lock.lock();
   		enviado.signalAll();
   		listPac.add(p);
   		lock.unlock();
   	}

	/**
	*Método que espera até que haja algum espaço no buffer do outro lado da conexão
	*
	*@param bytes Nº de bytes que se pretende enviar
	*/
	public void esperaWindow(int bytes){				//Pode sair pq ganhou espaço
		lock.lock();
		try{
			while(bytes+seq>flowWindow+lastAck){;
		 		ackReceivedC.await();
			}
		}
		catch(InterruptedException e){}
		finally{
		 lock.unlock();
		}
	}

	/**
	*Método que define o último pacote a ser confirmado e atualiza o tamanho da janela do outro lado da conexão
	*
	*@param p Pacote ACK recebido
	*/
	public void setLastAck(Pacote p){			//DEVE DEPOIS ELIMINAR PACOTES DO BUFFER DOS ENVIADOS
		lock.lock();
		int ack = p.getOffset();
		if(ack>this.lastAck || (ack==this.lastAck && p.getWindow()>this.flowWindow)){			//Só interessa atualizar se o ack for maior ou se for igual e uma janela maior
			this.lastAck = ack;
			this.flowWindow=p.getWindow();
			listPac.removeIf(e->e.getOffset()<ack);
			ackReceivedC.signalAll();
			if(ack == finalAck){
				terminaTransferencia();
			}
		}
		lock.unlock();
	}

	public int getLastAck(){			//DEVE DEPOIS ELIMINAR PACOTES DO BUFFER DOS ENVIADOS
		lock.lock();
		int ret = this.lastAck;
		lock.unlock();
		return ret;
	}

	/**
	*Método que define o último ack que deverá ser enviado
	*
	*@param s Valor do ACK final
	*/
	public void setFinalAck(int s){		
		lock.lock();
		this.finalAck = s;
		lock.unlock();
	}

	/**
	*Método que lê o buffer, bloqueando caso não seja possível ler o nº de bytes indicados
	*
	*@param size Nº de bytes a ler
	*@return Array de bytes lidos
	*/
	public byte[] readBuffer(int size) throws DadosAindaNaoRecebidos{
		return this.buffer.read(size);
	}

	/**
	*Método que escreve no buffer um array de bytes, bloqueando até que este possa ser escrito
	*
	*@param arr Bytes a escrever
	*/
	public void writeBuffer(byte[] arr){
		this.buffer.write(arr);
	}

	/**
	*Método que devolve o espaço disponível no buffer local
	*
	*@return Nº de bytes disponíveis
	*/
	public int bufferAvailableSpace(){
		return this.buffer.getAvailableSpace();
	}

	/**
	*Método que fecha o buffer local
	*
	*/
	public void closeBuffer(){
		this.buffer.close();
	}

	/**
	*Método que identifica a conexão como terminada
	*
	*/
	public void terminaConexao(){
		lock.lock();
		terminaTransferencia();
		this.fin = true;
		lock.unlock();
	}

	/**
	*Método que reinicializa variáveis caso se pretenda transferir mais do que um ficheiro por conexão
	*
	*/
	public void reset(){
		lock.lock();

		this.seq = 0;
		this.lastAck = 0;
		this.finalAck = -1;
		this.listPac = new ArrayList<>();
		this.transferir = true;				
		this.pacotes = new ArrayList<>();
		this.buffer = new Buffer(this.buffer.getBufferSize()); 
		lock.unlock();
	}

	public boolean isFin(){
		lock.lock();
		boolean ret = this.fin;
		lock.unlock();
		return ret;
	}

	public void addBufferPacRecebido(Pacote recebido){
		this.pacBufferRec.add(recebido);
	}

	public Pacote bufferPacRecebidoPollFirst(){
		return this.pacBufferRec.pollFirst();
	}
}
