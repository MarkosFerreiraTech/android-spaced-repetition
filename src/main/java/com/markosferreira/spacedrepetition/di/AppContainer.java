package com.markosferreira.spacedrepetition.di;

import android.content.Context;

import androidx.room.Room;

import com.markosferreira.spacedrepetition.BuildConfig;
import com.markosferreira.spacedrepetition.data.csv.OpenCsvParser;
import com.markosferreira.spacedrepetition.data.local.AppDatabase;
import com.markosferreira.spacedrepetition.data.prefs.EncryptedPreferencesRepository;
import com.markosferreira.spacedrepetition.data.repository.RoomCardRepository;
import com.markosferreira.spacedrepetition.data.repository.RoomDeckRepository;
import com.markosferreira.spacedrepetition.data.repository.RoomReviewRepository;
import com.markosferreira.spacedrepetition.data.repository.RoomSessionRepository;
import com.markosferreira.spacedrepetition.data.repository.RoomStatisticsRepository;
import com.markosferreira.spacedrepetition.domain.fsrs.FsrsParameters;
import com.markosferreira.spacedrepetition.domain.fsrs.FsrsScheduler;
import com.markosferreira.spacedrepetition.domain.port.CardRepository;
import com.markosferreira.spacedrepetition.domain.port.CsvParser;
import com.markosferreira.spacedrepetition.domain.port.DeckRepository;
import com.markosferreira.spacedrepetition.domain.port.PreferencesRepository;
import com.markosferreira.spacedrepetition.domain.port.ReviewRepository;
import com.markosferreira.spacedrepetition.domain.port.ReviewScheduler;
import com.markosferreira.spacedrepetition.domain.port.SessionRepository;
import com.markosferreira.spacedrepetition.domain.port.StatisticsRepository;
import com.markosferreira.spacedrepetition.domain.usecase.AnswerQuestionUseCase;
import com.markosferreira.spacedrepetition.domain.usecase.BuildStudyQueueUseCase;
import com.markosferreira.spacedrepetition.domain.usecase.ImportDeckUseCase;
import com.markosferreira.spacedrepetition.domain.usecase.StartStudySessionUseCase;
import com.markosferreira.spacedrepetition.data.concurrent.AppExecutors;
import com.markosferreira.spacedrepetition.data.concurrent.ImportExecutor;
import com.markosferreira.spacedrepetition.presentation.deck.DeckListViewModel;
import com.markosferreira.spacedrepetition.presentation.importing.ImportViewModel;
import com.markosferreira.spacedrepetition.presentation.stats.StatsViewModel;
import com.markosferreira.spacedrepetition.presentation.study.StudyViewModel;

import java.time.Clock;
import java.util.concurrent.Executor;

/**
 * Container de injecao de dependencia da aplicacao.
 *
 * <p>Injecao manual (composition root), sem framework: o grafo tem um unico
 * escopo — a aplicacao inteira — e e pequeno o bastante para ser lido de uma
 * vez. Cada dependencia e criada sob demanda e memorizada, de modo que o banco
 * e o armazenamento cifrado existam uma unica vez por processo.
 *
 * <p>As camadas de dominio e apresentacao nao conhecem esta classe: recebem as
 * portas por construtor. Trocar uma implementacao aqui nao toca em mais
 * nenhum arquivo (Dependency Inversion).
 *
 * <p><b>Extensao em teste.</b> A classe nao e {@code final} de proposito: os
 * testes de tela (Robolectric) estendem este container para substituir
 * repositorios por dublês e o executor de IO por um sincrono, sem tocar em
 * codigo de producao.
 */
public class AppContainer {

    private final Context context;

    private AppDatabase database;
    private PreferencesRepository preferences;
    private CsvParser csvParser;
    private CardRepository cardRepository;
    private DeckRepository deckRepository;
    private ReviewRepository reviewRepository;
    private SessionRepository sessionRepository;
    private StatisticsRepository statisticsRepository;
    private ReviewScheduler scheduler;

    private AppExecutors appExecutors;
    private ImportExecutor importExecutor;
    private ImportDeckUseCase importDeckUseCase;
    private StartStudySessionUseCase startStudySessionUseCase;
    private AnswerQuestionUseCase answerQuestionUseCase;

    /**
     * Cria o container.
     *
     * @param context contexto da aplicacao (nao de activity: o container vive
     *                tanto quanto o processo)
     */
    public AppContainer(final Context context) {
        // Aceita nulo para permitir containers de teste que sobrescrevem tudo
        // que dependeria de contexto (banco, preferencias).
        this.context = context == null ? null : context.getApplicationContext();
    }

    /**
     * Banco Room, criado uma vez por processo.
     *
     * @return banco aberto
     */
    public synchronized AppDatabase database() {
        if (database == null) {
            database = Room.databaseBuilder(context, AppDatabase.class, AppDatabase.NAME)
                    // Sem fallback destrutivo: perder historico silenciosamente
                    // violaria o RF06. Uma migracao faltante deve falhar alto.
                    .build();
        }
        return database;
    }

    /**
     * Preferencias cifradas.
     *
     * @return porta de preferencias
     */
    public synchronized PreferencesRepository preferences() {
        if (preferences == null) {
            preferences = EncryptedPreferencesRepository.create(context);
        }
        return preferences;
    }

