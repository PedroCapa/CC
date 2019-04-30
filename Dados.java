class Dados{
	byte[] dados;
	Integer offset;

	Dados(byte[] dados, Integer offset){
		this.dados = dados;
		this.offset = offset;
	}

	Dados(){
		this.dados = new byte[1];
		this.offset = 0;
	}

	public byte[] getDados(){
		return this.dados;
	}

	public Integer getOffset(){
		return this.offset;
	}

	public Integer tamanhoDados(){
		return this.dados.length;
	}

	public String toString() {
		String str = this.offset + " ";
		String data = new String(dados);
		str += data;
		return str;
	}
}