# CC

Controlo de erros
FIN ACK ACK?

Melhorar a parte de caso ocorrer um problema no envio enviar de novo o pacote

Para upload cliente:

O cliente envia N pacotes de cada vez e espera um X tempo:
--se o último ACK recebido for o esperado, coloca no estado o offset do último ACK recebido e passa para o próximo grupo.
--se não for o esperado, reenvia o pacote do último offset recebido.

Servidor recebe corretamente:
O servidor recebe um pacote coloca numa lista do estado para a outra thread enviar um ACK com o offset do pacote recebido + tamanho.
Quando o servidor for enviar um ACK remove da lista o valor do offset do ACK a enviar e envia pacote.

Servidor não receber corretamente:
O servidor observa que o offset é alto demais então envia ACK com o offset da variável de estado que corresponde ao último offset do ACK enviado, adiciona à lista,...