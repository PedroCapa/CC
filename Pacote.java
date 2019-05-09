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
	private int portaOrigem;
	private int portaDestino;
	private InetAddress ipOrigem;
	private InetAddress ipDestino;
	long checksum;
	static int header_size = 17;

	Pacote(boolean ack, boolean syn, boolean fin, boolean psh, boolean req, byte[] dados, int window, int offset, int portaOrigem, int portaDestino, InetAddress ipOrigem, InetAddress ipDestino){
		this.ack = ack;
		this.syn = syn;
		this.fin = fin;
		this.psh = psh;
		this.req = req;
		this.dados = dados;
		this.window = window;
		this.offset = offset;
		this.portaOrigem = portaOrigem;
		this.portaDestino = portaDestino;
		this.ipOrigem = ipOrigem;
		this.ipDestino = ipDestino;
		this.setChecksum();
	}

	Pacote(){
	}

	/**
	*Método que define a origem e destino deste pacote
	*
	*@param portaOrigem Porta pela qual a máquina origem está a comunicar
	*@param portaDestino Porta pela qual a máquina destino está a comunicar
	*@param ipOrigem IP da máquina origem
	*@param ipDestino IP da máquina destino
	*/
	public void setIntervenientes(int portaOrigem, int portaDestino, InetAddress ipOrigem, InetAddress ipDestino){
		this.portaOrigem = portaOrigem;
		this.portaDestino = portaDestino;
		this.ipOrigem = ipOrigem;
		this.ipDestino = ipDestino;
	}

	public String getIntervenientes(){
		return ""+portaOrigem+portaDestino+ipOrigem.toString()+ipDestino.toString();
	}

	public InetAddress getIpDestino(){
		return ipDestino;
	}

	public int getPortaDestino(){
		return portaDestino;
	}

	public InetAddress getIpOrigem(){
		return ipOrigem;
	}

	public int getPortaOrigem(){
		return portaOrigem;
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

	public long getChecksum(){
		return this.checksum;
	}

	public void setFin(boolean f){
		this.fin = f;
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
		str += (this.req) ? "REQ ": "";
		//String data = new String(dados);
		str += this.offset;
		return str;
	}

	/**
	*Método que devolve o cabeçalho num array de bytes
	*
	*@return Array de bytes onde é inserida a informação do cabeçalho
	*/
	byte[] header2byte(){

		byte[] pac = new byte[9];

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

		return pac;

	}


	/**
	*Método que devolve o pacote num array de bytes para que possa ser enviado
	*
	*@return array de bytes onde é inserida a informação do pacote
	*/
	byte[] pacote2bytes(){
		byte[] pac = new byte[dados.length + Pacote.header_size];

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

		byte [] check = new byte [8];
		check = ByteBuffer.allocate(Long.BYTES).putLong(this.checksum).array();
		System.arraycopy(check, 0, pac, 9, 8);

		System.arraycopy(dados, 0, pac, Pacote.header_size, dados.length);

		return pac;
	}

	/**
	*Método que transforma este pacote baseado no array de bytes fornecido
	*
	*@param copia Array de bytes do qual se irá extrair a informação
	*/
	void bytes2pacote(byte[] copia){
		dados = new byte[copia.length - Pacote.header_size];

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


		byte [] check = new byte [8];
		System.arraycopy(copia, 9, check, 0, 8);
		ByteBuffer bb = ByteBuffer.allocate(Long.BYTES);
		bb.put(check);
		bb.flip();
		this.checksum = bb.getLong();

		System.arraycopy(copia, Pacote.header_size, dados, 0, dados.length);
	}


	/**
	*Método que verifica a integridade de um pacote
	*
	*@return True se o pacote está correto, false caso contrário
	*/
	public boolean check(){
		byte [] c_dados = new byte[this.dados.length];
		byte complement;
		long soma = 0;
		for(int i = 0; i < this.dados.length; i++){
			complement = (byte) ((Math.floor(Math.log(this.dados[i]) / Math.log(2))) + 1);
			c_dados[i] = (byte) (((1 << complement) - 1) ^ this.dados[i]);
			soma += (long) c_dados[i];
		}

		byte [] c_header = new byte[9];
		byte [] header = header2byte();
		for(int i = 0; i < header.length; i++){
			complement = (byte) ((Math.floor(Math.log(header[i]) / Math.log(2))) + 1);
			c_header[i] = (byte) (((1 << complement) - 1) ^ header[i]);
			soma += (long) c_header[i];
		}

		complement = (byte) ((Math.floor(Math.log(soma) / Math.log(2))) + 1);
		return (soma == this.checksum);
	}

	/**
	*Método que define o checksum do pacote
	*
	*/
	public void setChecksum(){
		byte [] c_dados = new byte[this.dados.length];
		byte complement;
		this.checksum = 0;
		for(int i = 0; i < this.dados.length; i++){
			complement = (byte) ((Math.floor(Math.log(dados[i]) / Math.log(2))) + 1);
			c_dados[i] = (byte) (((1 << complement) - 1) ^ this.dados[i]);
			this.checksum += c_dados[i];
		}

		byte [] c_header = new byte[9];
		byte [] header = header2byte();
		for(int i = 0; i < header.length; i++){
			complement = (byte) ((Math.floor(Math.log(header[i]) / Math.log(2))) + 1);
			c_header[i] = (byte) (((1 << complement) - 1) ^ header[i]);
			this.checksum += c_header[i];
		}
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
