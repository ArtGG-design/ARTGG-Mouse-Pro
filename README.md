# ARTGG Mouse Pro

Aplicativo Android nativo para mapeamento de teclado e mouse em jogos mobile.  
Converte comandos físicos (USB OTG / Bluetooth) em toques, deslizamentos e controle de câmera na tela.

> **Status:** Fase 1 (MVP) — estrutura compilável, serviços de ativação e captura, editor visual básico e persistência de perfis.

---

## Requisitos do Ambiente

| Ferramenta | Versão Mínima |
|---|---|
| Android Studio | Ladybug (2024.2) ou superior |
| JDK | 17+ (Eclipse Temurin recomendado) |
| Android SDK | API 35 (compileSdk), minSdk 29 |
| Gradle | 8.9 (via wrapper incluso) |
| Git | 2.x |

### Configuração Rápida (Windows)

```powershell
# Execute como Administrador:
Set-ExecutionPolicy Bypass -Scope Process -Force
.\setup-android-dev.ps1

# Depois no Git Bash:
source setup-android-dev-bash.sh
```

---

## Como Compilar e Executar

1. Abra o projeto no Android Studio (`File > Open > artgg-mouse-pro`)
2. Aguarde a sincronização do Gradle
3. Conecte um dispositivo Android (API 29+) ou inicie um emulador
4. Clique em **Run** (▶️) ou execute:

```bash
./gradlew assembleDebug
```

O APK debug será gerado em `app/build/outputs/apk/debug/`.

---

## Arquitetura

```
com.artgg.mousepro/
├── ArtggApp.kt                    # Application + canais de notificação
├── activation/
│   └── ActivationActivity.kt      # Guia de ativação (Acessibilidade/ADB/Shizuku)
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt         # Room database
│   │   ├── Converters.kt          # TypeConverters para listas serializadas
│   │   └── GameProfileDao.kt      # DAO de perfis
│   ├── model/
│   │   ├── GameProfile.kt         # Entidade de perfil por jogo
│   │   └── MappingComponent.kt    # Modelo de componente mapeável
│   └── repository/
│       └── ProfileRepository.kt   # Repositório de perfis
├── macro/                         # (Fase 3) Motor de macros
├── mapping/
│   ├── components/                # (Fase 2) Implementações por tipo
│   ├── engine/                    # (Fase 2) Motor de conversão entrada→toque
│   └── input/                     # (Fase 2) Captura avançada de HID/Bluetooth
├── overlay/                       # (Fase 2) Overlay flutuante in-game
├── service/
│   ├── ArtggAccessibilityService.kt  # Injeção de gestos via Accessibility API
│   └── InputCaptureService.kt        # Foreground service + detecção de dispositivos
├── ui/
│   ├── MainActivity.kt            # Tela principal com lista de perfis
│   ├── MappingEditorActivity.kt   # Editor visual fullscreen landscape
│   ├── navigation/                # (Fase 2) Navigation Compose
│   ├── screens/                   # (Fase 2) Telas adicionais
│   └── theme/
│       ├── Color.kt               # Paleta ARTGG
│       └── Theme.kt               # Material3 dark theme personalizado
└── util/                          # Utilitários gerais
```

---

## Permissões e Justificativa Técnica

| Permissão | Uso |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Overlay de edição e menu flutuante sobre jogos |
| `FOREGROUND_SERVICE` | Serviço persistente de captura de entrada |
| `BIND_ACCESSIBILITY_SERVICE` | Injeção de gestos sem root (única API pública disponível) |
| `BLUETOOTH_*` | Detecção de mouses/teclados Bluetooth |
| `WAKE_LOCK` | Evitar sleep durante sessões de jogo |
| `VIBRATE` | Feedback tátil em ações mapeadas |
| `QUERY_ALL_PACKAGES` | Detectar jogos instalados para vincular perfis |
| `POST_NOTIFICATIONS` | Notificação do foreground service (API 33+) |

> ⚠️ O serviço de acessibilidade é usado **exclusivamente** para simular toques. Nenhum dado pessoal é coletado ou transmitido.

---

## Limitações Técnicas Conhecidas (Android sem Root)

1. **Captura global de mouse motion**: Android não permite que serviços capturem movimento relativo do mouse em segundo plano. O modo FPS View funciona apenas quando o overlay tem foco ou via ADB/Shizuku.
2. **Injeção de gestos**: Limitada à AccessibilityService (`dispatchGesture`). Alguns jogos bloqueiam eventos sintéticos.
3. **Multi-touch simultâneo**: `dispatchGesture` suporta múltiplos strokes, mas há limite prático de ~5 pontos simultâneos dependendo do OEM.
4. **Latência**: A cadeia KeyEvent → AccessibilityService → dispatchGesture adiciona ~10-30ms. Suficiente para maioria dos gêneros, mas não competitivo profissional.
5. **Anticheat**: Alguns jogos detectam e bloqueiam serviços de acessibilidade. Não há contorno legítimo.

---

## Roadmap

| Fase | Conteúdo | Status |
|---|---|---|
| **1 — MVP** | Estrutura, ativação, captura básica, editor, perfis | ✅ Implementado |
| **2 — Controle Avançado** | Joystick contínuo, FPS View, sensibilidade XY, modo digitação | 🔲 Próximo |
| **3 — Recursos Extras** | Macros, import/export JSON, cursor custom, diagnósticos | 🔲 Planejado |
| **4 — Estabilidade** | Testes multi-dispositivo, otimização, APK release | 🔲 Planejado |

---

## Licença

Projeto de código aberto. Não utiliza código, marcas ou assets proprietários do GG Mouse Pro 3.  
Identidade visual ARTGG é original.

---

## Contribuição

Issues e PRs são bem-vindos. Antes de contribuir:
- Leia as limitações técnicas acima
- Não proponha contornos de anticheat
- Mantenha compatibilidade com minSdk 29
- Siga Kotlin Coding Conventions + Compose guidelines