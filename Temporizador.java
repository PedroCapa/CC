import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Temporizador extends Thread{
	private Estado estado;
	private AgenteUDP agente;

	RecebeACK(Estado e, AgenteUDP a){
		this.estado = e;
		this.agente = a;
	}

	public void run(){
		while(estado.transferir()){				//Enquanto ainda se estiver na fase de transferencia
			Pacote p = estado.timer();			//Se for necessário retransmitir, recebe o pacote, cc recebe null
			if(p != null){
				agente.send(p);					//Falta garantir que nao se envia mais nada
			}
		}
	}

}
