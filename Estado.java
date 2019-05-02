import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/*O agenteUDP so envia e recebe pacotes, o transporteUDP faz o resto
   TRansfereCC processa o pedido de conexao
   Enviar e receber datagramas e o agenteUDP
*/

// Para ja isto pode ser usado para o cliente indicar que ficheiro quer receber e se a conexão foi aceite
class Estado{


	private String origem;
	private String destino;
	private int portaOrigem;
	private int portaDest;
	private Lock lock;
	private Condition recebe;
	private boolean receber;
	private Condition ackReceivedC;			
	private int flowWindow;					//Tamanho da janela
	private int lastAck;					//Maior Ack recebido
	private int timeout;					//Tempo para receber um ACK até que reenvie (em ms)
	private List<Pacote> listPac;			//Lista de pacotes ainda não ACK
	private boolean transferir;				//Flag que indica se a transferencia ainda está a decorrer


   public Estado(){
      this.origem = null;
      this.destino = null;

      this.lock = new ReentrantLock();
      this.recebe = lock.newCondition();
      this.receber = true;
   }

   public Estado(String origem, String destino, int portaOrigem, int portaDest){
      this.origem = origem;
      this.destino = destino;
      this.portaOrigem = portaOrigem;
      this.portaDest = portaDest;

      this.lock = new ReentrantLock();
      this.recebe = lock.newCondition();
      this.receber = true;

      this.flowWindow = 0;
      this.lastAck = 0;
      this.timeout = 100;
      this.listPac = new ArrayList<>();
   }


   	public Pacote timer(){
   		lock.lock();
   		try{
   			while(listPac.isEmpty()){
				enviado.await();	 								//esperar que algum timeout inicie
   			}
			while(!listPac.isEmpty()){
				if(!ackReceivedC.await(timeout,MILLISECONDS)){		//começar a espera
					return listPac[0];								//Se levou timeout reenvia menor
				}
			}										
		}
		catch(InterruptedException e){}
		finally{
		 lock.unlock();
		}
		return null;
   	}

	public void esperaWindow(int bytes){				//Pode sair pq ganhou espaço
		lock.lock();
		try{
			while(bytes>flowWindow+lastAck){
		 		ackReceivedC.await();
			}
		}
		catch(InterruptedException e){}
		finally{
		 lock.unlock();
		}
	}

	public void setLastAck(int ack){			//DEVE DEPOIS ELIMINAR PACOTES DO BUFFER DOS ENVIADOS
		lock.lock();
		if(ack>this.lastAck){
			this.lastAck = ack;
			listPac.removeIf(e->e.getOffset()<ack);
			ackReceivedC.signalAll();
		}
		lock.unlock();
	}

	public boolean esperaConfirmacao(long tempo){
		lock.lock();
		boolean recebeuSinal = true;
		try{
			while(receber){
		 		recebeuSinal = recebe.await(tempo,MILLISECONDS);
			}
			receber=true;
		}
		catch(InterruptedException e){}
		finally{
		 lock.unlock();
		}
		return recebeuSinal;
	}

	public void acordaRecebe(){
	  lock.lock();
	  try{
	     recebe.signalAll();
	     receber=false;
	  }
	  finally{
	     lock.unlock();
	  }
	}
















