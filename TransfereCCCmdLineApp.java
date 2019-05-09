import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.net.*;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;

class  TransfereCCCmdLineApp{

	public static void main(String args[]){
		try{
  			
  			if(args.length > 0){
  				File filename = new File(args[2]);
				if(args[1].equals("put") && filename.exists()){
					System.out.println("Entrei no upload");
					writeFile(args[0],args[2],args[3]);
				}
				else if(args[1].equals("get")){
					System.out.println("Entrei no download");
					readFile(args[0],args[2],args[3]);
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
		catch(FileNotFoundException e){
			System.out.println(e.getMessage());
		}
		catch(ConexaoNaoEstabelecidaException e){
			System.out.println(e.getMessage());
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	*Metodo que irá criar um ficheiro localmente a partir de um ficheiro remoto
	*
	*@param ip IP do servidor onde se encontra o ficheiro
	*@param filename Nome do ficheiro a descarregar do servidor
	*@param filenameLocal Nome do ficheiro a ser criado localmente
	*/
	public static void readFile(String ip, String filename, String filenameLocal) throws UnknownHostException,IOException,ConexaoNaoEstabelecidaException{
		FileOutputStream fos = new FileOutputStream(filenameLocal);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		TransfereCC tcc = new TransfereCC();
		tcc.connect(InetAddress.getByName(ip),32000);
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

	/**
	*Metodo que irá criar um ficheiro no servidor a partir de um ficheiro local
	*
	*@param ip IP do servidor para onde o ficheiro deverá ser entregue
	*@param filename Nome do ficheiro a ser entregue
	*@param filenameServer Nome do ficheiro a ser criado no servidor
	*/
	public static void writeFile(String ip, String filename, String filenameServer) throws FileNotFoundException,UnknownHostException,IOException,ConexaoNaoEstabelecidaException{
		FileInputStream fis = new FileInputStream(filename);
		BufferedInputStream bis = new BufferedInputStream(fis);
		TransfereCC tcc = new TransfereCC();
		tcc.connect(InetAddress.getByName(ip),32000);
		tcc.put(filenameServer);
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
