# pdefi 📖

Leitor de PDF offline para Android com design **neo-skeuomórfico** — sem anúncios, leve e rápido.

## Funcionalidades

- 📄 Abre qualquer PDF do armazenamento ou de outros apps
- 📖 Navegação página a página com swipe horizontal
- 🔍 Pinch-to-zoom, pan e double-tap para reset
- 🖥️ Modo imersivo tela cheia com barras auto-hide
- 📋 Histórico de arquivos recentes (até 20)
- 🚫 Zero anúncios — sem permissão de internet
- ⚡ Renderização nativa (`PdfRenderer`) — sem dependências externas de PDF

## Design

Interface **neo-skeuomórfica** com:
- Gradientes multicamada, sombras internas e externas
- Botões com relevo e estado pressionado com deslocamento visual
- Paleta navy profundo com acento dourado
- Painéis com elevação perceptível e superfícies polidas

## Download APK

> Veja a aba [Releases](../../releases) para baixar o APK mais recente.

## Build

### Pré-requisitos
- JDK 17+
- Android SDK (API 34 + Build-Tools 34.0.0)

### Compilar

```bash
# Windows
.\gradlew.bat assembleDebug

# Linux / macOS
./gradlew assembleDebug
```

APK gerado em: `app/build/outputs/apk/debug/app-debug.apk`

## Compatibilidade

- **Mínimo**: Android 5.0 (API 21)
- **Alvo**: Android 14 (API 34)

## Licença

MIT
