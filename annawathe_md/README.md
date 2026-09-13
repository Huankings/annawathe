# AnnaWathe 文档索引 / Documentation Index

AnnaWathe 是面向原版 **Wathe: Murder Mystery（1.3.2 ~ 1.4.1）** 的**扩展框架 Mod**，同时自带一整套原版加强玩法。
AnnaWathe is an **extension framework mod** for vanilla **Wathe: Murder Mystery (1.3.2 – 1.4.1)**, bundled with a full set of vanilla-enhancing gameplay rules.

---

## 文档地图 / Document Map

| 文件 File | 语言 Language | 内容 Content |
| --- | --- | --- |
| [`03-简短介绍-中文.md`](03-简短介绍-中文.md) | 中文 | **简短版介绍**（适合发布到 Mod 网站）：机制重做清单、指令表、API 一览、兼容性 |
| [`03-Brief-Introduction-English.md`](03-Brief-Introduction-English.md) | English | **Brief introduction** (for mod listing pages): reworked mechanics, commands, API list, compatibility |
| [`01-模组介绍-中文.md`](01-模组介绍-中文.md) | 中文 | 完整模组介绍：定位、环境、玩法特性、机制数值、指令、兼容性与已知问题 |
| [`01-Mod-Overview-English.md`](01-Mod-Overview-English.md) | English | Full mod overview: positioning, requirements, features, mechanics, commands, compatibility and known issues |
| [`02-API参考-中文.md`](02-API参考-中文.md) | 中文 | 扩展开发参考：24 组公开 API 的方法签名、结果语义、最小示例与边界警告 |
| [`02-API-Reference-English.md`](02-API-Reference-English.md) | English | Add-on development reference: method signatures, result semantics, minimal examples and boundary warnings for all 24 public APIs |

**想快速了解这个 Mod**，先看 `03` 简短介绍；**想看完整机制数值与开发细节**，看 `01` 与 `02`。
**For a quick overview**, start with `03`; **for full mechanic values and development details**, read `01` and `02`.

中英两版内容**逐节对应、信息量对等**，可并列阅读或单独发布。
The Chinese and English editions are **section-by-section parallel with equal information density**, and can be read side by side or published separately.

---

## 30 秒速览 / 30-Second Summary

**它是什么** —— AnnaWathe 给原版 Wathe 装了一层注册式 API：本能透视、胜利仲裁、心情任务、商店经济、疯魔模式、各类 HUD、目标可见性等原本需要深层 Mixin 才能改的地方，全部变成扩展可以注册规则的标准入口。它自己**不新增职业、物品或方块**。
**What it is** — AnnaWathe adds a registration-based API layer on top of vanilla Wathe. Instinct highlighting, victory arbitration, mood tasks, shop & economy, Psycho Mode, HUDs and target visibility — everything that previously required deep Mixin patching — becomes a standard registration point. It adds **no new roles, items or blocks**.

**它改了什么玩法** —— 独立定价的杀手商店（12 项）、多货币支付、按自改 Wathe 节奏重做的心情系统（1/4000 每 tick）、5 个新任务、任务点透视（`Y` 键）、可 profile 化的疯魔模式（护盾/锁栏/临时物品/皮肤）、重绘的欢迎与结算界面、尸体死亡信息、开局 30 秒免碰撞、调试变形。
**What it changes in play** — an independently priced killer shop (12 entries), multi-currency payment, a reworked mood curve (1/4000 per tick), 5 new tasks, task-point wallhack (`Y` key), profile-driven Psycho Mode (shields, hotbar locking, temporary items, skins), redrawn welcome/round-end screens, corpse death info, 30-second spawn collision immunity, and debug transformations.

**关键数字** —— 158 个 Java 文件 / 6574 行；8 个 CCA 组件；48 个 Mixin；约 105 个注册型扩展点；产物 `annawathe-1.0.0-1.21.1.jar`。
**Key numbers** — 158 Java files / 6,574 lines; 8 CCA components; 48 Mixins; ~105 registration-based extension points; artifact `annawathe-1.0.0-1.21.1.jar`.

---

## 一句话定位 / One-Line Positioning

> AnnaWathe 不是"Wathe 的下一版"，而是"Wathe 的扩展底座"。
> AnnaWathe is not "the next version of Wathe" — it is "the extension substrate for Wathe".

它和生态里其他扩展的分工不同：Kin's Wathe 与 Wathe: Extended 主要**加职业、加物品、加配置**；AnnaWathe 主要**换规则、开接口**。
It occupies a different niche from other add-ons: Kin's Wathe and Wathe: Extended mainly **add roles, items and config**; AnnaWathe mainly **replaces rules and opens interfaces**.

---

## 快速事实 / Quick Facts

| 项目 Item | 值 Value |
| --- | --- |
| 模组 id / Mod id | `annawathe` |
| 版本 / Version | `1.0.0-1.21.1` |
| Minecraft | 1.21.1 |
| Java | 21 |
| 加载器 / Loader | Fabric Loader 0.17.2+ |
| 基础 Mod / Base mod | Wathe `1.3.2-1.21.1` ~ `1.4.1-1.21.1`（支持范围，推荐写法 `>=1.3.2-1.21.1 <=1.4.1-1.21.1`） |
| 必需依赖 / Required deps | Fabric API、Cardinal Components API 6.1.1 |
| 环境 / Environment | 双端（客户端 + 服务端都要装）/ Both sides required |
| 软兼容 / Soft compat | HarpyModLoader（可选）/ optional |
| 互斥 / Incompatible with | 自改版 Wathe jar / modified Wathe jars |

---

*本索引与配套文档基于 `D:\哈比快车最新源码\原版哈比列车\annawathe` 的实际源码整理，所有数值均取自源码常量与语言文件，未做推测。*
*This index and its companion documents were compiled from the actual source of `D:\哈比快车最新源码\原版哈比列车\annawathe`; every number comes from source constants and language files — nothing is inferred.*
