import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/*O agenteUDP so envia e recebe pacotes, o transporteUDP faz o resto
   TRansfereCC processa o pedido de conexao
   Enviar e receber datagramas e o agenteUDP
*/

// Para ja isto pode ser usado para o cliente indicar que ficheiro quer receber e se a conexão foi aceite
class Estado{
   private List<Pacote> pacotes;
   private String origem;
   private String destino;
   //int portaDest;
   //int portaOrigem;
   private List<Integer> ACK;
   private int fase;
   private int numero; //numero de pacotes de dados que serao enviados
   private Lock lock;
   private Condition espera;

   public Estado(){
      this.pacotes = new ArrayList<>();
      this.origem = null;
      this.destino = null;
      //this.portaDest = 0;
      //this.portaOrigem = 0;
      this.ACK = new ArrayList<>();
      this.fase = 0;
      this.numero = 0;
      this.lock = new ReentrantLock();
      this.espera = lock.newCondition();
   }

   public Estado(List<Pacote> pacotes, String origem, String destino, /*int portaDest, int portaOrigem,*/ List<Integer> ACK, int fase, int numero, Lock lock, Condition espera){
      this.pacotes = new ArrayList<>(pacotes);
      this.origem = origem;
      this.destino = destino;
      //this.portaDest = portaDest;
      //this.portaOrigem = portaOrigem;
      this.ACK = new ArrayList<>(ACK);
      this.fase = fase;
      this.numero = numero;
      this.lock = lock;
      this.espera = espera;
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
/*
   public int getPortaOrigem(){
      return this.portaOrigem;
   }

   public int getPortaDest(){
      return this.portaDest;
   }
*/
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

   public List<Integer> getACK(){
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
   public boolean verificaEnvio(){
      lock.lock();
      try{
         if(this.fase == 4)
            return true;
         else return false;
      }
      finally{
         lock.unlock();
      }
   }

   public void addACK(Integer ack){
      lock.lock();
      try{
         this.ACK.add(ack);
      }
      finally{
         lock.unlock();
      }
   }

   public Integer getLastACK(){
        lock.lock();
        try{
            return this.ACK.get(this.ACK.size() - 1);
        }
        finally{
            lock.unlock();
        }
   }

   public void espera(){
      lock.lock();
      try{
         espera.await();
      }
      catch(InterruptedException e){

      }
      finally{
         lock.unlock();
      }
   }

   public void acorda(){
      lock.lock();
      try{
         espera.signalAll();
      }
      finally{
         lock.unlock();
      }
   }
}