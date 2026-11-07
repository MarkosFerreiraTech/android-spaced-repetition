# ADR-005 — Classificação de dados e escopo da criptografia

- **Status:** Aceito com risco documentado
- **Data:** 2026-11-06
- **Decisores:** Markos Ferreira (PO, Tech Lead)
- **Issue:** #15

## Contexto

O requisito de DevSecOps exige que "qualquer dado sensível do usuário" seja armazenado cifrado com `EncryptedSharedPreferences`. Ao mesmo tempo, as questões do simulado são gravadas no Room, que **não** é cifrado sem SQLCipher.

Deixar isso implícito produziria um requisito decorativo: "cifrado" sem dizer o quê.

## Decisão

Adotar uma **classificação explícita de dados** e cifrar apenas o que é classificado como sensível.

| Dado | Local | Classificação | Proteção |
|---|---|---|---|
| Limite diário, fuso horário, preferências de UI | EncryptedSharedPreferences | **Sensível** | AES-256-GCM, chave no Android Keystore |
| Métricas agregadas de desempenho (acurácia, streak) | EncryptedSharedPreferences (cache) | **Sensível** | Idem |
| Nome do baralho | Room | **Interno** | Isolamento de sandbox do Android |
| Enunciados, alternativas, gabarito, explicações | Room | **Interno** | Sandbox (`MODE_PRIVATE`) |
| Logs de revisão (datas, ratings, acertos) | Room | **Interno** | Sandbox |
| Nenhum dado pessoal (nome, e-mail, localização) | — | — | **Não coletado** |

## Justificativa

1. **Proporcionalidade:** cifrar o banco inteiro exigiria SQLCipher, que adiciona dependência nativa, aumenta o tamanho do APK e complica migrações — custo alto para o prazo de uma semana.
2. **Modelo de ameaça:** o vetor realista é acesso casual ao arquivo de preferências em dispositivo com root ou via backup; o conteúdo de questões de certificação tem valor limitado para terceiros, enquanto preferências e métricas revelam comportamento do usuário.
3. **Sandbox do Android** já impede que outros apps leiam `/data/data/<package>` em dispositivos não rooteados. A decisão documenta que isso é uma camada, não uma ausência de proteção.
4. O app **não declara permissão de internet**, portanto não há exfiltração possível por este aplicativo (RNF05, RNF15).

## Consequências

**Positivas**
- Reforça o requisito de criptografia com escopo verificável e testável.
- Evita dependência nativa e mantém build leve.

**Negativas / Risco aceito**
- Um atacante com root e acesso ao banco poderia ler questões e histórico de revisão em texto puro. **Risco aceito** por: (a) baixo valor do dado, (b) ausência de dados pessoais, (c) prazo do MVP.
- **Gatilho de revisão:** se uma versão futura incluir sincronização em nuvem ou dados pessoais, SQLCipher passa a ser obrigatório, e este ADR deve ser substituído.

## Alternativas consideradas

- **SQLCipher em todo o banco:** rejeitado por custo/benefício no prazo; registrado como evolução v1.1.
- **Cifrar campos individualmente no Room com AES manual:** rejeitado porque impede queries agregadas (estatísticas) sobre campos cifrados.
- **Não cifrar nada:** rejeitado por violar o requisito de DevSecOps.
