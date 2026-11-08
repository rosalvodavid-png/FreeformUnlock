# Freeform Unlock

Módulo **LSPosed** que remove as restrições de janela flutuante (freeform / 小窗)
do HyperOS/MIUI, para que apps como o **Kwai** possam ser abertos em janela
flutuante mesmo não estando na lista de apps suportados.

Testado como conceito em HyperOS; os hooks são tolerantes a ROMs diferentes
(se um método não existir, ele é ignorado).

---

## Por que LSPosed e não KernelSU Next?

O bloqueio de janela flutuante **não é um arquivo nem uma prop**: é uma decisão
tomada em tempo de execução dentro do `system_server`, em classes como
`android.util.MiuiMultiWindowUtils`, `android.util.MiuiMultiWindowAdapter` e
`com.android.server.wm.Task` (mais o flag `mDevEnableNonResizableMultiWindow`
do `ActivityTaskManagerService`).

- **Módulo KernelSU Next**: só troca arquivos/props. Para mudar essa decisão
  seria preciso montar um `framework.jar`/`services.jar` remendado — frágil,
  quebra a assinatura do sistema, atrapalha atualização da ROM e é receita de
  bootloop.
- **Módulo LSPosed**: intercepta os métodos Java no processo do sistema, sem
  tocar em nenhum arquivo. É o instrumento certo para esse problema e é o que
  os módulos existentes (ex.: MaxFreeForm) fazem.

Conclusão: para este objetivo, **LSPosed é o caminho eficiente**. Um módulo
KernelSU Next só faria sentido aqui como "instalador" (baixar/instalar o APK e
o LSPosed automaticamente), não como solução.

> Atalho: se você só quer resolver agora, o módulo
> [MaxFreeForm](https://github.com/YifePlayte/MaxFreeForm) já faz isso
> (opção "Forçar janela flutuante", escopo **Sistema Android**). Este projeto
> existe para você ter o seu, mínimo e auditável.

---

## Requisitos

- HyperOS/MIUI (Android 12+)
- Root com **KernelSU Next** (ou Magisk)
- **Zygisk Next** habilitado (ou Zygisk do Magisk)
- **LSPosed** instalado e funcionando

## Build

Não é possível compilar APK neste ambiente (sem JDK/Android SDK). Duas opções:

### Opção A — GitHub Actions (recomendado)

1. Crie um repositório no GitHub e envie este projeto:
   ```bash
   git add .
   git commit -m "Freeform Unlock: módulo LSPosed"
   git remote add origin https://github.com/SEU_USUARIO/FreeformUnlock.git
   git push -u origin main
   ```
2. Abra **Actions → build → Run workflow** (ou apenas faça o push).
3. Baixe o artefato `FreeformUnlock-debug` e instale o `.apk`.

### Opção B — máquina local

Com JDK 17 e Android SDK (platform 35 + build-tools 35):

```bash
gradle :app:assembleDebug
# APK em app/build/outputs/apk/debug/app-debug.apk
```

## Instalação

1. Instale o APK (assinatura debug é aceita pelo LSPosed).
2. Abra o **LSPosed → Módulos**, ative **Freeform Unlock**.
3. Em **Escopo**, marque:
   - **Sistema Android** (`android`) — obrigatório;
   - **System UI** (`com.android.systemui`) — opcional, ajuda no botão de janela flutuante.
4. **Reinicie o celular** (o `system_server` precisa subir com os hooks).
5. Abra o Kwai e use "janela flutuante" pelo menu de recentes / barra lateral /
   notificação, conforme o seu HyperOS.

## Verificar se funcionou

```bash
adb shell su -c 'logcat -s FreeformUnlock'
```

Você deve ver `instalando hooks ...` seguido das linhas `hook ...` e
`hooks instalados`. No LSPosed, os mesmos logs aparecem em **Logs**.

Se aparecer `classe ausente`, aquela classe não existe na sua ROM — normal, os
outros hooks continuam valendo.

## Segurança / recuperação

Hookar o `system_server` pode causar bootloop se a sua ROM for muito diferente.
Proteções incluídas:

- **Interruptor de emergência**: se o arquivo
  `/data/local/tmp/freeformunlock.disabled` existir, o módulo não instala
  nenhum hook. Crie com:
  ```bash
  adb shell su -c 'touch /data/local/tmp/freeformunlock.disabled'
  ```
- **Modo seguro do LSPosed**: em caso de bootloop, desative o módulo pelo
  LSPosed (ou pelo gerenciador do KernelSU Next) e reinicie.
- Todos os hooks são isolados em `try/catch`: um ponto que falha não derruba os
  outros.

## Limitações

- O Kwai pode ter verificações próprias (ex.: travar a orientação, recusar
  `multi-window` internamente). Nesse caso o hook do sistema não basta.
- Atualizações do HyperOS podem renomear métodos. O módulo usa busca por padrão
  de nome justamente para sobreviver a isso, mas ajustes podem ser necessários.
- Apps com proteção contra captura/overlay podem ficar com tela preta em janela
  flutuante (limitação do app, não do módulo).

## Estrutura

```
app/src/main/assets/xposed_init          -> aponta para ModuleEntry
app/src/main/java/com/freeform/unlock/
  ModuleEntry.java                       -> entrada do módulo (processo "android")
  FreeformHooks.java                     -> todos os hooks
  MainActivity.java                      -> tela informativa
.github/workflows/build.yml              -> compila o APK no GitHub
```

## Créditos / referências

Os pontos de hook foram levantados a partir do código aberto do
[MaxFreeForm](https://github.com/YifePlayte/MaxFreeForm) (GPL-3.0) e do
comportamento do AOSP (`ActivityTaskManagerService`,
`mDevEnableNonResizableMultiWindow`).
