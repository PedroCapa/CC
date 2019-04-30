import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

class Checksum{
	boolean ack;
	boolean syn;
	boolean fin;
	boolean psh;
	byte[] dados;
	Integer offset;
	String origem;
	String destino;
	//
	long checksum;
	static int header_size = 24;
	//

	Checksum(boolean ack, boolean syn, boolean fin, boolean psh, byte[] dados, 
		Integer offset, String origem , String destino, /**/long check/**/){
		this.ack = ack;
		this.syn = syn;
		this.fin = fin;
		this.psh = psh;
		this.dados = dados;
		this.offset = offset;
		this.origem = origem;
		this.destino = destino;
		//
		this.checksum = check;
		//
	}

	Checksum(){
		this.ack = false;
		this.syn = false;
		this.fin = false;
		this.psh = false;
		this.dados = new byte[1];
		this.offset = 0;
		this.origem = null;
		this.destino = null;
		//
		this.checksum = 0;
		//
	}

	Checksum(Checksum p){
		this.ack = p.getAck();
		this.syn = p.getSyn();
		this.fin = p.getFin();
		this.psh = p.getPsh();
		this.dados = p.getDados();
		this.offset = p.getOffset();
		this.origem = p.getOrigem();
		this.destino = p.getDestino();
		//
		this.checksum = p.getChecksum();
		//
	}

	public boolean getAck(){
		return this.ack;
	}

	public boolean getSyn(){
		return this.syn;
	}

	public boolean getFin(){
		return this.fin;
	}

	public boolean getPsh(){
		return this.psh;
	}

	public byte[] getDados(){
		return this.dados;
	}

	public Integer getOffset(){
		return this.offset;
	}

	public String getOrigem(){
		return this.origem;
	}

	public String getDestino(){
		return this.destino;
	}
	//
	public long getChecksum(){
		return this.checksum;
	}
	//
	public String toString() {
		String str = "";
		str += (this.ack) ? "ACK ": "";
		str += (this.syn) ? "SYN ": "";
		str += (this.psh) ? "PSH ": "";
		str += (this.fin) ? "FIN ": "";
		//String data = new String(dados);
		str += this.offset;
		return str;
	}

	byte[] pacote2bytes(){
		//
		byte[] pac = new byte[dados.length + Checksum.header_size];
		//
		pac[0] = (byte)(this.ack?1:0);
		pac[1] = (byte)(this.syn?1:0);
		pac[2] = (byte)(this.fin?1:0);
		pac[3] = (byte)(this.psh?1:0);

		byte [] off;
		off = ByteBuffer.allocate(4).putInt(offset).array();
		System.arraycopy(off, 0, pac, 4, 4);

		String [] ip = new String[4];
		ip = origem.split("\\.");
		for(int i = 0; i < 4; i++){
			off[i] = Integer.valueOf(ip[i]).byteValue();
		}

		System.arraycopy(off, 0, pac, 8, 4);

		ip = destino.toString().split("\\.");
		for(int i = 0; i < 4; i++){
			off[i] = Integer.valueOf(ip[i]).byteValue();
		}
		System.arraycopy(off, 0, pac, 12, 4);
		//
		byte [] check = new byte [8];
		check = ByteBuffer.allocate(Long.BYTES).putLong(this.checksum).array();
		System.arraycopy(check, 0, pac, 16, 8);
		System.arraycopy(dados, 0, pac, Checksum.header_size, dados.length);
		//
		return pac;
	}

	void bytes2pacote(byte[] copia){
		//
		dados = new byte[copia.length - Checksum.header_size];
		//
		ack = copia[0] != 0;
		syn = copia[1] != 0;
		fin = copia[2] != 0;
		psh = copia[3] != 0;

		byte [] off = new byte[4];

		System.arraycopy(copia, 4, off, 0, 4);
		offset = ByteBuffer.wrap(off).getInt();

		System.arraycopy(copia, 8, off, 0, 4);
		origem = off[0] + "." + off[1] + "." + off[2] + "." + off[3];

		System.arraycopy(copia, 12, off, 0, 4);
		destino = off[0] + "." + off[1] + "." + off[2] + "." + off[3];

		//
		byte [] check = new byte [8];
		System.arraycopy(copia, 16, check, 0, 8);
		ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
		bb.put(check);
		bb.flip();
		this.checksum = bb.getLong();
		System.arraycopy(copia, Checksum.header_size, dados, 0, dados.length);
		//
	}

	//
	public boolean check(){
		byte [] c_dados = new byte[this.dados.length];
		byte complement;
		long soma = 0;
		for(int i = 0; i < this.dados.length; i++){
			complement = (byte) ((Math.floor(Math.log(this.dados[i]) / Math.log(2))) + 1);
			c_dados[i] = (byte) (((1 << complement) - 1) ^ this.dados[i]);
			soma += (long) c_dados[i];
		}
		complement = (byte) ((Math.floor(Math.log(soma) / Math.log(2))) + 1);
		return (soma == this.checksum);
	}

	public void setChecksum(){
		byte [] c_dados = new byte[this.dados.length];
		byte complement;
		this.checksum = 0;
		for(int i = 0; i < this.dados.length; i++){
			complement = (byte) ((Math.floor(Math.log(dados[i]) / Math.log(2))) + 1);
			c_dados[i] = (byte) (((1 << complement) - 1) ^ this.dados[i]);
			this.checksum += c_dados[i];
		}
	}

	//
	public boolean synAck(){
		return (this.ack && this.syn);
	}

	public boolean pshAck(){
		return (this.ack && this.psh);
	}

	public boolean acabou(){
		return (this.fin && !this.ack && !this.psh && !this.syn);
	}

	public boolean pshFin(){
		return (this.fin && this.psh);
	}

	public boolean finAck(){
		return (this.fin && this.ack && !this.psh);
	}

	public int tamanhoDados(){
		return this.dados.length;
	}

	public static void main(String args[]){
		byte [] dados = {14, 54, 02, 49, 20, 94, 103, 109, 34, 29, 94, 28, 49, 12};
		Checksum p = new Checksum(false, false, false, false, dados, 0, "127.0.0.1", "127.0.0.1", 0);
		p.setChecksum();
		System.out.println(p.getChecksum() + "----" + p.check());
	}
}

class CompareOffsetPacote implements Comparator<Checksum>{

	public int compare(Checksum p1, Checksum p2){
		return Integer.valueOf(p1.getOffset()).compareTo(Integer.valueOf(p2.getOffset()));
	}
}