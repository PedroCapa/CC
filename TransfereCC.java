import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.io.FileNotFoundException;

class TransfereCC{

	TransfereCC(){}

	void upload(String filename) {
        try{

            AgenteUDP client = new AgenteUDP();
            List<Dados> dados = file2Dados(filename);
            client.upload(dados);
        }
        catch(Exception e){
            System.out.println(e);
        }
	}

	void download(String filename){

        try{    
  		    AgenteUDP client = new AgenteUDP();
      		List<Pacote> pacotes = client.download(filename); //client.download(filename);
            escreveFile(pacotes);
        }
        catch(Exception e){
            System.out.println(e);
        }
  		
	}

    void iniciaServidor(){
        try{
            List<Pacote> pacotes = new ArrayList<>();
            AgenteUDP udp = new AgenteUDP();
            pacotes = udp.servidor();
            escreveFile(pacotes);
        }
        catch(IOException e){
            System.out.println(e);
        }
        catch(Exception e){
            System.out.println(e);
        }
    }

    List<Dados> file2Dados(String filename) throws IOException{
        
        File file = new File(filename);
        byte[] fileContent = new byte[(int) file.length()];
        try{
            FileInputStream fis = new FileInputStream(file);
            fis.read(fileContent);
            fis.close();
        }
        catch(IOException e){
            throw new IOException(e);
        }
        
        try{
            List<Dados> dados = new ArrayList<>();
            for(int i = 0; i < fileContent.length; i = i + 1024){
                byte [] copia;
                if(fileContent.length - i > 1024){
                    copia = new byte[1024];
                    System.arraycopy(fileContent, i, copia, 0, 1024);
                }
                else{
                    copia = new byte[(fileContent.length - i)];
                    System.arraycopy(fileContent, i, copia, 0, fileContent.length - i);
                }
                Dados d = new Dados(copia, i);
                System.out.println(d);
                dados.add(d);
            }
            return dados;
        }
        catch(Exception e){
            System.out.println(e);
        }
        return (new ArrayList<Dados>());
    }

    public void escreveFile(List<Pacote> pacotes){
        pacotes.sort(new CompareOffsetPacote());
        List<Byte> data = new ArrayList<>();
        for(Pacote p: pacotes){
            for(byte b: p.getDados())
                data.add(b);
        }

        byte[] bin_file = new byte[data.size()];

        int j = 0;

        for(Byte b: data)
            bin_file[j++] = b.byteValue();

        try {
            FileOutputStream fos = new FileOutputStream("Teste/Recebi.txt");
            fos.write(bin_file);
            fos.close();
        }
        catch(IOException exc){}
    }
}