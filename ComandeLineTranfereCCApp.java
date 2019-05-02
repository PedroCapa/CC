import java.io.File;

class ComandeLineTransfereCCApp{

	public static void main(String args[]) throws FicheiroNaoExisteException{
		try{
  			TransfereCC tcc = new TransfereCC();
  			if(args.length > 0){
  				File filename = new File(args[1]);
				if(args[0].equals("put") && filename.exists()){
					System.out.println("Entrei no upload");
					enviaFicheiro(args[1]);
				}
				else if(args[0].equals("get")){
					System.out.println("Entrei no download");
					readFile(args[1]);
				}
			}
			else if(args.length == 0){
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
	}

	public void readFile(String filename){
		FileOutputStream fos = new FileOutputStream("Teste/Recebi.txt");
		TransfereCC tcc = new TransfereCC("localhost");
		tcc.get(filename);
		byte[] lido;
		int bytesLidos;
		while((bytesLidos = tcc.read(lido))!=0){
            fos.write(lido);
		}
        fos.close();
		tcc.close();
	}
}