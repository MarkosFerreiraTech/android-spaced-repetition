package com.markosferreira.spacedrepetition.presentation.importing;

import java.io.IOException;
import java.io.Reader;

/**
 * Fornece o leitor do arquivo escolhido pelo usuario.
 *
 * <p>Existe para que o ViewModel nunca toque em {@code ContentResolver} nem em
 * {@code Uri}: a Activity resolve o acesso ao arquivo e entrega um leitor. Isso
 * mantem a logica de importacao testavel com um {@code StringReader}.
 */
public interface ReaderSource {

    /**
     * Abre o conteudo do arquivo.
     *
     * @return leitor pronto para consumo; o chamador e responsavel por fechar
     * @throws IOException se o arquivo nao puder ser aberto
     */
    Reader open() throws IOException;
}
