# Segurança e Privacidade

| Campo | Valor |
|---|---|
| Versão | 1.0 |
| Issue de origem | [#16](https://github.com/MarkosFerreiraTech/android-spaced-repetition/issues/16) |
| ADR relacionado | [ADR-005](./adr/ADR-005-classificacao-de-dados-e-criptografia.md) |
| Última revisão | 2026-11-07 |

## 1. Modelo de ameaça

O ativo a proteger não é o conteúdo das questões — é o **comportamento de estudo do
usuário**: o que ele erra, com que frequência estuda, qual o seu desempenho.

| Vetor | Probabilidade | Impacto | Controle |
|---|---|---|---|
| Outro app lendo os dados do sandbox | Baixa (Android isola) | Alto | Sandbox `MODE_PRIVATE` |
| Acesso casual ao arquivo de preferências (dispositivo roteado, adb em debug) | Média | Médio | `EncryptedSharedPreferences` |
| Backup em nuvem expondo o banco | Baixa | Médio | Backup desabilitado + `data_extraction_rules.xml` |
| Exfiltração por rede | Nula | Alto | Sem permissão `INTERNET` no manifesto |
| Segredo versionado no Git | Média | Alto | `gitleaks` no CI + `.gitignore` |
| Dependência vulnerável | Média | Médio | Dependabot + revisão de PR |

**Fora do modelo de ameaça:** atacante com root ativo e persistente no dispositivo.
Nesse cenário, nenhum controle em espaço de usuário é eficaz, e o dado não tem valor
de mercado suficiente para justificar SQLCipher no MVP (risco aceito no ADR-005).

## 2. Classificação de dados

| Dado | Local | Nível | Proteção |
|---|---|---|---|
| Limite diário de questões, fuso horário | `EncryptedSharedPreferences` | **Sensível** | AES-256-GCM, chave no Keystore |
| Métricas agregadas (acurácia, sequência) em cache | `EncryptedSharedPreferences` | **Sensível** | Idem |
| Nome do baralho | Room | Interno | Sandbox |
| Enunciado, alternativas, gabarito | Room | Interno | Sandbox |
| Histórico de revisões (datas, ratings) | Room | Interno | Sandbox |
| Nome, e-mail, localização, contatos | — | — | **Não coletado** |

## 3. Criptografia em repouso

```java
MasterKey masterKey = new MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build();

SharedPreferences prefs = EncryptedSharedPreferences.create(
        context,
        "cerebro_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
```

**Tratamento de falha (RF24):** se o Keystore estiver indisponível, o app **não**
grava o valor em texto puro silenciosamente. Ele informa o usuário e bloqueia a
gravação, registrando o evento sem dados sensíveis.

## 4. Registro de logs

Proibido registrar: enunciado, alternativa escolhida, gabarito, preferências,
identificadores de sessão.

Verificado por duas regras de Checkstyle que falham o build:
- `System.out.print` → proibido.
- `printStackTrace` → proibido.

## 5. Verificações automatizadas no CI

| Verificação | Job | Bloqueia merge |
|---|---|---|
| `gitleaks` sobre o histórico completo | Varredura de seguranca | ✅ |
| Ausência de `INTERNET` / `ACCESS_NETWORK_STATE` no manifesto (ignorando comentários XML) | Varredura de seguranca | ✅ |
| `.gitignore` cobrindo `*.keystore`, `*.jks`, `keystore.properties`, `local.properties` | Inspeção + teste manual | ✅ |

## 6. Pendências

| Item | Issue | Marcos |
|---|---|---|
| Implementar `EncryptedSharedPreferences` e `MasterKey` | #16 | M4 |
| Regra de lint customizada para log de dado sensível | #16 | M4 |
| Dependabot para dependências do Gradle e das Actions | #16 | M4 |
