import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

/*O agenteUDP so envia e recebe pacotes, o transporteUDP faz o resto
   TRansfereCC processa o pedido de conexao
   Enviar e receber datagramas e o agenteUDP
*/

// Para ja isto pode ser usado para o cliente indicar que ficheiro quer receber e se a conexão foi aceite
class Estado{
   List<Pacote> pacotes;
   String origem;
   String destino;
   //int portaDest;
   //int portaOrigem;
   List<Integer> ACK;
   int fase;
   int numero; //numero de pacotes de dados que serao enviados

   public Estado(){
      this.pacotes = new ArrayList<>();
      this.origem = null;
      this.destino = null;
      //this.portaDest = 0;
      //this.portaOrigem = 0;
      this.ACK = new ArrayList<>();
      this.fase = 0;
      this.numero = 0;
   }

   public Estado(List<Pacote> pacotes, String origem, String destino, /*int portaDest, int portaOrigem,*/ List<Integer> ACK, int fase, int numero){
      this.pacotes = new ArrayList<>(pacotes);
      this.origem = origem;
      this.destino = destino;
      //this.portaDest = portaDest;
      //this.portaOrigem = portaOrigem;
      this.ACK = new ArrayList<>(ACK);
      this.fase = fase;
      this.numero = numero;
   }

   public List<Pacote> getPacotes(){
      return this.pacotes;
   }

   public String getDestino(){
      return this.destino;
   }

   public String getOrigem(){
      return this.origem;
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
      return this.fase;
   }

   public int getNumero(){
      return this.numero;
   }

   public List<Integer> getACK(){
      return this.ACK;
   }

   public void setFase(int fase){
      this.fase = fase;
   }

   //verifica se e necessario reenviar algum pacote
   public boolean verificaEnvio(){
      if(this.fase == 4)
         return true;
      else return false;
   }

   public void addACK(Integer ack){
      this.ACK.add(ack);
   }

   public Integer getLastACK(){
      return this.ACK.get(this.ACK.size() - 1);
   }
}