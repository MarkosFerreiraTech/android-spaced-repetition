package com.markosferreira.spacedrepetition.domain.usecase;

import com.markosferreira.spacedrepetition.domain.model.Card;
import com.markosferreira.spacedrepetition.domain.model.CsvQuestion;
import com.markosferreira.spacedrepetition.domain.model.Deck;
import com.markosferreira.spacedrepetition.domain.model.ParseOutcome;
import com.markosferreira.spacedrepetition.domain.port.CardRepository;
import com.markosferreira.spacedrepetition.domain.port.CsvParser;
import com.markosferreira.spacedrepetition.domain.port.DeckRepository;
import com.markosferreira.spacedrepetition.domain.service.ContentHasher;

import java.io.Reader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Importa uma planilha e cria ou atualiza o baralho correspondente (UC01).
 *
 * <p>Responsabilidades:
 * <ol>
 *   <li>delegar o parsing a porta {@link CsvParser};</li>
 *   <li>converter {@link CsvQuestion} (contrato do CSV) em {@link Card} (entidade);</li>
 *   <li>calcular o {@code contentHash} para garantir upsert idempotente (RF06);</li>
 *   <li>montar o relatorio de importacao (RF07).</li>
 * </ol>
 *
 * <p>Este caso de uso nao conhece thread, Android, arquivo ou banco: recebe um
 * {@link Reader} e portas. Por isso e testavel com um parser real e repositorios
 * em memoria.
 */
public final class ImportDeckUseCase {

    /** Tamanho do lote gravado por transacao (RNF02, RNF07). */
    public static final int BATCH_SIZE = 500;

    private final CsvParser csvParser;
    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;

    public ImportDeckUseCase(final CsvParser csvParser,
                             final DeckRepository deckRepository,
                             final CardRepository cardRepository) {
        this.csvParser = csvParser;
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
    }

    /**
     * Executa a importacao.
     *
     * @param reader fonte do CSV
     * @param deckName nome do baralho (normalmente o nome do arquivo)
     * @param now instante da importacao
     * @return relatorio com importados, atualizados, ignorados e erros
     */
    public ImportResult execute(final Reader reader, final String deckName, final Instant now) {
        if (reader == null) {
            throw new IllegalArgumentException("Fonte do CSV obrigatoria");
        }
        final ParseOutcome outcome = csvParser.parse(reader);
        if (!outcome.headerValid()) {
            throw new InvalidHeaderException(outcome.foundColumns());
        }

        // Reimportar a mesma planilha deve cair no MESMO baralho; caso contrario o
        // content hash (que inclui o id do baralho) mudaria e as questoes seriam
        // duplicadas em vez de atualizadas (RF06).
        final String name = sanitizeName(deckName);
        final Deck existingDeck = deckRepository.findByName(name);
        final Deck deck = existingDeck != null
                ? existingDeck
                : new Deck(UUID.randomUUID().toString(), name, now, null);
        deckRepository.save(deck);

        final Set<String> existingHashes = cardRepository.findHashesByDeck(deck.id());
        final List<Card> batch = new ArrayList<>(BATCH_SIZE);
        final List<ImportResult.LineError> errors = new ArrayList<>();
        for (final ParseOutcome.LineError error : outcome.errors()) {
            errors.add(new ImportResult.LineError(error.lineNumber(), error.reason()));
        }

        int inserted = 0;
        int updated = 0;
        for (final CsvQuestion question : outcome.questions()) {
            final String hash = ContentHasher.hash(deck.id(), question.question());
            final boolean exists = existingHashes.contains(hash);
            batch.add(toCard(deck.id(), question, hash, exists));
            if (exists) {
                updated++;
            } else {
                inserted++;
            }
            if (batch.size() >= BATCH_SIZE) {
                cardRepository.upsertAll(new ArrayList<>(batch));
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            cardRepository.upsertAll(batch);
        }

        return new ImportResult(inserted, updated, outcome.errors().size(), errors);
    }

    private Card toCard(final String deckId, final CsvQuestion question,
                        final String hash, final boolean existing) {
        return Card.builder()
                .id(existing ? deckId + ':' + hash : UUID.randomUUID().toString())
                .deckId(deckId)
                .question(question.question())
                .options(question.options())
                .correctIndex(question.correctIndex())
                .explanation(question.explanation())
                .contentHash(hash)
                .build();
    }

    private String sanitizeName(final String deckName) {
        if (deckName == null || deckName.trim().isEmpty()) {
            return "Simulado importado";
        }
        String name = deckName.trim();
        final int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name.length() > 80 ? name.substring(0, 80) : name;
    }

    /**
     * Sinaliza cabecalho fora do contrato do Anexo A (UC01/A1).
     */
    public static final class InvalidHeaderException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final transient List<String> foundColumns;

        public InvalidHeaderException(final List<String> foundColumns) {
            super("Cabecalho do CSV fora do contrato");
            this.foundColumns = new ArrayList<>(foundColumns);
        }

        public List<String> foundColumns() {
            return new ArrayList<>(foundColumns);
        }
    }
}
