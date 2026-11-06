# User Stories e Critérios de Aceite

| Campo | Valor |
|---|---|
| Versão | 1.0 |
| Issue de origem | [#4](https://github.com/MarkosFerreiraTech/android-spaced-repetition/issues/4) |
| Rastreia | [Casos de Uso](./casos-de-uso.md) → este documento → [Issues](https://github.com/MarkosFerreiraTech/android-spaced-repetition/issues) |
| Última revisão | 2026-11-06 |

**Convenções**

- Formato canônico: *Como [ator], quero [ação] para que [valor de negócio]*.
- Critérios em **BDD/Gherkin**: pelo menos um cenário de sucesso e um de caminho triste por história.
- Estimativa por **story points** (Fibonacci) e verificação de **INVEST** ao final de cada história.

---

## Épico 1 — Importação de conteúdo

### US01 — Importar questões a partir de um CSV

**Story** — Como **estudante**, quero **importar um arquivo CSV com as minhas questões** para que **eu não precise digitar centenas de questões manualmente**.

**Valor de negócio:** remove a maior barreira de adoção — o custo de entrada de conteúdo.
**Prioridade:** Must · **Pontos:** 5 · **Requisitos:** RF01–RF05, RF07 · **Issue:** #11, #12

```gherkin
Cenário: Importação bem-sucedida
  Dado que estou na tela de importação
  E tenho um arquivo "simulado_aws.csv" com 65 questões válidas
  Quando seleciono o arquivo
  Então vejo o progresso da leitura
  E ao final vejo "65 importadas, 0 atualizadas, 0 ignoradas"
  E sou levado à lista de baralhos com o novo baralho no topo

Cenário: Arquivo com cabeçalho inválido
  Dado um arquivo cujo cabeçalho é "pergunta;resposta"
  Quando seleciono o arquivo
  Então vejo a mensagem "Cabeçalho inválido"
  E vejo as colunas esperadas e as colunas encontradas
  E nenhum baralho é criado

Cenário: Arquivo com linhas inválidas
  Dado um arquivo com 9 linhas válidas e 1 linha com gabarito "F"
  Quando a importação termina
  Então vejo "9 importadas" e "1 erro"
  E posso expandir os detalhes e ver "linha 10: gabarito inválido"
```

**Invest:** Independente ✓ · Negociável ✓ · Valiosa ✓ · Estimável ✓ · Small ✓ · Testável ✓

---

### US02 — Reimportar sem duplicar questões

**Story** — Como **estudante**, quero **reimportar a planilha depois de corrigi-la** para que **minhas questões sejam atualizadas sem perder o meu histórico de revisões**.

**Valor:** permite iterar na planilha sem destruir meses de progresso.
**Prioridade:** Must · **Pontos:** 3 · **Requisitos:** RF06 · **Issue:** #10, #11

```gherkin
Cenário: Reimportação idempotente
  Dado um baralho já importado com 65 questões e 40 revisões registradas
  Quando reimporto o mesmo arquivo
  Então o total de questões continua 65
  E as 40 revisões continuam registradas
  E o relatório indica "0 importadas, 65 atualizadas"

Cenário: Correção de gabarito
  Dado que a questão "X" estava com gabarito B e foi revisada 3 vezes
  Quando reimporto o CSV com a questão X corrigida para gabarito C
  Então a questão X passa a ter gabarito C
  E o histórico das 3 revisões é preservado
```

---

## Épico 2 — Organização

### US03 — Ver meus baralhos e o que está pendente hoje

**Story** — Como **estudante**, quero **ver meus baralhos com a contagem de revisões pendentes** para que **eu decida rapidamente o que estudar no tempo que tenho**.

**Prioridade:** Must · **Pontos:** 3 · **Requisitos:** RF09–RF11 · **Issue:** #12

```gherkin
Cenário: Lista com pendências
  Dado que tenho 2 baralhos importados
  E 12 questões do baralho "AWS Practitioner" estão vencidas hoje
  Quando abro a tela inicial
  Então vejo "AWS Practitioner — 65 questões — 12 para revisar"
  E vejo o outro baralho com o seu próprio contador

Cenário: Primeiro uso
  Dado que nunca importei nada
  Quando abro o app
  Então vejo uma mensagem explicando que posso importar uma planilha CSV
  E vejo o botão "Importar CSV" habilitado
```

---

## Épico 3 — Estudo

### US04 — Refazer um simulado em três modos

**Story** — Como **estudante**, quero **refazer o simulado completo, só com as questões que errei, ou com erros mais as que marquei como duvidosas** para que **eu foque no que realmente preciso reforçar**.

**Valor:** é o coração do produto; espelha a estratégia real de quem faz simulados.
**Prioridade:** Must · **Pontos:** 8 · **Requisitos:** RF12–RF14 · **Issue:** #13

```gherkin
Cenário: Modo completo
  Dado um simulado de 65 questões
  Quando escolho o modo "Completo"
  Então a sessão tem 65 questões, incluindo as que já acertei

Cenário: Modo somente erros
  Dado um simulado em que errei 20 e acertei 45
  Quando escolho o modo "Só os erros"
  Então a sessão tem exatamente 20 questões
  E nenhuma questão acertada aparece

Cenário: Modo erros mais marcadas
  Dado que errei 20 questões
  E marquei 7 acertos como "acertei com dúvida"
  Quando escolho o modo "Erros + marcadas"
  Então a sessão tem 27 questões

Cenário: Modo somente erros sem erros
  Dado um simulado em que acertei todas as questões na última tentativa
  Quando escolho o modo "Só os erros"
  Então vejo "Nenhum erro para revisar neste baralho"
  E sou convidado a usar o modo "Completo"
```

---

### US05 — Responder e receber feedback imediato

**Story** — Como **estudante**, quero **responder cada questão e saber imediatamente se acertei** para que **eu associe a correção ao raciocínio que acabei de fazer**.

**Prioridade:** Must · **Pontos:** 5 · **Requisitos:** RF13, RF15 · **Issue:** #13

```gherkin
Cenário: Resposta correta
  Dado que estou na questão 3 de 20
  E seleciono a alternativa correta e confirmo
  Então vejo o feedback "Correto"
  E vejo o progresso avançar para 4 de 20

Cenário: Resposta incorreta
  Dado que seleciono a alternativa errada e confirmo
  Então vejo o feedback "Incorreto" com a alternativa correta destacada
  E vejo a justificativa, quando o CSV a fornecer

Cenário: Confirmação bloqueada
  Dado que não selecionei nenhuma alternativa
  Então o botão "Confirmar" permanece desabilitado
```

---

### US06 — Retomar uma sessão interrompida

**Story** — Como **estudante**, quero **retomar uma sessão de estudo no ponto em que parei** para que **interrupções no ônibus não me façam perder o progresso**.

**Prioridade:** Should · **Pontos:** 3 · **Requisitos:** RF18, RNF10 · **Issue:** #13

```gherkin
Cenário: Retomada após fechar o app
  Dado que respondi 12 de 27 questões e fechei o app
  Quando abro o app novamente e acesso o mesmo baralho
  Então vejo a opção "Retomar sessão (12/27)"
  E ao retomar, a próxima questão é a de número 13

Cenário: Rotação de tela
  Dado que estou na questão 5
  Quando giro o dispositivo
  Então continuo na questão 5, com a alternativa selecionada preservada
```

---

## Épico 4 — Progresso

### US07 — Acompanhar minha evolução

**Story** — Como **estudante**, quero **ver minha acurácia e minha sequência de dias estudados** para que **eu tenha evidência objetiva de que estou melhorando**.

**Prioridade:** Must · **Pontos:** 5 · **Requisitos:** RF19–RF21 · **Issue:** #14

```gherkin
Cenário: Acurácia agregada
  Dado 40 acertos em 65 respostas registradas
  Quando abro as estatísticas
  Então vejo "Acurácia: 61,5%"
  E o número confere com o cálculo feito no banco

Cenário: Sem dados
  Dado que nunca respondi uma questão
  Quando abro as estatísticas
  Então vejo o estado vazio com o botão "Começar a estudar"
  E não vejo gráfico quebrado nem erro de divisão por zero

Cenário: Sequência de dias
  Dado que estudei ontem e hoje
  Quando abro as estatísticas
  Então vejo "Sequência: 2 dias"
```

---

### US08 — Controlar meus estudos com privacidade

**Story** — Como **estudante preocupado com privacidade**, quero **que minhas preferências e métricas fiquem cifradas no meu aparelho** para que **nada do meu desempenho fique legível ou vaze para fora do dispositivo**.

**Prioridade:** Must · **Pontos:** 3 · **Requisitos:** RF22–RF24, RNF12 · **Issue:** #15

```gherkin
Cenário: Dado sensível cifrado
  Dado que altero o limite diário para 30 questões
  Quando inspeciono os arquivos de preferências do app
  Então o valor não está legível em texto puro
  E nenhum enunciado ou resposta é enviado para fora do dispositivo

Cenário: Falha de keystore
  Dado um dispositivo cujo keystore não pode ser acessado
  Quando tento salvar uma preferência
  Então vejo um aviso de que a proteção não está disponível
  E o valor não é gravado em texto puro silenciosamente
```

---

### US09 — Saber quando uma questão volta

**Story** — Como **estudante**, quero **saber quando cada questão vai reaparecer** para que **eu confie no algoritmo e entenda o meu plano de revisão**.

**Prioridade:** Could · **Pontos:** 2 · **Requisitos:** RF25 · **Issue:** #14

```gherkin
Cenário: Próxima revisão visível
  Dado uma questão que acabei de acertar com facilidade
  Quando abro os detalhes da questão
  Então vejo a data da próxima revisão em formato dd/MM/yyyy
  E vejo o estágio atual ("Aprendizado", "Revisão" ou "Reaprendizado")
```

---

## Épico 5 — Processo e qualidade

### US10 — Confiar no código entregue

**Story** — Como **avaliador do projeto**, quero **que cada push rode testes e portões de qualidade** para que **eu possa confiar que o MVP funciona sem ler todo o código**.

**Prioridade:** Must · **Pontos:** 5 · **Requisitos:** RNF17–RNF19 · **Issue:** #9, #16

```gherkin
Cenário: Gate bloqueante
  Dado um pull request com um teste falhando
  Quando o CI executa
  Então o check fica vermelho
  E o merge é bloqueado

Cenário: Cobertura insuficiente
  Dado um pull request que reduz a cobertura do domínio para 88%
  Quando o CI executa
  Então o job de cobertura falha indicando o limite de 95%

Cenário: Arquitetura protegida
  Dado um pull request que importa android.content.Context na camada de domínio
  Quando o CI executa os testes
  Então o teste de ArchUnit falha identificando a classe e o pacote
```

---

### US11 — Acompanhar a evolução do projeto

**Story** — Como **desenvolvedor do projeto**, quero **um histórico de commits e versões legível** para que **eu consiga entender o que mudou, quando e por quê**.

**Prioridade:** Must · **Pontos:** 3 · **Requisitos:** RNF19 · **Issue:** #17

```gherkin
Cenário: Histórico convencional
  Dado o histórico do repositório
  Quando leio as mensagens de commit
  Então cada uma segue "tipo(escopo): descrição imperativa (#issue)"
  E existem tags anotadas v0.1.0, v0.2.0, v0.3.0 e v1.0.0

Cenário: Commit fora do padrão
  Dado um commit com a mensagem "ajustes"
  Quando o hook de validação executa
  Então o commit é rejeitado com o padrão esperado
```

---

### US12 — Entender e executar o projeto

**Story** — Como **novo contribuidor**, quero **um README com instruções de setup e links para a documentação** para que **eu consiga rodar o projeto sem depender de outra pessoa**.

**Prioridade:** Must · **Pontos:** 2 · **Requisitos:** — · **Issue:** #18

```gherkin
Cenário: Setup a partir do zero
  Dado um clone limpo do repositório
  E JDK 17 instalado
  Quando sigo os passos do README
  Então executo o build e os testes com sucesso
  E o resultado dos testes aparece no meu terminal

Cenário: Documentação navegável
  Dado o README
  Quando clico em cada link da seção de documentação
  Então todos abrem um documento existente
```

---

## Backlog Resumido

| ID | História | Épico | Prio. | Pontos | Issue |
|---|---|---|---|---|---|
| US01 | Importar CSV | Importação | Must | 5 | #11, #12 |
| US02 | Reimportar sem duplicar | Importação | Must | 3 | #10, #11 |
| US03 | Ver baralhos e pendências | Organização | Must | 3 | #12 |
| US04 | Três modos de simulado | Estudo | Must | 8 | #13 |
| US05 | Feedback imediato | Estudo | Must | 5 | #13 |
| US06 | Retomar sessão | Estudo | Should | 3 | #13 |
| US07 | Acompanhar evolução | Progresso | Must | 5 | #14 |
| US08 | Privacidade do desempenho | Progresso | Must | 3 | #15 |
| US09 | Ver próxima revisão | Progresso | Could | 2 | #14 |
| US10 | Confiar no código | Processo | Must | 5 | #9, #16 |
| US11 | Histórico legível | Processo | Must | 3 | #17 |
| US12 | Entender e executar | Processo | Must | 2 | #18 |
| | **Total** | | | **47** | |
