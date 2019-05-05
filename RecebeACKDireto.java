import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class RecebeACKDireto extends Thread{
	private Estado estado;
	private AgenteUDP agente;


	RecebeACKDireto(Estado e, AgenteUDP a){
		this.estado = e;
		this.agente = a;
	}

	public void run(){
		while(estado.transferir()){				//Enquanto ainda se estiver na fase de transferencia
			Pacote pacote = agente.receive();
			if(pacote.getAck()){
				estado.setLastAck(pacote);
			}
		}
	}


}
