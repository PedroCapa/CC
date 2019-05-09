/**
 * Classe que recebe dados de pacotes para preencher um buffer e envia ACKs
 *
 */
class RecebeDados extends Thread{
	private Estado estado;
	private AgenteUDP agente;


	RecebeDados(Estado e, AgenteUDP a){
		this.estado = e;
		this.agente = a;
	}

	public void run(){

		estado.setSeq(0);

		Pacote ack = new Pacote(true,false,false,false,false,new byte[0],estado.bufferAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino());

		//Começa a receber o ficheiro


		boolean terminado = false;
		while(!terminado){

			Pacote recebido = estado.receive();
			Pacote escrito = null;
			if(recebido.getPsh()){ //Verifica se esta dentro da janela

				while(recebido != null && estado.getSeq() == recebido.getOffset()){
					estado.writeBuffer(recebido.getDados());										//Extração e entrega à aplicação
					escrito = recebido;
					estado.addSeq(recebido.tamanhoDados());
					recebido = estado.bufferPacRecebidoPollFirst();
				}if(recebido != null && estado.getSeq() < recebido.getOffset()){				//Caso haja receções fora de ordem
					estado.addBufferPacRecebido(recebido);
				}
				ack = new Pacote(true,false,false,false,false,new byte[0],estado.bufferAvailableSpace(),estado.getSeq(),0,estado.getPortaDestino(),null,estado.getDestino());
			
				if(escrito!=null && escrito.pshFin()){
					terminado = true;
				}
				agente.send(ack);
			}
		}


		//Término da conexao

		estado.closeBuffer();
	}
}
