package com.markosferreira.spacedrepetition.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import org.junit.jupiter.api.DisplayName;

/**
 * Regras de arquitetura verificadas automaticamente (US10, ADR-006).
 *
 * <p>Cada regra aqui e um contrato que o ADR fixa e que o compilador nao
 * consegue garantir sozinho. Se alguem importar {@code android.content.Context}
 * dentro do dominio, o build quebra apontando a classe e o pacote da violacao.
 */
@DisplayName("Arquitetura em camadas")
@AnalyzeClasses(
        packages = "com.markosferreira.spacedrepetition",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class ArchitectureTest {

    /**
     * O dominio e o nucleo do produto: nao pode conhecer o Android.
     *
     * <p>E o que permite testar o FSRS e os casos de uso em JVM pura, sem
     * emulador, e o que torna o algoritmo portavel.
     */
    @ArchTest
    static final ArchRule DOMINIO_NAO_DEPENDE_DE_ANDROID =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("android..", "androidx..", "com.google.android..")
                    .because("o dominio deve rodar em JVM pura e ignore o framework");

    /**
     * O dominio nao pode depender das camadas externas.
     *
     * <p>Dependencia aponta sempre para dentro: dados e interface conhecem o
     * dominio, nunca o contrario (Dependency Inversion).
     */
    @ArchTest
    static final ArchRule DOMINIO_NAO_DEPENDE_DE_DADOS_UI_OU_DI =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..data..", "..presentation..", "..di..")
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
                    .that().resideInAPackage("..data..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..presentation..")
                    .because("a camada de dados nao deve conhecer telas");

    /**
     * As portas do dominio sao interfaces.
     *
     * <p>Se uma porta virar classe abstrata com estado, a inversao de
     * dependencia perde o sentido e os dublês de teste ficam presos a heranca.
     */
    @ArchTest
    static final ArchRule PORTAS_SAO_INTERFACES =
            classes()
                    .that().resideInAPackage("..domain.port..")
                    // O filtro por nome completo e proposital: as classes
                    // aninhadas das portas (UpsertResult, Accuracy, LineError,
                    // CsvReadException) vivem no mesmo pacote e NAO sao
                    // portas. O sufixo nao as alcanca porque o nome completo
                    // de uma classe aninhada contem '$'.
                    .and().haveNameMatching(".*(Repository|Parser|Scheduler)")
                    .should().beInterfaces()
                    .because("portas sao contratos, nao implementacoes");

    /**
     * Os adaptadores de repositorio vivem em {@code data.repository}.
     *
     * <p>Mantem a simetria: toda porta {@code *Repository} tem sua
     * implementacao concreta em um unico lugar previsivel.
     */
    @ArchTest
    static final ArchRule ADAPTADORES_DE_REPOSITORIO_FICAM_EM_DATA_REPOSITORY =
            classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .and().areNotInterfaces()
                    .and().resideOutsideOfPackage("..di..")
                    .should().resideInAPackage("..data.repository..")
                    .because("implementacoes de repositorio pertencem a camada de dados");

    /**
     * Entidades do Room ficam restritas a camada de dados.
     *
     * <p>O sufixo {@code Entity} marca o modelo de persistencia: se uma delas
     * aparecer no dominio, o modelo de negocio passa a carregar anotacoes de
     * banco (violacao do principio de Entities separadas dos DTOs).
     */
    @ArchTest
    static final ArchRule ENTIDADES_DE_PERSISTENCIA_NAO_VAZAM_PARA_O_DOMINIO =
            noClasses()
                    .that().resideInAPackage("..domain..")
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
                    .should().resideInAPackage("..domain.usecase..")
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
                    .should().resideInAPackage("..domain.fsrs..")
                    .because("o algoritmo deve ser substituivel sem afetar o resto");
}
