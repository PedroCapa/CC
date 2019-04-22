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
class AgenteUDP{

    private DatagramSocket udpSocket;
    private DatagramPacket udpPacket;
    private InetAddress IPAddress;


    public void downloadServer(Estado estado, byte [] tipo_conexao){
        TransfereCC tcc = new TransfereCC();
        String str = new String(tipo_conexao);
        String [] filename = str.split(" ");
        //Testar se o filename tem tamanha correto e se ficheiro existe
        List<Dados> dados = tcc.file2Dados(filename[1]);
        int tamanho = dados.size();
        for(int i = 0; i < tamanho; i++){
            Dados d = dados.get(i);
            Pacote enviar = new Pacote();
            if(i == tamanho - 1){
                try{
                    enviar = new Pacote(false, false, true, true, d.getDados(), d.getOffset(), IPAddress.getLocalHost().getHostAddress(), IPAddress.getHostAddress());
                }
                catch(UnknownHostException e){
                    System.out.println(e);

                }
            }
            else{
                try{
                    enviar = new Pacote(false, false, false, true, d.getDados(), d.getOffset(), IPAddress.getLocalHost().getHostAddress(), IPAddress.getHostAddress());
                }
                catch(UnknownHostException e){
                    System.out.println(e);
                }
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
    }

    public void uploadServer(Estado estado, int port){
        while(estado.getFase() == 2){
            try{
                byte [] sendData = new byte[1024];
                estado.esperaRecebe();
                Pacote pshAck = new Pacote(true, false, false, true, new byte[0], estado.getLastACK(), estado.getDestino(), estado.getOrigem());
                sendData = pshAck.pacote2bytes();
                this.udpPacket = new DatagramPacket(sendData, sendData.length, this.IPAddress, port);
                this.udpSocket.send(udpPacket);
                System.out.println("FROM: UDPServer: Enviei " + pshAck.toString());
            }
            catch(IOException e){
                System.out.println(e);
            }
        }
    }

    public List<Pacote> servidor() throws Exception{
        this.udpSocket = new DatagramSocket(9876);
        Lock lock = new ReentrantLock();
        Condition espera = lock.newCondition();
        Condition controlo = lock.newCondition();
        byte[] receiveData = new byte[10024];
        byte[] sendData = new byte[1024];
 
        //Inicio da conexão
        this.udpPacket = new DatagramPacket(receiveData, receiveData.length);
        this.udpSocket.receive(udpPacket);
        Pacote syn = new Pacote();
        syn.bytes2pacote(udpPacket.getData());
        System.out.println("FROM: UDPServer: Recebi " + syn.toString());
        
        receiveData = syn.getDados();
        char tipo_conexao = (char)receiveData[0];

        this.IPAddress = udpPacket.getAddress();// substituir por syn.getOrigem ???
        int port = udpPacket.getPort();                 // Ver o q faz

        Estado estado = new Estado(new ArrayList<>(), syn.getDestino(), syn.getOrigem(), new ArrayList<>(), 1, 0, lock, espera, controlo);
        
        ControloConexaoServidor ccs = new ControloConexaoServidor(estado, udpPacket, udpSocket);
        ccs.start();

        while(estado.getFase() != 2){
            Pacote synAck = new Pacote(true, true, false, false, new byte[0], -1, syn.getDestino(), syn.getOrigem());
            sendData = synAck.pacote2bytes();
            DatagramPacket udpPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); // Ver o IPAddress e a port
            udpSocket.send(udpPacket);
            System.out.println("FROM: UDPServer: Enviei " + synAck.toString());
            Thread.sleep(200);
        }

        RecebePacotes rp = new RecebePacotes(estado, udpPacket, udpSocket);
        rp.start();

        if(tipo_conexao == 'u'){
            uploadServer(estado, port);
        }
        else if(tipo_conexao == 'd'){
            downloadServer(estado, receiveData);
        }

        rp.join();
        estado.acordaControlo();

        while (estado.getFase() != 4) {
            estado.esperaRecebe();
        }

        Pacote finAck = new Pacote(true, false, true, false, new byte[0], -1, syn.getDestino(), syn.getOrigem());
        sendData = finAck.pacote2bytes();
        DatagramPacket udpPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); // Ver o IPAddress e a port
        udpSocket.send(udpPacket);
        System.out.println("FROM: UDPServer: Enviei " + finAck.toString());

        ccs.join();
        udpSocket.close();

        return estado.getPacotes();
        }

    public void enviaPacote(Pacote p){
        try{
            byte[] sendData = new byte[1024];
            sendData = p.pacote2bytes();
            this.udpPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 9876);
            this.udpSocket.send(this.udpPacket);
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
        this.udpSocket = new DatagramSocket();
        this.IPAddress = InetAddress.getByName("localhost");

        estado = new Estado(new ArrayList<>(), myIp, IPAddress.getHostAddress(), new ArrayList<>(), 0, dados.size(), lock, espera, controlo);
        //Começar a thread receberACK
        ControloConexaoCliente ccc = new ControloConexaoCliente(estado, udpSocket);
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
            udpSocket.close();
            return;
        }

        Pacote synAck = new Pacote(true, true, false, false, new byte[0], -1, myIp, IPAddress.getHostAddress());
        System.out.println("FROM: UDPClient: Enviei " + synAck.toString());
        enviaPacote(synAck);
        Thread.sleep(300);

        RecebePacotes R = new RecebePacotes(estado, udpSocket);
        R.start();

        while(estado.getFase() == 2){
            estado.esperaRecebe();
            Pacote pshAck = new Pacote(true, false, false, true, new byte[0], estado.getLastACK(), syn.getDestino(), syn.getOrigem());
            sendData = pshAck.pacote2bytes();
            DatagramPacket udpPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); // Ver o IPAddress e a port
            udpSocket.send(udpPacket);
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
        udpSocket.close();

    }
*/
    public void upload(List<Dados> dados) throws Exception{
        Estado estado = new Estado();
        Lock lock = new ReentrantLock();
        Condition espera = lock.newCondition();
        Condition controlo = lock.newCondition();
        String myIp = InetAddress.getLocalHost().getHostAddress();
        this.udpSocket = new DatagramSocket();
        this.IPAddress = InetAddress.getByName("localhost");

        estado = new Estado(new ArrayList<>(), myIp, IPAddress.getHostAddress(), new ArrayList<>(), 0, dados.size(), lock, espera, controlo);
        //Começar a thread receberACK
        ControloConexaoCliente ccc = new ControloConexaoCliente(estado, udpSocket);
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
            udpSocket.close();
            return;
        }

        RecebeACK R = new RecebeACK(estado, udpSocket);
        R.start();

        Pacote synAck = new Pacote(true, true, false, false, new byte[0], -1, myIp, IPAddress.getHostAddress());
        System.out.println("FROM: UDPClient: Enviei " + synAck.toString());
        enviaPacote(synAck);
        Thread.sleep(300);


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
        udpSocket.close();
    }
}