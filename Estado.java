import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

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
	private List<Pacote> listPac;			//Lista de pacotes ainda não ACK
	private boolean transferir;				//Flag que indica se a transferencia ainda está a decorrer
	private Condition enviado;
	private List<Pacote> pacotes;			//Lista de pacotes a receber
	private Condition recebePacote;
	private int seq;						//qt de bytes já lidos e escritos

	/**Construutor usado pelo servidor*/
	public Estado(){
		this.destino = null;
		this.portaDest = 0;

		this.lock = new ReentrantLock();

		this.flowWindow = 0;
		this.lastAck = 0;
		this.finalAck = -1;
		this.timeout = 100;
		this.listPac = new ArrayList<>();
		this.transferir = true;				
		this.enviado = lock.newCondition();
		this.ackReceivedC = lock.newCondition();
		this.recebePacote = lock.newCondition();
		this.pacotes = new ArrayList<>();
   }

	/**Construtor utilizado pelos clientes*/   
	public Estado(InetAddress destino){
		this.destino = destino;
		this.portaDest = 7777;

		this.lock = new ReentrantLock();

		this.flowWindow = 0;
		this.lastAck = 0;
		this.finalAck = -1;
		this.timeout = 100;
		this.listPac = new ArrayList<>();
		this.transferir = true;				
		this.enviado = lock.newCondition();
		this.ackReceivedC = lock.newCondition();
		this.recebePacote = lock.newCondition();
		this.pacotes = new ArrayList<>();
   }

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

   	public void redirecionaPacote(Pacote p){
   		lock.lock();
   		pacotes.add(p);
   		recebePacote.signalAll();
   		lock.unlock();
   	}

 	public boolean transferir(){
		lock.lock();
		boolean ret = transferir;
		lock.unlock();
		return ret;
	}

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

   	public void enviou(Pacote p){
   		lock.lock();
   		enviado.signalAll();
   		listPac.add(p);
   		lock.unlock();
   	}

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

	public void setFinalAck(int s){		
		lock.lock();
		this.finalAck = s;
		lock.unlock();
	}


}