    /**
     * Parser de CSV sobre OpenCSV.
     *
     * @return porta de leitura de planilhas
     */
    public synchronized CsvParser csvParser() {
        if (csvParser == null) {
            csvParser = new OpenCsvParser(BuildConfig.CSV_MAX_LINES);
        }
        return csvParser;
    }

    /**
     * Repositorio de questoes.
     *
     * @return porta de questoes
     */
    public synchronized CardRepository cardRepository() {
        if (cardRepository == null) {
            cardRepository = new RoomCardRepository(database(), database().cardDao());
        }
        return cardRepository;
    }

    /**
     * Repositorio de baralhos.
     *
     * @return porta de baralhos
     */
    public synchronized DeckRepository deckRepository() {
        if (deckRepository == null) {
            deckRepository = new RoomDeckRepository(database().deckDao());
        }
        return deckRepository;
    }

    /**
     * Repositorio de revisoes.
     *
     * @return porta de revisoes
     */
    public synchronized ReviewRepository reviewRepository() {
        if (reviewRepository == null) {
            reviewRepository = new RoomReviewRepository(
                    database(), database().cardDao(), database().reviewLogDao());
        }
        return reviewRepository;
    }

    /**
     * Repositorio de sessoes.
     *
     * @return porta de sessoes
     */
    public synchronized SessionRepository sessionRepository() {
        if (sessionRepository == null) {
            sessionRepository = new RoomSessionRepository(database().studySessionDao());
        }
        return sessionRepository;
    }

    /**
     * Repositorio de estatisticas.
     *
     * @return porta de agregados
     */
    public synchronized StatisticsRepository statisticsRepository() {
        if (statisticsRepository == null) {
            statisticsRepository = new RoomStatisticsRepository(
                    database().reviewLogDao(), preferences().zoneId());
        }
        return statisticsRepository;
    }

    /**
     * Algoritmo de repeticao espacada (FSRS).
     *
     * @return porta do agendador
     */
    public synchronized ReviewScheduler scheduler() {
        if (scheduler == null) {
            scheduler = new FsrsScheduler(FsrsParameters.defaults());
        }
        return scheduler;
    }

    /**
     * Caso de uso de importacao.
     *
     * @return caso de uso
     */
    public synchronized ImportDeckUseCase importDeckUseCase() {
        if (importDeckUseCase == null) {
            importDeckUseCase = new ImportDeckUseCase(
                    csvParser(), deckRepository(), cardRepository());
        }
        return importDeckUseCase;
    }

    /**
     * Caso de uso de inicio de sessao.
     *
     * @return caso de uso
     */
    public synchronized StartStudySessionUseCase startStudySessionUseCase() {
        if (startStudySessionUseCase == null) {
            startStudySessionUseCase = new StartStudySessionUseCase(
                    new BuildStudyQueueUseCase(cardRepository(), reviewRepository()),
                    sessionRepository());
        }
        return startStudySessionUseCase;
    }

    /**
     * Fabrica do ViewModel da lista de baralhos.
     *
     * <p>O {@code Clock} do sistema e injetado aqui — o unico lugar do app que
     * conhece o relogio real. Os testes passam um {@code Clock.fixed}.
     *
     * @return ViewModel pronto para a tela
     */
    public DeckListViewModel deckListViewModel() {
        return new DeckListViewModel(deckRepository(), io(),
                Clock.systemDefaultZone());
    }

    /**
     * Executor usado pelos ViewModels.
     *
     * <p>Ponto de extensao para os testes de tela, que substituem o pool por um
     * executor sincrono. Em producao delega ao pool real.
     *
     * @return executor de background
     */
    public Executor io() {
        return appExecutors().io();
    }

    /**
     * Executores do processo.
     *
     * @return executores
     */
    public synchronized AppExecutors appExecutors() {
        if (appExecutors == null) {
            appExecutors = new AppExecutors();
        }
        return appExecutors;
    }

    /**
     * Executor das tarefas de importacao.
     *
     * <p>Uma unica instancia por processo: duas importacoes simultaneas
     * disputariam escrita no mesmo baralho.
     *
     * @return executor serial
     */
    public synchronized ImportExecutor importExecutor() {
        if (importExecutor == null) {
            importExecutor = new ImportExecutor(BuildConfig.CSV_IMPORT_POOL_SIZE);
        }
        return importExecutor;
    }

    /**
     * Fabrica do ViewModel de importacao.
     *
     * @return ViewModel pronto para a tela
     */
    public ImportViewModel importViewModel() {
        return new ImportViewModel(importDeckUseCase(), importExecutor(),
                Clock.systemDefaultZone());
    }

    /**
     * Fabrica do ViewModel de sessao de estudo.
     *
     * @return ViewModel pronto para a tela
     */
    public StudyViewModel studyViewModel() {
        return new StudyViewModel(startStudySessionUseCase(), answerQuestionUseCase(),
                cardRepository(), sessionRepository(), io(),
                Clock.systemDefaultZone());
    }

    /**
     * Fabrica do ViewModel de estatisticas.
     *
     * @return ViewModel pronto para a tela
     */
    public StatsViewModel statsViewModel() {
        return new StatsViewModel(statisticsRepository(), io());
    }

    /**
     * Caso de uso de resposta a questao.
     *
     * @return caso de uso
     */
    public synchronized AnswerQuestionUseCase answerQuestionUseCase() {
        if (answerQuestionUseCase == null) {
            answerQuestionUseCase = new AnswerQuestionUseCase(scheduler(), reviewRepository());
        }
        return answerQuestionUseCase;
    }
}
