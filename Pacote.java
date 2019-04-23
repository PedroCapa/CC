import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

class Pacote{
	boolean ack;
	boolean syn;
	boolean fin;
	boolean psh;
	byte[] dados;
	Integer offset;
	String origem;
	String destino;

	Pacote(boolean ack, boolean syn, boolean fin, boolean psh, byte[] dados, Integer offset, String origem , String destino){
		this.ack = ack;
		this.syn = syn;
		this.fin = fin;
		this.psh = psh;
		this.dados = dados;
		this.offset = offset;
		this.origem = origem;
		this.destino = destino;
	}

	Pacote(){
		this.ack = false;
		this.syn = false;
		this.fin = false;
		this.psh = false;
		this.dados = new byte[1];
		this.offset = 0;
		this.origem = null;
		this.destino = null;
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

	public String toString() {
		String str = "";
		str += (this.ack) ? "ACK ": "";
		str += (this.syn) ? "SYN ": "";
		str += (this.psh) ? "PSH ": "";
		str += (this.fin) ? "FIN ": "";
		String data = new String(dados);
		str += data;
		return str;
	}

	byte[] pacote2bytes(){
		byte[] pac = new byte[dados.length + 16];

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
		System.arraycopy(dados, 0, pac, 16, dados.length);

		return pac;
	}

	void bytes2pacote(byte[] copia){
		dados = new byte[copia.length - 16];

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

		System.arraycopy(copia, 16, dados, 0, dados.length);
	}


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
}

class CompareOffsetPacote implements Comparator<Pacote>{

	public int compare(Pacote p1, Pacote p2){
		return Integer.valueOf(p1.getOffset()).compareTo(Integer.valueOf(p2.getOffset()));
	}
}