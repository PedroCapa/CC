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

    public void enviaPacoteServidor(Pacote p, int port){
        try{
            byte[] sendData = new byte[1024];
            System.out.println("FROM: UDPServer: Enviei " + p.toString());
            sendData = p.pacote2bytes();
            this.udpPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
            udpSocket.send(udpPacket);
        }
        catch(IOException e){
            System.out.println(e);
        }
    }

    public List<Dados> getFile(byte [] tipo_conexao) throws IOException, FicheiroNaoExisteException{
        try{
            TransfereCC tcc = new TransfereCC();
            String str = new String(tipo_conexao);
            String [] filename = str.split(" ");
            //Testar se o filename tem tamanha correto e se ficheiro existe
            if(filename.length > 0){
                String name = filename[1];
                List<Dados> dados = tcc.file2Dados(name);
                System.out.println("E para fazer download " + filename[1].length());
                return dados;
            }
            else{
                //Trocar IOException por uma Excecao criada
                throw new FicheiroNaoExisteException("O ficheiro nao existe ficheiro\n");
            }
        }
        catch(IOException e){
            throw new IOException(e);
        }
        catch(FicheiroNaoExisteException e){
            throw new FicheiroNaoExisteException(e.getMessage());
        }
    }

    public void downloadServer(Estado estado, List<Dados> dados, int port){
        
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
            enviaPacoteServidor(enviar, port);
            // Verificar se o ACK que recebeu está correto e em caso afirmativo envia o ultimo ACK que tiver
            if(estado.reenvia()){
                System.out.println("E preciso reenviar ");
                Integer indice = estado.getLastACK();
                Pacote reenvio = estado.getPacotes().get(indice);
                enviaPacote(reenvio);
                estado.setFase(2);
            }
        }

        while(estado.getFase() == 5 || estado.getFase() == 2){
            if(estado.getFase() == 5){
                System.out.println("FROM: UDPClient: E necessario reenviar ");
                Integer indice = estado.getLastACK();
                Pacote reenvio = estado.getPacotes().get(indice);
                enviaPacote(reenvio);
                estado.esperaRecebe();
            }

            if(estado.getACK().size() == dados.size())
                estado.setFase(3);
        }
    }

    public void uploadServer(Estado estado, int port){
        while(estado.getFase() == 2 || estado.getACK().size() != 0){
            if(estado.getFase() == 2 || estado.getACK().size() == 0)
                estado.esperaRecebe();
            byte [] sendData = new byte[1024];
            Integer i = estado.enviaAck();
            if(i != -1){
                Pacote pshAck = new Pacote(true, false, false, true, new byte[0], i, estado.getDestino(), estado.getOrigem());
                enviaPacoteServidor(pshAck, port);
            }
        }
    }

    public List<Pacote> servidor() throws Exception, IOException, FicheiroNaoExisteException{
        this.udpSocket = new DatagramSocket(7777);
        Lock lock = new ReentrantLock();
        Condition espera = lock.newCondition();
        Condition controlo = lock.newCondition();
        byte[] receiveData = new byte[1024];
        byte[] sendData = new byte[1024];
 
        //Inicio da conexão
        this.udpPacket = new DatagramPacket(receiveData, receiveData.length);
        this.udpSocket.receive(udpPacket);
        Pacote syn = new Pacote();
        syn.bytes2pacote(udpPacket.getData());
        System.out.println("FROM: UDPServer: Recebi " + syn.toString());
        
        receiveData = syn.getDados();
        char tipo_conexao = (char)receiveData[0];

        List<Dados> dados = new ArrayList<>();

        try{
            if(tipo_conexao == 'd'){
                dados = getFile(receiveData);
            }
        }
        catch(IOException e){
            throw new IOException(e);
        }

        catch(FicheiroNaoExisteException e){
            throw new FicheiroNaoExisteException(e.getMessage());
        }

        this.IPAddress = udpPacket.getAddress();// substituir por syn.getOrigem ???
        int port = udpPacket.getPort();                 // Ver o q faz

        //System.out.println(this.IPAddress.toString());

        Estado estado = new Estado(new ArrayList<>(), syn.getDestino(), syn.getOrigem(), new ArrayList<>(), 1, dados.size(), lock, espera, controlo);
        
        ControloConexaoServidor ccs = new ControloConexaoServidor(estado, udpPacket, udpSocket);
        ccs.start();

        while(estado.getFase() != 2){
            Pacote synAck = new Pacote(true, true, false, false, new byte[0], -1, syn.getDestino(), syn.getOrigem());
            enviaPacoteServidor(synAck, port);
            Thread.sleep(200);
        }

        if(tipo_conexao == 'u'){
            RecebePacotes rp = new RecebePacotes(estado, this.udpPacket, this.udpSocket);
            rp.start();
            uploadServer(estado, port);
            rp.join();
        }
        else if(tipo_conexao == 'd'){
            RecebeACK rp = new RecebeACK(estado, this.udpSocket);
            rp.start();
            downloadServer(estado, dados, port);
            rp.join();
        }
        estado.acordaControlo();

        while (estado.getFase() != 4) {
            estado.esperaRecebe();
        }

        Pacote finAck = new Pacote(true, false, true, false, new byte[0], -1, syn.getDestino(), syn.getOrigem());
        enviaPacoteServidor(finAck, port);

        ccs.join();
        udpSocket.close();

        return estado.getPacotes();
        }

    public void enviaPacote(Pacote p){
        try{
            System.out.println("FROM: UDPClient: Enviei" + p);
            byte[] sendData = new byte[1024];
            sendData = p.pacote2bytes();
            this.udpPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 7777);
            this.udpSocket.send(this.udpPacket);
        }
        catch(IOException e){
            System.out.println(e);
        }
    }

    public List<Pacote> download(String filename) throws Exception, ConexaoNaoEstabelecidaException{
        Estado estado = new Estado();
        Lock lock = new ReentrantLock();
        Condition espera = lock.newCondition();
        Condition controlo = lock.newCondition();
        String myIp = InetAddress.getLocalHost().getHostAddress();
        this.udpSocket = new DatagramSocket();
        this.IPAddress = InetAddress.getByName("localhost");
        List<Pacote> dados = new ArrayList<>();

        estado = new Estado(new ArrayList<>(), myIp, IPAddress.getHostAddress(), new ArrayList<>(), 0, dados.size(), lock, espera, controlo);
        //Começar a thread receberACK
        ControloConexaoCliente ccc = new ControloConexaoCliente(estado, udpSocket);
        ccc.start();
     
        //Incio da conexão
        //Um pacote SYN nao recebe dados
        int tentativas = 0;
        String syn_data = "d " + filename + " "; // O problema e que o filename tem 1006 de tamanhod
        while(estado.getFase() == 0 && tentativas < 10){
            Pacote inicio = new Pacote(false, true, false, false, syn_data.getBytes(), -1, myIp, IPAddress.getHostAddress());
            enviaPacote(inicio);
            Thread.sleep(100);
            tentativas++;
        }

        if(tentativas == 10){
            ccc.interrupt();
            udpSocket.close();
            throw new ConexaoNaoEstabelecidaException("Não foi possível estabelecer uma conexão");
        }

        Pacote synAck = new Pacote(true, true, false, false, new byte[0], -1, myIp, IPAddress.getHostAddress());
        enviaPacote(synAck);

        RecebePacotes R = new RecebePacotes(estado, udpPacket, udpSocket);
        R.start();

        while(estado.getFase() == 2 || estado.getACK().size() != 0){
            if(estado.getFase() == 2 || estado.getACK().size() == 0)
                estado.esperaRecebe();
            byte [] sendData = new byte[1024];
            Integer i = estado.enviaAck();
            if(i != -1){
                Pacote pshAck = new Pacote(true, false, false, true, new byte[0], i, estado.getDestino(), estado.getOrigem());
                enviaPacote(pshAck);
            }
        }

        R.join();
        estado.acordaControlo();

        //Vai adormecer até receber o ultimo ACK em que vai ser um ciclo pq pode ter de reenviar pacotes
        // Fim da conexão
        estado.esperaRecebe();
        while(estado.getFase() == 3){
            Pacote fim = new Pacote(false, false, true, false, new byte[0], (dados.size() + 1) * 1024, myIp, IPAddress.getHostAddress());
            enviaPacote(fim);
            Thread.sleep(100);
        }

        ccc.join();
        udpSocket.close();

        return estado.getPacotes();
    }

    public void upload(List<Dados> dados) throws Exception, ConexaoNaoEstabelecidaException{
        Estado estado = new Estado();
        Lock lock = new ReentrantLock();
        Condition espera = lock.newCondition();
        Condition controlo = lock.newCondition();
        String myIp = InetAddress.getLocalHost().getHostAddress();
        this.udpSocket = new DatagramSocket();
        //this.IPAddress = InetAddress.getByAddress(new byte[]{(byte) 172, (byte)26, (byte)100, (byte)157});
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
            enviaPacote(inicio);
            Thread.sleep(100);
            tentativas++;
        }

        if(tentativas == 10){
            ccc.interrupt();
            udpSocket.close();
            throw new ConexaoNaoEstabelecidaException("Não foi possível estabelecer uma conexão");
        }

        RecebeACK R = new RecebeACK(estado, udpSocket);
        R.start();

        Pacote synAck = new Pacote(true, true, false, false, new byte[0], -1, myIp, IPAddress.getHostAddress());
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
        while(estado.getFase() == 5 || estado.getFase() == 2){
            if(estado.getFase() == 5){
                System.out.println("FROM: UDPClient: E necessario reenviar ");
                Integer indice = estado.getLastACK();
                Pacote reenvio = estado.getPacotes().get(indice);
                enviaPacote(reenvio);
                estado.esperaRecebe();
            }

            if(estado.getACK().size() == dados.size())
                estado.setFase(3);
        }

        R.join();
        estado.acordaControlo();

        //Vai adormecer até receber o ultimo ACK em que vai ser um ciclo pq pode ter de reenviar pacotes
        // Fim da conexão
        while(estado.getFase() == 3){
            Pacote fim = new Pacote(false, false, true, false, new byte[0], (dados.size() + 1) * 1024, myIp, IPAddress.getHostAddress());
            enviaPacote(fim);
            Thread.sleep(100);
        }

        ccc.join();
        udpSocket.close();
    }
}