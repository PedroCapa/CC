import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.net.*;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;

class ComandeLineTransfereCCApp{

	public static void main(String args[]) throws FicheiroNaoExisteException{
		try{
  			
  			if(args.length > 0){
  				File filename = new File(args[2]);
				if(args[1].equals("put") && filename.exists()){
					System.out.println("Entrei no upload");
					writeFile(args[0],args[2]);
				}
				else if(args[1].equals("get")){
					System.out.println("Entrei no download");
					readFile(args[0],args[2]);
				}
			}
			else if(args.length == 0){
					System.out.println("Server iniciado");
					TransfereCC tcc = new TransfereCC(7777);
					tcc.iniciaServidor();
  			}
  			else{
  				throw new FicheiroNaoExisteException("O ficheiro não existe");
			}
		}
		catch(FicheiroNaoExisteException e){
			System.out.println(e.getMessage());
		}
		catch(ConexaoNaoEstabelecidaException e){
			System.out.println(e.getMessage());
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void readFile(String ip, String filename) throws FileNotFoundException,UnknownHostException,IOException,ConexaoNaoEstabelecidaException{
		FileOutputStream fos = new FileOutputStream("Teste/Recebi.txt");
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		TransfereCC tcc = new TransfereCC();
		tcc.connect(InetAddress.getByName(ip),4000);
		tcc.get(filename);
		byte[] lido;
		//int bytesLidos;
		while((lido = tcc.read(1000))!=null){ //Para ler indica-se o maximo de bytes a ler e recebe-se uma array de bytes
            bos.write(lido);   
		}
		bos.flush();
        fos.close();
		tcc.disconnect();
		tcc.close();
	}

	public static void writeFile(String ip, String filename) throws FileNotFoundException,UnknownHostException,IOException,ConexaoNaoEstabelecidaException{
		FileInputStream fis = new FileInputStream(filename);
		BufferedInputStream bis = new BufferedInputStream(fis);
		TransfereCC tcc = new TransfereCC();
		tcc.connect(InetAddress.getByName(ip),4000);
		tcc.put(filename);
		byte[] lant = new byte[1000], lido = new byte[1000];
		int blant = bis.read(lant);
		int bytesLidos = bis.read(lido);
		while(bytesLidos!=-1){ //Para ler indica-se o maximo de bytes a ler e recebe-se uma array de bytes
            tcc.write(lant,blant);
			blant = bytesLidos;
			lant = lido;
			lido = new byte[1000];
			bytesLidos = bis.read(lido);
		}
		tcc.writeFin(lant,blant);

		bis.close();
		fis.close();
		tcc.disconnect();
		tcc.close();
	}
}
