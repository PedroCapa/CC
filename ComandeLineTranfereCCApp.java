import java.io.FileOutputStream;
import java.io.File;
import java.net.*;
import java.io.IOException;
import java.io.FileNotFoundException;

class ComandeLineTransfereCCApp{

	public static void main(String args[]) throws FicheiroNaoExisteException{
		try{
  			
  			if(args.length > 0){
  				File filename = new File(args[1]);
				if(args[0].equals("put") && filename.exists()){
					System.out.println("Entrei no upload");
					//enviaFicheiro(args[1]);
				}
				else if(args[0].equals("get")){
					System.out.println("Entrei no download");
					readFile(args[1]);
				}
			}
			else if(args.length == 0){
					System.out.println("Server iniciado");TransfereCC tcc = new TransfereCC(InetAddress.getLocalHost(),4000);
					tcc.iniciaServidor();
  			}
  			else{
  				throw new FicheiroNaoExisteException("O ficheiro não existe");
			}
		}
		catch(FicheiroNaoExisteException e){
			System.out.println(e.getMessage());
		}
		catch(Exception e){
			e.printStackTrace();
		}
		/*catch(ConexaoNaoEstabelecidaException e){
            System.out.println(e.getMessage());
        }*/
	}

	public static void readFile(String filename) throws FileNotFoundException,UnknownHostException,IOException{
		FileOutputStream fos = new FileOutputStream("Teste/Recebi.txt");
		TransfereCC tcc = new TransfereCC(InetAddress.getLocalHost(),4000); //PASSAR A THREAD
		tcc.get(filename);
		byte[] lido;
		//int bytesLidos;
		while((lido = tcc.read(1000)).length!=0){ //Para ler indica-se o maximo de bytes a ler e recebe-se uma array de bytes
            fos.write(lido);
		}
        fos.close();
		//tcc.close();
	}
}