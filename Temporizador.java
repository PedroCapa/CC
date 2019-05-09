/**
 * Classe que trata de verificar se um pacote deve ser reenviado por timeout e o reenvia nesse caso 
 *
 */

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Temporizador extends Thread{
	private Estado estado;
	private AgenteUDP agente;

	public Temporizador(Estado e, AgenteUDP a){
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
