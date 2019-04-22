import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class UDPServer{
/*
    public void download(Estado estado, byte [] tipo_conexao, InetAddress IPAddress){
        TransfereCC tcc = new TransfereCC();
        String str = new String(tipo_conexao);
        String [] filename = str.split(" ");
        //Testar se o filename tem tamanha correto e se ficheiro existe
        List<Dados> dados = tcc.file2Dados(filename[1]);
        int tamanho = dados.size();
        for(int i = 0; i < tamanho; i++){
            Dados d = dados.get(i);
            Pacote enviar;
            if(i == tamanho - 1){
                enviar = new Pacote(false, false, true, true, d.getDados(), d.getOffset(), IPAddress.getLocalHost().getHostAddress(), IPAddress.getHostAddress());
            }
            else{
                enviar = new Pacote(false, false, false, true, d.getDados(), d.getOffset(), IPAddress.getLocalHost().getHostAddress(), IPAddress.getHostAddress());
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

    public void upload(Estado estado, DatagramSocket serverSocket){
        while(estado.getFase() == 2){
            byte [] sendData = new byte[1024];
            estado.esperaRecebe();
            Pacote pshAck = new Pacote(true, false, false, true, new byte[0], estado.getLastACK(), syn.getDestino(), syn.getOrigem());
            sendData = pshAck.pacote2bytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); // Ver o IPAddress e a port
            serverSocket.send(sendPacket);
            System.out.println("FROM: UDPServer: Enviei " + pshAck.toString());
        }
    }

    public void servidor() throws Exception{
        DatagramSocket serverSocket = new DatagramSocket(9876);
        Lock lock = new ReentrantLock();
        Condition espera = lock.newCondition();
        Condition controlo = lock.newCondition();
        byte[] receiveData = new byte[10024];
        byte[] sendData = new byte[1024];
 
        //Inicio da conexão
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        Pacote syn = new Pacote();
        syn.bytes2pacote(receivePacket.getData());
        System.out.println("FROM: UDPServer: Recebi " + syn.toString());
        
        receiveData = syn.getDados();
        char tipo_conexao = (char)receiveData[0];

        InetAddress IPAddress = receivePacket.getAddress();// substituir por syn.getOrigem ???
        int port = receivePacket.getPort();                 // Ver o q faz

        Estado estado = new Estado(new ArrayList<>(), syn.getDestino(), syn.getOrigem(), new ArrayList<>(), 1, 0, lock, espera, controlo);
        
        ControloConexaoServidor ccs = new ControloConexaoServidor(estado, receivePacket, serverSocket);
        ccs.start();

        while(estado.getFase() != 2){
            Pacote synAck = new Pacote(true, true, false, false, new byte[0], -1, syn.getDestino(), syn.getOrigem());
            sendData = synAck.pacote2bytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); // Ver o IPAddress e a port
            serverSocket.send(sendPacket);
            System.out.println("FROM: UDPServer: Enviei " + synAck.toString());
            Thread.sleep(200);
        }

        RecebePacotes rp = new RecebePacotes(estado, receivePacket, serverSocket);
        rp.start();

        if(tipo_conexao == 'u'){
            upload(estado, serverSocket);
        }
        else if(tipo_conexao == 'd'){
            download(estado, receiveData, InetAddress.getLocalHost().getHostAddress());
        }

        rp.join();
        estado.acordaControlo();

        while (estado.getFase() != 4) {
            estado.esperaRecebe();
        }

        Pacote finAck = new Pacote(true, false, true, false, new byte[0], -1, syn.getDestino(), syn.getOrigem());
        sendData = finAck.pacote2bytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port); // Ver o IPAddress e a port
        serverSocket.send(sendPacket);
        System.out.println("FROM: UDPServer: Enviei " + finAck.toString());

        ccs.join();
        serverSocket.close();
        }
        */
}