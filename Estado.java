import java.io.*;
import java.net.*;


/*O agenteUDP so envia e recebe pacotes, o transporteUDP faz o resto
   TRansfereCC processa o pedido de conexao
   Enviar e receber datagramas e o agenteUDP
*/

// Para ja isto pode ser usado para o cliente indicar que ficheiro quer receber e se a conexão foi aceite
class Estado{
   List<Pacote> pacotes;
   String origem;
   String destino;
   int portaDest;
   int portaOrigem;
   List<Integer> ACK;
   int fase;
}