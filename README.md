# 出納帳スキャナー - Android Receipt Scanner App

レシート・領収書をカメラで撮影して、自動的に出納帳CSV を生成するAndroidアプリです。

## 主な機能

| 機能 | 説明 |
|------|------|
| **自動スキャン** | ボタン不要。レシートが安定したら自動キャプチャ |
| **日本語OCR** | ML Kit Text Recognition v2 で日本語レシートを読み取り |
| **AI解析** | Google Gemini API で科目・取引先・内容を自動分類 |
| **効果音** | 読み取り完了時にチャイム音を再生 |
| **CSV出力** | UTF-8 BOM (Excel対応) でCSVを自動生成 |
| **連続スキャン** | 保存後即カメラへ戻り、次のレシートを待機 |

## CSVフォーマット

```
写真番号,科目,取引先,内容,税込み価格,消費税率,撮影日時
0001,交際費,株式会社○○,会議費用,3300,10%,2026-03-18 14:23:01
```

## セットアップ

### 1. Android Studio でプロジェクトを開く

```
File > Open > /path/to/Accounting
```

### 2. Gemini API キーを設定

1. [Google AI Studio](https://aistudio.google.com) でAPIキーを無料取得
2. アプリの **設定画面** > **Gemini API キー** に入力

### 3. ビルド・実行

```bash
./gradlew assembleDebug
```

実機 (Android 8.0以上) でテストしてください。

## 効果音のカスタマイズ

`app/src/main/res/raw/scan_complete.wav` を任意のMP3/WAVに差し替えてください。

## ファイル保存場所

```
/storage/emulated/0/Android/data/com.example.receiptscanner/files/Documents/出納帳スキャナー/
├── 2026-03-18/
│   ├── receipts_20260318.csv
│   ├── receipt_0001.jpg
│   └── receipt_0002.jpg
```

## 技術スタック

- **言語**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **カメラ**: CameraX 1.4
- **OCR**: ML Kit Text Recognition (日本語)
- **AI解析**: Google Gemini 1.5 Flash (無料枠)
- **DB**: Room
- **DI**: Hilt
- **CSV**: OpenCSV

## 動作環境

- Android 8.0 (API 26) 以上
- カメラ付き端末
- インターネット接続 (Gemini API使用時)
