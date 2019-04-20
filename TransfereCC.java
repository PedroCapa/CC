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
		
		File file = new File(filename);
  		byte[] fileContent = new byte[(int) file.length()];
  		try{
			FileInputStream fis = new FileInputStream(file);
  			fis.read(fileContent);
  			fis.close();
  		}
  		catch(IOException e){
            System.out.println("IOException");
        }
		
  		UDPClient client = new UDPClient();
		try{
            List<Dados> dados = new ArrayList<>();
            for(int i = 0; i < fileContent.length; i = i + 1000){
                byte [] copia;
                if(fileContent.length - i > 1000){
                    copia = new byte[1000];
                    System.arraycopy(fileContent, i, copia, 0, 1000);
                }
                else{
                    copia = new byte[(fileContent.length - i)];
                    System.arraycopy(fileContent, i, copia, 0, fileContent.length - i);
                }
                Dados d = new Dados(copia, i);
                System.out.println(d);
                dados.add(d);
            }
			client.upload(dados);
		}
		catch(Exception e){
            System.out.println(e);
        }
	}

	void download(String filename){

  		UDPClient client = new UDPClient();
  		List<Pacote> pacotes = new ArrayList<>(); //client.download(filename);
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
			FileOutputStream fos = new FileOutputStream(filename);
   			fos.write(bin_file);
   			fos.close();
   		}
   		catch(IOException exc){}
	}

}