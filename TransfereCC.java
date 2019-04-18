import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.io.FileNotFoundException;

class TransfereCC{

	TransfereCC(){}

	void upload(String filename) {
		
		File file = new File(filename);
  		byte[] bin_file = new byte[(int) file.length()];
  		try{
			FileInputStream fis = new FileInputStream(file);
  			fis.read(bin_file);
  			fis.close();
  		}
  		catch(IOException e){}
		
  		UDPClient client = new UDPClient();
		try{
			byte[] nada = new byte[0];
			client.upload(nada);
		}
		catch(Exception e){}
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