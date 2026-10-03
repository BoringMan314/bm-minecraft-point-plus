# [B.M] Minecraft 點位 PLUS

[![Paper](https://img.shields.io/badge/Paper-26.3-2D2D2D)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![GitHub](https://img.shields.io/badge/GitHub-bm--minecraft--point--plus-181717?logo=github)](https://github.com/BoringMan314/bm-minecraft-point-plus)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

適用於 **Minecraft Paper 26.3** 的插件：`/home` 回該世界的床（沒有則世界重生點），`/spawn` 回該世界重生點，每世界可存 10 個個人點位。

*适用于 **Minecraft Paper 26.3** 的插件：`/home` 回该世界的床（没有则世界重生点），`/spawn` 回该世界重生点，每世界可存 10 个个人点位。*<br>
*Minecraft Paper 26.3 向け：`/home` でその世界のベッド（なければスポーン）へ、`/spawn` でスポーンへ。世界ごとに 10 地点を保存できます。*<br>
*A **Minecraft Paper 26.3** plugin. `/home` returns to that world's bed or spawn, `/spawn` returns to that world's spawn, and each world can store 10 personal points.*

> **說明**：僅支援 Paper 26.3 與 Java 25。

---

## 目錄

- [功能](#功能)
- [系統需求](#系統需求)
- [安裝方式](#安裝方式)
- [指令與權限](#指令與權限)
- [設定檔](#設定檔)
- [本機建置](#本機建置)
- [專案結構](#專案結構)
- [版本與多語系](#版本與多語系)
- [資料與隱私說明](#資料與隱私說明)
- [授權](#授權)
- [問題與建議](#問題與建議)

---

## 功能

- `/home` 傳送到目前世界的床或重生錨；沒有或已失效則傳該世界重生點。
- `/spawn` 傳送到目前世界重生點。
- `/point set 1-10 [標註]` 把目前位置存成該世界的點位，每世界最多 10 個。
- `/point` 列出目前世界已存點位：`point 1 X Y Z`，有標註則加 `(標註)`。
- `/point 1-10` 傳送到自己在目前世界的對應點位。
- 床與點位都依世界分開儲存，換世界不會共用。

---

## 系統需求

- **Paper 26.3** 伺服器。
- **Java 25**。

---

## 安裝方式

1. 從 [`dist/`](dist/) 選擇所需語系 JAR。
2. 將 JAR 放入 Paper 伺服器的 `plugins/` 資料夾。
3. 啟動伺服器後，設定檔建立於 `plugins/bm-minecraft-point-plus/`。

> 請勿同時安裝多個語系 JAR；它們是同一插件的不同預設語言版本。

---

## 指令與權限

| 指令 | 說明 | 權限 |
| --- | --- | --- |
| `/home` | 回該世界的床，沒有則世界重生點 | `.use` |
| `/spawn` | 回該世界重生點 | `.use` |
| `/point set 1-10 [標註]` | 儲存目前位置 | `.use` |
| `/point` | 列出本世界點位 | `.use` |
| `/point 1-10` | 傳送到自己的點位 | `.use` |
| `/bm-minecraft-point-plus 0/1` | 開關功能 | `.admin` |
| `/bm-minecraft-point-plus reload` | 重載設定 | `.admin` |
| `/bm-minecraft-point-plus info` | 顯示資訊 | `.admin` |
| `/bm-minecraft-point-plus status` | 顯示狀態 | `.admin` |

`bm-minecraft-point-plus.use` 預設所有玩家可用；`.admin` 預設 OP 可用。

---

## 設定檔

```yml
enabled: true
admin-require-op: true
```

---

## 本機建置

執行 `build.bat`；預設會在結束時暫停。自動化環境使用：

```bat
build.bat --no-pause
```

會在 `dist/` 產生 `zh_TW`、`zh_CN`、`ja_JP`、`en_US` JAR。

---

## 專案結構

```text
src/main/java/bm.minecraft.point.plus/
src/main/resources/lang/
src/main/resources/config.yml
```

---

## 版本與多語系

版本為 `26.3_0.0.1`；建置時以 `active-language.yml` 選擇四種語系之一。

---

## 資料與隱私說明

本插件在 `data.yml` 儲存玩家各世界的床／重生錨與十個個人點位座標，僅供本機伺服器傳送使用。

---

## 授權

本專案以 [MIT License](LICENSE) 授權。

---

## 問題與建議

歡迎透過 [GitHub Issues](https://github.com/BoringMan314/bm-minecraft-point-plus/issues) 回報錯誤或提出改善建議。回報時請一併提供 Paper 版本、Java 版本、設定檔與完整錯誤日誌。
