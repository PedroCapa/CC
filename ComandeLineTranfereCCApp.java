

class ComandeLineTransfereCCApp{

	public static void main(String args[]) {
		
		String filename = args[1];
		
  		TransfereCC tcc = new TransfereCC();
		if(args[0].equals("put")){
			tcc.upload(filename);
		}

		if(args[0].equals("get")){
			tcc.download(filename);
		}
	}

}