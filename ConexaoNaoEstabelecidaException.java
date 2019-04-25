/**
 * Classe que reporta a excecao no caso de se tentar registar com um nome de utilizador que ja exista
 *
 */
public class ConexaoNaoEstabelecidaException extends Exception{
	/** Mensagem vazia no caso se tentar fazer registar com um  nome que ja exista*/
	public ConexaoNaoEstabelecidaException(){
		super();
	}
	/** Mensagem no caso se tentar fazer registar com um  nome que ja exista*/
	public ConexaoNaoEstabelecidaException(String s){
		super(s);
	}
}