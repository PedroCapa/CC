import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class RecebeACK extends Thread{
	private Estado estado;


	RecebeACK(Estado e){
		this.estado = e;
	}

	public void run(){
		while(estado.transferir()){				//Enquanto ainda se estiver na fase de transferencia
			Pacote pacote = estado.receive();
			if(pacote.getAck()){
				estado.setLastAck(pacote);
			}if(pacote.getFin() || pacote.getReq()){
				estado.terminaTransferencia();			//Significa que já não está interessado na transferẽncia atual, útil caso o ultimo ack tenha sido perdido
			}
		}
	}


}
