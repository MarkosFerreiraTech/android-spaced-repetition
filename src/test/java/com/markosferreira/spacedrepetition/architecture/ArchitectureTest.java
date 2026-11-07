package com.markosferreira.spacedrepetition.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import org.junit.jupiter.api.DisplayName;

/**
 * Regras de arquitetura verificadas automaticamente (US10, ADR-006).
 *
 * <p>Cada regra aqui e um contrato que o ADR fixa e que o compilador nao
 * consegue garantir sozinho. Se alguem importar {@code android.content.Context}
 * dentro do dominio, o build quebra apontando a classe e o pacote da violacao.
 *
 * <p><b>Precaucao com os padroes de pacote.</b> Os padroes usados aqui sao
 * totalmente qualificados ({@code com.markosferreira...data..}) e nunca
 * genericos como {@code ..data..}. O motivo e concreto: o padrao {@code ..data..}
 * tambem casa com {@code org.assertj.core.data}, e a regra passaria a acusar
 * codigo de biblioteca em vez do codigo do projeto.
 */
@DisplayName("Arquitetura em camadas")
@AnalyzeClasses(
        packages = "com.markosferreira.spacedrepetition",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class ArchitectureTest {

    /** Raiz do projeto. */
    private static final String RAIZ = "com.markosferreira.spacedrepetition";

    /**
     * O dominio e o nucleo do produto: nao pode conhecer o Android.
     *
     * <p>E o que permite testar o FSRS e os casos de uso em JVM pura, sem
     * emulador, e o que torna o algoritmo portavel.
     */
    @ArchTest
    static final ArchRule DOMINIO_NAO_DEPENDE_DE_ANDROID =
            noClasses()
                    .that().resideInAPackage(RAIZ + ".domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("android..", "androidx..", "com.google.android..")
                    .because("o dominio deve rodar em JVM pura e ignorar o framework");

    /**
     * O dominio nao pode depender das camadas externas.
     *
     * <p>Dependencia aponta sempre para dentro: dados e interface conhecem o
     * dominio, nunca o contrario (Dependency Inversion).
     */
    @ArchTest
    static final ArchRule DOMINIO_NAO_DEPENDE_DE_DADOS_UI_OU_DI =
            noClasses()
                    .that().resideInAPackage(RAIZ + ".domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            RAIZ + ".data..",
                            RAIZ + ".presentation..",
                            RAIZ + ".di..")
                    .because("as dependencias devem apontar para o dominio, nunca a partir dele");

    /**
     * A camada de dados nao conhece a interface.
     *
     * <p>Um repositorio que importa uma Activity para "avisar a tela" inverte a
     * seta e amarra o acesso a dados ao ciclo de vida de uma tela.
     */
    @ArchTest
    static final ArchRule DADOS_NAO_DEPENDEM_DE_APRESENTACAO =
            noClasses()
                    .that().resideInAPackage(RAIZ + ".data..")
                    .should().dependOnClassesThat()
                    .resideInAPackage(RAIZ + ".presentation..")
                    .because("a camada de dados nao deve conhecer telas");

    /**
     * Toda implementacao de repositorio vive na camada de dados.
     *
     * <p>Nao exige especificamente {@code data.repository} porque as
     * preferencias cifradas moram legitimamente em {@code data.prefs}: o
     * contrato e "camada de dados", nao uma pasta unica.
     */
    @ArchTest
    static final ArchRule ADAPTADORES_DE_REPOSITORIO_FICAM_NA_CAMADA_DE_DADOS =
            classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .and().areNotInterfaces()
                    // Dublês de teste e classes aninhadas de teste tem o mesmo
                    // sufixo, mas nao sao adaptadores de producao.
                    .and().resideOutsideOfPackage(RAIZ + ".fakes..")
                    .and().haveNameNotMatching(".*\\$.*")
                    .should().resideInAPackage(RAIZ + ".data..")
                    .because("implementacoes de repositorio pertencem a camada de dados");

    /**
     * As portas do dominio sao interfaces.
     *
     * <p>O filtro por nome e proposital: as classes aninhadas das portas
     * ({@code UpsertResult}, {@code Accuracy}, {@code DeckAccuracy}) vivem no
     * mesmo pacote e nao sao portas.
     */
    @ArchTest
    static final ArchRule PORTAS_SAO_INTERFACES =
            classes()
                    .that().resideInAPackage(RAIZ + ".domain.port..")
                    .and().haveNameMatching(".*(Repository|Parser|Scheduler)")
                    .should().beInterfaces()
                    .because("portas sao contratos, nao implementacoes");

    /**
     * Entidades do Room ficam restritas a camada de dados.
     *
     * <p>O sufixo {@code Entity} marca o modelo de persistencia: se uma delas
     * aparecer no dominio, o modelo de negocio passa a carregar anotacoes de
     * banco (violacao da separacao entre Entities e DTOs).
     */
    @ArchTest
    static final ArchRule ENTIDADES_DE_PERSISTENCIA_NAO_VAZAM_PARA_O_DOMINIO =
            noClasses()
                    .that().resideInAPackage(RAIZ + ".domain..")
                    .should().dependOnClassesThat()
                    .haveSimpleNameEndingWith("Entity")
                    .because("entidades do Room nao podem vazar para o dominio");

    /**
     * Casos de uso ficam em {@code domain.usecase}.
     *
     * <p>Um caso de uso escondido em outra camada e um fluxo de negocio sem
     * lugar definido — o tipo de coisa que vira logica duplicada na Activity.
     */
    @ArchTest
    static final ArchRule CASOS_DE_USO_FICAM_EM_DOMAIN_USECASE =
            classes()
                    .that().haveSimpleNameEndingWith("UseCase")
                    .should().resideInAPackage(RAIZ + ".domain.usecase..")
                    .because("casos de uso pertencem ao nucleo de negocio");

    /**
     * O algoritmo de repeticao espacada fica isolado em {@code domain.fsrs}.
     *
     * <p>Isolar o FSRS e o que permite troca-lo por SM-2 sem tocar em caso de
     * uso algum (ADR-001).
     */
    @ArchTest
    static final ArchRule FSRS_ISOLADO_NO_DOMINIO =
            classes()
                    .that().haveSimpleNameStartingWith("Fsrs")
                    .should().resideInAPackage(RAIZ + ".domain.fsrs..")
                    .because("o algoritmo deve ser substituivel sem afetar o resto");

    /**
     * A injecao de dependencia e o unico ponto que conhece as duas pontas.
     *
     * <p>O container monta o grafo ligando portas a adaptadores; nenhuma outra
     * classe deveria precisar dessa visao simultanea.
     */
    @ArchTest
    static final ArchRule APRESENTACAO_NAO_CONHECE_IMPLEMENTACOES_DE_DADOS =
            noClasses()
                    .that().resideInAPackage(RAIZ + ".presentation..")
                    .and().haveSimpleNameNotEndingWith("Application")
                    .should().dependOnClassesThat()
                    .resideInAPackage(RAIZ + ".data.local..")
                    .because("a tela deve depender de casos de uso, nao de DAOs ou entidades");

    /**
     * Nenhum ViewModel instancia repositorio ou DAO diretamente (#20).
     *
     * <p>E a definicao de pronto da issue de MVVM: dependencia entra pelo
     * construtor, nunca por {@code new}. Sem esta regra, a primeira pressa
     * transforma o ViewModel em composition root e os testes passam a precisar
     * de banco real.
     */
    @ArchTest
    static final ArchRule VIEWMODEL_NAO_INSTANCIA_REPOSITORIO =
            classes()
                    .that().haveSimpleNameEndingWith("ViewModel")
                    .should(new ArchCondition<JavaClass>("nao instanciar repositorio ou DAO") {
                        @Override
                        public void check(final JavaClass item, final ConditionEvents events) {
                            for (final JavaConstructorCall call : item.getConstructorCallsFromSelf()) {
                                final String alvo = call.getTarget().getOwner().getName();
                                if (alvo.endsWith("Repository") || alvo.endsWith("Dao")) {
                                    events.add(SimpleConditionEvent.violated(item,
                                            item.getName() + " instancia " + alvo
                                                    + " em vez de receber por construtor"));
                                }
                            }
                        }
                    })
                    .because("dependencias de ViewModel entram pelo construtor, nao por new");

    /**
     * Guarda de sanidade: a analise precisa enxergar o projeto.
     *
     * <p>Sem isto, um filtro de importacao errado faria todas as regras
     * passarem por vacuidade — o pior tipo de falso verde.
     */
    @ArchTest
    static final ArchRule ANALISE_ENXERGA_O_DOMINIO =
            classes()
                    .that().resideInAPackage(RAIZ + ".domain.model..")
                    .should().bePublic()
                    .because("os modelos do dominio sao publicos; se a varredura vier vazia, a analise nao enxerga o projeto");
}
