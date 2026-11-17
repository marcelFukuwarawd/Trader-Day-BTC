# ₿ BTC Trader Sim — Simulador Day/Swing BTC/USDT (Android)

Simulador de paper trading automático para BTC/USDT na Binance.
- Capital inicial: **$100**, **sem alavancagem** (spot 1x)
- Estratégia: EMA9/EMA21 + RSI + ATR (pullback), stop 1.5x ATR, TP 2R
- Risco por trade: 1% ($1) | máx. 3 trades/dia | perda máx. dia: $5
- Taxas reais simuladas: 0,1% por lado + slippage 0,05%
- **Relatório diário automático às 21h** (notificação + histórico)
- **Widget 5x5** na tela inicial com saldo, P&L e posição

## Como gerar o APK
1. Baixe este repositório como ZIP (botão Code → Download ZIP)
2. Abra a pasta no Android Studio (Open)
3. Build → Generate Signed Bundle / APK → APK → crie uma keystore → Release
4. O APK final sai em `app/release/app-release.apk`
5. Instale no celular e conceda: notificações + alarme exato (Configurações do app)

> ⚠️ Simulador educacional. Resultados de simulação não garantem lucro real.
