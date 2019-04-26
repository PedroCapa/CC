
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import java.util.List;

/*O agenteUDP so envia e recebe pacotes, o transporteUDP faz o resto
   TRansfereCC processa o pedido de conexao
   Enviar e receber datagramas e o agenteUDP
*/

// Para ja isto pode ser usado para o cliente indicar que ficheiro quer receber e se a conexão foi aceite
class UDPClient{

    private DatagramSocket clientSocket;
    private DatagramPacket sendPacket;
    private InetAddress IPAddress;

    public void enviaPacote(Pacote p){
        try{
            byte[] sendData = new byte[1024];
            sendData = p.pacote2bytes();
            this.sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
            this.clientSocket.send(this.sendPacket);
        }
        catch(IOException e){
            System.out.println(e);
        }
    }
/*
    public List<Pacote> download(String filename){
        Estado estado = new Estado();
        Lock lock = new ReentrantLock();
        Condition espera = lock.newCondition();
        Condition controlo = lock.newCondition();
        String myIp = InetAddress.getLocalHost().getHostAddress();
        this.clientSocket = new DatagramSocket();
        this.IPAddress = InetAddress.getByName("localhost");

        estado = new Estado(new ArrayList<>(), myIp, IPAddress.getHostAddress(), new ArrayList<>(), 0, dados.size(), lock, espera, controlo);
        //Começar a thread receberACK
        ControloConexaoCliente ccc = new ControloConexaoCliente(estado, clientSocket);
        ccc.start();
     
        //Incio da conexão
        //Um pacote SYN nao recebe dados
        int tentativas = 0;
        while(estado.getFase() == 0 && tentativas < 10){
            Pacote inicio = new Pacote(false, true, false, false, "d" + filename, -1, myIp, IPAddress.getHostAddress());
            System.out.println("FROM: UDPClient: Enviei " + inicio.toString());
            enviaPacote(inicio);
            Thread.sleep(100);
            tentativas++;
        }

        if(tentativas == 10){
            ccc.interrupt();
            System.out.println("Nao foi aceite o pedido");
            clientSocket.close();
            return;
        }

        Pacote synAck = new Pacote(true, true, false, false, new byte[0], -1, myIp, IPAddress.getHostAddress());
        System.out.println("FROM: UDPClient: Enviei " + synAck.toString());
        enviaPacote(synAck);
        Thread.sleep(300);

        RecebePacotes R = new RecebePacotes(estado, clientSocket);
        R.start();

        while(estado.getFase() == 2){
            estado.esperaRecebe();
            Pacote pshAck = new Pacote(true, false, false, true, new byte[0], estado.getLastACK(), syn.getDestino(), syn.getOrigem());
            sendData = pshAck.pacote2bytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); // Ver o IPAddress e a port
            serverSocket.send(sendPacket);
            System.out.println("FROM: UDPClient: Enviei " + pshAck.toString());
        }

        R.join();
        estado.acordaControlo();

        //Vai adormecer até receber o ultimo ACK em que vai ser um ciclo pq pode ter de reenviar pacotes
        // Fim da conexão
        while(estado.getFase() != 3){
            Pacote fim = new Pacote(false, false, true, false, new byte[0], (dados.size() + 1) * 1000, myIp, IPAddress.getHostAddress());
            System.out.println("FROM UDPClient: Enviei " + fim.toString());
            enviaPacote(fim);
            Thread.sleep(100);
        }

        ccc.join();
        clientSocket.close();

    }
*/
    /*
    public void upload(List<Dados> dados) throws Exception{
        Estado estado = new Estado();
        Lock lock = new ReentrantLock();
        Condition espera = lock.newCondition();
        Condition controlo = lock.newCondition();
        String myIp = InetAddress.getLocalHost().getHostAddress();
        this.clientSocket = new DatagramSocket();
        this.IPAddress = InetAddress.getByName("localhost");

        estado = new Estado(new ArrayList<>(), myIp, IPAddress.getHostAddress(), new ArrayList<>(), 0, dados.size(), lock, espera, controlo);
        //Começar a thread receberACK
        ControloConexaoCliente ccc = new ControloConexaoCliente(estado, clientSocket);
        ccc.start();
     
        //Incio da conexão
        //Um pacote SYN nao recebe dados
        String tipo = "u";
        byte [] tipo_conexao = tipo.getBytes();
        int tentativas = 0;
        while(estado.getFase() == 0 && tentativas < 10){
            Pacote inicio = new Pacote(false, true, false, false, tipo_conexao, -1, myIp, IPAddress.getHostAddress());
            System.out.println("FROM: UDPClient: Enviei " + inicio.toString());
            enviaPacote(inicio);
            Thread.sleep(100);
            tentativas++;
        }

        if(tentativas == 10){
            ccc.interrupt();
            System.out.println("Nao foi aceite o pedido");
            clientSocket.close();
            return;
        }

        Pacote synAck = new Pacote(true, true, false, false, new byte[0], -1, myIp, IPAddress.getHostAddress());
        System.out.println("FROM: UDPClient: Enviei " + synAck.toString());
        enviaPacote(synAck);
        Thread.sleep(300);

        RecebeACK R = new RecebeACK(estado, clientSocket);
        R.start();

        //Recebe o ACK
        //Passar esta parte para o RecebeACK e substituir por um await para esperar pela resposta

        //Envia ACK a dizer que vai começar a transmitir os dados
        // Envio de dados Nesta parte e para criar pacotes de acordo com a lista de bytes e enviar para o Servidor
        int tamanho = dados.size();
        for(int i = 0; i < tamanho; i++){
            Dados d = dados.get(i);
            Pacote enviar;
            if(i == tamanho - 1){
                enviar = new Pacote(false, false, true, true, d.getDados(), d.getOffset(), myIp, IPAddress.getHostAddress());
            }
            else{
                enviar = new Pacote(false, false, false, true, d.getDados(), d.getOffset(), myIp, IPAddress.getHostAddress());
            }
            estado.addPacote(enviar);
            System.out.println("FROM: UDPClient: Enviei Pacote " + enviar.toString());
            enviaPacote(enviar);
            // Verificar se o ACK que recebeu está correto e em caso afirmativo envia o ultimo ACK que tiver
            if(estado.reenvia()){
                System.out.println("E preciso reenviar ");
                Integer indice = estado.getLastACK();
                Pacote reenvio = estado.getPacotes().get(indice);
                enviaPacote(reenvio);
                estado.setFase(2);
            }
        }

        estado.esperaRecebe();
        System.out.println("FROM: UDPClient: Ja foi tudo enviado ");
        while(estado.getFase() == 4 || estado.getFase() == 1){
            if(estado.getFase() == 4){
                System.out.println("FROM: UDPClient: E necessario reenviar ");
                Integer indice = estado.getLastACK();
                Pacote reenvio = estado.getPacotes().get(indice);
                enviaPacote(reenvio);
                estado.esperaRecebe();
            }

            if(estado.getACK().size() == dados.size())
                estado.setFase(2);
        }

        R.join();
        estado.acordaControlo();

        //Vai adormecer até receber o ultimo ACK em que vai ser um ciclo pq pode ter de reenviar pacotes
        // Fim da conexão
        while(estado.getFase() != 3){
            Pacote fim = new Pacote(false, false, true, false, new byte[0], (dados.size() + 1) * 1000, myIp, IPAddress.getHostAddress());
            System.out.println("FROM UDPClient: Enviei " + fim.toString());
            enviaPacote(fim);
            Thread.sleep(100);
        }

        ccc.join();
        clientSocket.close();
    }
*/
}