   /*private List<Pacote> pacotes;
   public Map<Integer, Pacote> adiantados;
   private String origem;
   private String destino;
   //int portaDest;
   //int portaOrigem;
   private Integer ACK;
   private int fase;
   private int numero; //numero de pacotes de dados que serao enviados
   private Lock lock;
   private Condition recebe;
   private Condition controlo;
   private boolean receber;
   private boolean controlar;

   public Estado(){
      this.pacotes = new ArrayList<>();
      this.adiantados = new TreeMap<>((p1, p2) -> Integer.compare(p1, p2));
      this.origem = null;
      this.destino = null;
      this.ACK = 0;
      this.fase = 0;
      this.numero = 0;
      this.lock = new ReentrantLock();
      this.recebe = lock.newCondition();
      this.controlo = lock.newCondition();
      this.receber = true;
      this.controlar = true;
   }

   public Estado(List<Pacote> pacotes, TreeMap<Integer, Pacote> adiantados,String origem, String destino, Integer ACK, int fase, int numero, Lock lock, Condition recebe, Condition controlo){
      this.pacotes = new ArrayList<>(pacotes);
      this.adiantados = new TreeMap<>((p1, p2) -> Integer.compare(p1, p2));
      this.adiantados.putAll(adiantados);
      this.origem = origem;
      this.destino = destino;
      this.ACK = ACK;
      this.fase = fase;
      this.numero = numero;
      this.lock = lock;
      this.recebe = recebe;
      this.controlo = controlo;
      this.receber = true;
      this.controlar = true;
   }

   public List<Pacote> getPacotes(){
      lock.lock();
      try{
         return this.pacotes;
      }
      finally{
         lock.unlock();
      }
   }

   public String getDestino(){
      lock.lock();
      try{
         return this.destino;
      }
      finally{
         lock.unlock();
      }
   }

    public String getOrigem(){
        lock.lock();
        try{
            return this.origem;
        }
        finally{
            lock.unlock();
        }
    }

    public Map<Integer, Pacote> getAdiantados(){
        return new TreeMap<>(this.adiantados);
    }

    public int getFase(){
      lock.lock();
      try{
         return this.fase;
      }
      finally{
         lock.unlock();
      }
   }

   public int getNumero(){
      lock.lock();
      try{
         return this.numero;
      }
      finally{
         lock.unlock();
      }
   }

   public Integer getACK(){
      lock.lock();
      try{
         return this.ACK;
      }
      finally{
         lock.unlock();
      }
   }

    public void setFase(int fase){
        lock.lock();
        try{
            this.fase = fase;
        }
        finally{
            lock.unlock();
        }
   }

   //verifica se e necessario reenviar algum pacote
   public boolean reenvia(){
      lock.lock();
      try{
         if(this.fase == 5)
            return true;
         else return false;
      }
      finally{
         lock.unlock();
      }
   }

   public void setACK(Integer ack){
      lock.lock();
      try{
         this.ACK = ack;
      }
      finally{
         lock.unlock();
      }
   }

   public void esperaRecebe(){
      lock.lock();
      try{
      	while(receber){
         recebe.await();
      	}
      	receber=true;
      }
      catch(InterruptedException e){}
      finally{
         lock.unlock();
      }
   }

    public void setNumero(int numero){
        lock.lock();
        try{
             this.numero = numero;
        }
        finally{
            lock.unlock();
        }
    }

   public void acordaRecebe(){
      lock.lock();
      try{
         recebe.signalAll();
         receber=false;
      }
      finally{
         lock.unlock();
      }
   }

   public void esperaControlo(){
      lock.lock();
      try{
      	while(controlar){
         controlo.await();
      	}
      	controlar=true;
      }
      catch(InterruptedException e){}
      finally{
         lock.unlock();
      }
   }

    public void acordaControlo(){
        lock.lock();
        try{
            controlo.signalAll();
            controlar=false;
        }
        finally{
            lock.unlock();
        }
    }

    public Pacote getPacoteWithOffset(Integer offset){
        Pacote p = new Pacote();
        for(int i = 0; i < pacotes.size(); i++){
            p = pacotes.get(i);
            if(p.getOffset() == offset)
                i = pacotes.size();
        }
        return p;
    }

    public void addPacote(Pacote p){
        lock.lock();
        try{
            this.pacotes.add(p);
        }
        finally{
            lock.unlock();
        }
    }

    public int enviaAck(){
        lock.lock();
        int x = 0;
        try{
            do{
                x = this.ACK;
                Thread.sleep(100);
            }
            while(x != this.ACK);
        }
        catch(InterruptedException e){
            System.out.println(e.getMessage());
        }
        finally{
            lock.unlock();
        }
        return x;
    }

    public Pacote getLastPacote(){
        return this.pacotes.get(pacotesSize() - 1);
    }

    public int tamanhoDados(){
        if(this.pacotes.size() == 0)
            return -1;
        else{
            return this.pacotes.get(pacotesSize() - 1).tamanhoDados();
        }
    }

    public int pacotesSize(){
        return this.pacotes.size();
    }

    public int lastOffset(){
        return this.pacotes.get(pacotesSize() - 1).getOffset();
    }

    public boolean seguinte(int off){
        int last = lastOffset();
        int tam = tamanhoDados();
        if(tam != -1)
            return (last + tam == off);
        else
            return false;
    }

    public void atualizaAdiantados(){
        
        while(adiantados.size() > 0 && seguinte(this.adiantados.get(0).getOffset())){
            Pacote p = new Pacote(this.adiantados.get(0));
            this.pacotes.add(p);
            this.adiantados.remove(0);
        }
    }

    public boolean eRepetido(Integer offset){
        boolean f = false;
        for(Pacote p: this.pacotes)
            if(p.getOffset() == offset)
                f = true;
        if(f)
            return true;
        else{
            for(Integer i: this.adiantados.keySet())
                if(i == offset)
                    f = true;
        }
        return f;
    }

    public void atualizaACK(){
        this.ACK = getLastPacote().getOffset() + tamanhoDados();
    }

    public void addAdiantados(Pacote p){
        this.adiantados.put(p.getOffset(), p);
    }*/
}