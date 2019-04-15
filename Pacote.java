import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

class Pacote{
	boolean ack;
	boolean syn;
	boolean fin;
	boolean psh;
	byte[] dados;
	Integer offset;
	InetAddress origem;
	InetAddress destino;

	Pacote(boolean ack, boolean syn, boolean fin, boolean psh, byte[] dados, Integer offset, InetAddress origem , InetAddress destino){
		this.ack = ack;
		this.syn = syn;
		this.fin = fin;
		this.psh = psh;
		this.dados = dados;
		this.offset = offset;
		this.origem = origem;
		this.destino = destino;
	}

	byte[] pacote2bytes(){
		byte[] pac = new byte[dados.length + 8];

		pac[0] = (byte)(this.ack?1:0);
		pac[1] = (byte)(this.syn?1:0);
		pac[2] = (byte)(this.fin?1:0);
		pac[3] = (byte)(this.psh?1:0);

		byte [] off;
		off = ByteBuffer.allocate(4).putInt(offset).array();
		System.arraycopy(off, 0, pac, 4, 4);

		String [] ip = new String[4];
		ip = origem.toString().split("\\.");
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
		dados = new byte[copia.length - 8];

		ack = copia[0] != 0;
		syn = copia[1] != 0;
		fin = copia[2] != 0;
		psh = copia[3] != 0;

		byte [] off = new byte[4];

		System.arraycopy(copia, 4, off, 0, 4);
		offset = ByteBuffer.wrap(off).getInt();
		try{
			System.arraycopy(copia, 8, off, 0, 4);
			origem = InetAddress.getByAddress(off);		

			System.arraycopy(copia, 12, off, 0, 4);
			destino = InetAddress.getByAddress(off);
		}
		catch(UnknownHostException e){
			System.out.println(e);
		}

		System.arraycopy(copia, 16, dados, 0, dados.length);
	}
}