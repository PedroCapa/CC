import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

class Pacote{
	private boolean ack;
	private boolean syn;
	private boolean fin;
	private boolean psh;
	private boolean req; //Pedido
	private byte[] dados;
	private int window;
	private int offset;

	Pacote(boolean ack, boolean syn, boolean fin, boolean psh, boolean req, byte[] dados, int window, int offset){
		this.ack = ack;
		this.syn = syn;
		this.fin = fin;
		this.psh = psh;
		this.req = req;
		this.dados = dados;
		this.window = window;
		this.offset = offset;
	}

	Pacote(){
		this.ack = false;
		this.syn = false;
		this.fin = false;
		this.psh = false;
		this.dados = new byte[1];
		this.offset = 0;
	}

	Pacote(Pacote p){
		this.ack = p.getAck();
		this.syn = p.getSyn();
		this.fin = p.getFin();
		this.psh = p.getPsh();
		this.dados = p.getDados();
		this.offset = p.getOffset();
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

	public boolean getReq(){
		return this.req;
	}

	public byte[] getDados(){
		return this.dados;
	}

	public int getOffset(){
		return this.offset;
	}

	public int getWindow(){
		return this.window;
	}


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
		byte[] pac = new byte[dados.length + 9];

		int flags = this.ack?1:0;
		flags += this.syn?2:0;
		flags += this.fin?4:0;
		flags += this.psh?8:0;
		flags += this.req?16:0;

		pac[0] = (byte)flags;
		byte [] off;
		off = ByteBuffer.allocate(4).putInt(offset).array();
		System.arraycopy(off, 0, pac, 1, 4);

		byte [] win;
		win = ByteBuffer.allocate(4).putInt(window).array();
		System.arraycopy(win, 0, pac, 5, 4);
		System.arraycopy(dados, 0, pac, 9, dados.length);

		return pac;
	}

	void bytes2pacote(byte[] copia){
		dados = new byte[copia.length - 9];

		int flags = (int)copia[0];
		ack = (flags % 2) == 1;
		flags = flags >> 1;
		syn = (flags % 2) == 1;
		flags = flags >> 1;
		fin = (flags % 2) == 1;
		flags = flags >> 1;
		psh = (flags % 2) == 1;
		flags = flags >> 1;
		req = (flags % 2) == 1;
		byte [] off = new byte[4];

		System.arraycopy(copia, 1, off, 0, 4);
		offset = ByteBuffer.wrap(off).getInt();

		System.arraycopy(copia, 5, off, 0, 4);
		window = ByteBuffer.wrap(off).getInt();
		System.arraycopy(copia, 9, dados, 0, dados.length);
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

	public int tamanhoDados(){
		return this.dados.length;
	}

}
