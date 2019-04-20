import java.io.File;

class ComandeLineTransfereCCApp{

	public static void main(String args[]) {
		
  		TransfereCC tcc = new TransfereCC();
  		File filename = new File(args[1]);

		if(args[0].equals("put") && filename.exists()){
			System.out.println("Entrei no upload");
			tcc.upload(args[1]);
		}

		else if(args[0].equals("get")){
			System.out.println("Entrei no download");
			tcc.download(args[1]);
		}
		else{
			System.out.println("Operação impossível");
		}
	}

}