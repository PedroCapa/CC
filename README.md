# CC

Controlo de erros
FIN ACK ACK?

Melhorar a parte de caso ocorrer um problema no envio enviar de novo o pacote

Para upload cliente:

O cliente envia N pacotes de cada vez e espera um X tempo:
--se o último ACK recebido for o esperado, coloca no estado o offset do último ACK recebido e passa para o próximo grupo.
--se não for o esperado, reenvia o pacote do último offset recebido.

Servidor recebe corretamente:
O servidor recebe um pacote coloca-o numa lista do estado para a outra thread enviar um ACK, se o offset estiver correto.
O offset do ACK será o offset do último pacote recebido + tamanho, se a lista de pacotes auxiliares estiver vazia.
Se a lista de pacotes não estiver vazia, se houver um pacote com o offset == Integer do estado incrementa o Integer e coloca o pacote na lista de pacotes corretos
Se não estiver na lista, então envia um ACK com esse offset.
Quando o servidor for enviar um ACK espera um x tempo para ver se recebe mais algum pacote e o ACK é alterado.

Servidor não receber corretamente:
O servidor observa que o offset é alto demais então envia ACK com o offset da variável de estado que corresponde ao último offset do ACK enviado, adiciona à lista,...
Em vez de adicionar o pacote à lista de pacotes adiciona a uma lista auxiliar.