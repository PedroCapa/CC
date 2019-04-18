import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/*O agenteUDP so envia e recebe pacotes, o transporteUDP faz o resto
   TRansfereCC processa o pedido de conexao
   Enviar e receber datagramas e o agenteUDP
*/

// Para ja isto pode ser usado para o cliente indicar que ficheiro quer receber e se a conexão foi aceite
class UDPClient{

    public void upload(byte[] bin_file) throws Exception{
        Estado estado = new Estado();
        Lock lock = new ReentrantLock();
        Condition espera = lock.newCondition();
        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        DatagramSocket clientSocket = new DatagramSocket();
        InetAddress IPAddress = InetAddress.getByName("localhost");
        String myIp = InetAddress.getLocalHost().getHostAddress();

        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];


        estado = new Estado(new ArrayList<>(), myIp, IPAddress.getHostAddress(), new ArrayList<>(), 0, (int) (bin_file.length / 1000), lock, espera);
        //Começar a thread receberACK
        RecebeACK R = new RecebeACK(estado, clientSocket);
        R.start();
     
        //Incio da conexão
        //Um pacote SYN nao recebe dados
        int tentativas = 0;
        while(estado.getFase() == 0 && tentativas < 10){
            Pacote inicio = new Pacote(false, true, false, false, new byte[0], -1, myIp, IPAddress.getHostAddress());
            System.out.println("FROM: UDPClient: Enviei SYN " + inicio.toString());
            sendData = inicio.pacote2bytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
            clientSocket.send(sendPacket);
            Thread.sleep(100);
            tentativas++;
        }

        if(tentativas == 10){
            R.interrupt();
            System.out.println("Nao foi aceite o pedido");
            clientSocket.close();
            return;
        }

        //Recebe o ACK
        //Passar esta parte para o RecebeACK e substituir por um await para esperar pela resposta

        //Envia ACK a dizer que vai começar a transmitir os dados
        // Envio de dados Nesta parte e para criar pacotes de acordo com a lista de bytes e enviar para o Servidor

        for(int i = 0; i < bin_file.length; i = i + 1000){
             byte[] dados;
            if(bin_file.length - i <= 1000){
                dados = new byte[1000];
                System.arraycopy(bin_file[i], i, dados, 0, 1000);
            }
            else{
                dados = new byte[(bin_file.length - i)];
                System.arraycopy(bin_file[i], i, dados, 0, bin_file.length - i);
            }
            Pacote enviar = new Pacote(false, false, false, true, dados, (Integer)(i / 1000), myIp, IPAddress.getHostAddress());
            sendData = enviar.pacote2bytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
            clientSocket.send(sendPacket);
            // Verificar se o ACK que recebeu está correto e em caso afirmativo envia o ultimo ACK que tiver
            if(!estado.verificaEnvio()){
                byte[] d;
                Pacote reenvio = new Pacote();
                d = new byte[1000];
                Integer indice = estado.getLastACK();
                System.arraycopy(bin_file[indice], indice, d, 0, 1000);
                sendData = reenvio.pacote2bytes();
                sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
                clientSocket.send(sendPacket);
            }
        }


        //Vai adormecer até receber o ultimo ACK em que vai ser um ciclo pq pode ter de reenviar pacotes
        // Fim da conexão
        while(estado.getFase() != 3){
            Pacote fim = new Pacote(false, false, true, false, new byte[0], -1, myIp, IPAddress.getHostAddress());
            System.out.println("FROM UDPClient: Enviei FIN" + fim.toString());
            sendData = fim.pacote2bytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
            clientSocket.send(sendPacket);
            Thread.sleep(100);
        }

        R.join();

        clientSocket.close();
    }
}