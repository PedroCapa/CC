

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

class Buffer{

    private byte[] buffer;
    private int bCursor;
    private int bRemaining;
    private Lock lock;
    private Condition bufferLivre;
    private Condition bufferEscrito;
    private boolean fim;		//Será verdadeiro quando não se intenciona em escrever mais no buffer

	Buffer(int bufferSize){
		this.buffer = new byte[bufferSize];
		this.bCursor = 0;
		this.bRemaining = 0;
		this.lock = new ReentrantLock();
		this.bufferLivre = lock.newCondition();
		this.bufferEscrito = lock.newCondition();
		this.fim = false;

	}

	public void close(){
		lock.lock();
		this.fim = true;
		bufferEscrito.signalAll();
		lock.unlock();
	}

	public int getAvailableSpace(){
		lock.lock();
		int ret = buffer.length - bRemaining;
		lock.unlock();
		return ret;
	}

	public boolean isClosed(){
		lock.lock();
		boolean ret = fim;
		lock.unlock();
		return ret;
	}

	public void waitForAvailableSpace(int bytes){
		lock.lock();
		try{
			while(bytes>buffer.length - bRemaining){
				bufferLivre.await();
			}
		}
		catch(InterruptedException e){}
		finally{
			lock.unlock();
		}
	}

	public byte[] read(int size) throws DadosAindaNaoRecebidos{
		lock.lock();
		try{
			while(bRemaining==0 && !fim){
				if(!bufferEscrito.await(100,TimeUnit.MILLISECONDS)){
					throw new DadosAindaNaoRecebidos();	//Publica a janela aberta
				}
			}
		}catch(InterruptedException e){}
		
		if(bRemaining==0){
			return null;
		}

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
		bufferLivre.signalAll();
		lock.unlock();
		return arr;
	}

	public void write(byte[] arr){
		lock.lock();
		waitForAvailableSpace(arr.length);
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

		bufferEscrito.signalAll();

		lock.unlock();
	}
}



class DadosAindaNaoRecebidos extends Exception{
	/** Mensagem vazia no caso se tentar fazer registar com um  nome que ja exista*/
	public DadosAindaNaoRecebidos(){
		super();
	}
	/** Mensagem no caso se tentar fazer registar com um  nome que ja exista*/
	public DadosAindaNaoRecebidos(String s){
		super(s);
	}
}