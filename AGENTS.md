# Agent 须知

本仓库是 GregTech Odyssey 整合包维护的新生魔艺分支（Ars Nouveau GTO Edition），只维护 `1.20` 分支（Minecraft 1.20.1 Forge）。

- 任何改变游戏设计的修改（数值、机制、配方规则、物品与方块行为、显示与反馈等），必须在同一次提交中更新根目录的 [GTO_CHANGES.md](GTO_CHANGES.md)，记录方式见该文档的"维护约定"。
- GTO_CHANGES.md 只记录游戏设计层面的差异，不记录实现方式。
- 模组 ID 保持 `ars_nouveau`，不得改动资源命名空间与 Java 包名，以免破坏存档与附属模组兼容。
- 新生魔艺本体不得依赖 GregTech、GTOCore、GTOLib 等整合包模组；需要整合包参与的行为，通过通用接口或配方字段开放，由整合包侧注册或声明。
- 本分支的代码与素材均依据 LGPLv3 授权。上游自 2025-02 起在 main 系分支将素材改为保留所有权利，因此不得从上游 main、1.21 等分支移植贴图、模型、音效等素材；移植代码前也要先确认对应提交的许可证。
- 允许的外部依赖例外只有 DataSyncLib 与 GTOUtils（另有可选的 AE2 联动，见下文）。DataSyncLib：方块实体向客户端的同步统一走它（`ModdedTile` 实现 `BlockEntitySync.Holder`，每 tick 合并、无人追踪跳过、只发变化的顶层 NBT 键），存盘仍用原版 NBT。`getUpdatePacket` 不再返回原版整包，改为登记同步；新增需要同步的状态照常写进 `saveAdditional` 并调用 `updateBlock()`，客户端只用部分字段的类覆写 `writeSyncTag`/`readSyncTag`（如 `RepositoryTile`）。不要修改 DataSyncLib 本身。
- GTOUtils（`com.suried.gtoutils`）：按范围找方块实体一律用它的八叉树索引（`LevelBlockIndex`/`BlockIndex`，查询范围用 `BlockRange` 选球形或立方体），方块实体在 `onLoad` 登记、`setRemoved` 注销，不要再逐格扫描世界；现有用法见 `SourceIndex`。
- 视觉特效一律由客户端生成：服务端不生成纯视觉实体（`EntityFollowProjectile`、`EntityFlyingItem`），不逐粒子或每 tick 发包。需要服务端告知时机的，用 `ClientFx`（`spawn`、`poof`、`orb`、`beam`、`emit`、`windshear`）发一个 `PacketClientFx` 小事件，由客户端本地重建；持续的特效用 `emit` 一次性告诉客户端持续时长。
- 玩家魔力由客户端按服务端同步的回魔速度本地推算，服务端只在偏差、上限或回魔速度变化时以及定时兜底时校正（`ManaCapEvents`）。
- 应用能源 2（AE2）是可选联动，取代了 Ars Énergistique：只在 `common/compat/ae2` 里引用 AE 类（compileOnly，坐标来自 GTO maven 的 AE fork），入口 `ArsAE2.init` 由 `ModList.isLoaded("ae2")` 守住，没装 AE 时不得加载这个包里的任何类；核心代码需要 AE 参与时开放钩子（如 `ScribesTile.registerItemSource`、`SourceTileIndex.registerCandidateClass/registerProbe`）由联动包注册。联动的配方放在 `data/ars_nouveau/recipes/ae2/` 并带 `forge:mod_loaded` 条件，方块掉落写在代码里，帕秋莉条目带 `"flag": "mod:ae2"`；改完要在不装 AE 的情况下跑一次 `runServer` 确认无报错。`arseng` 命名空间的注册名（含 `ae2:keytypes` 里的 `arseng:source`）通过别名和缺失映射转到 `ars_nouveau`。
- 魔源能力 `CapabilityRegistry.SOURCE_TILE`（`IAdvancedSourceTile`）是中继器连接与魔源查找的统一入口：普通 `ISourceTile` 方块实体自动挂上包装，其他方块实体自己提供该能力即可。`SourceUtil` 的范围查找里，魔源罐走 `SourceIndex`，其余带该能力的方块实体走 `SourceTileIndex`（区块增删方块实体时由 `LevelChunkMixin` 登记），都不要逐格扫描。
- 构建与运行都用 JDK 21（DataSyncLib 是 Java 21 字节码），构建工具是 ModDevGradle legacyforge。

## 发布

- 发布方式与 GTO 的 AE、GTM、植物魔法分支相同：使用 `com.gto.gtopublishgradleplugin`，发布到 `https://gtodyssey.com/maven/releases`，坐标为 `com.hollingsworth.ars_nouveau:ars_nouveau-1.20.1-forge-1.20.1:<版本>`（插件按 `archivesBaseName` 加 `-forge-1.20.1` 生成），发布的是重混淆后的正式 jar（`jar` 之后自动跑 `reobfJar` 原地覆盖，不内嵌 GeckoLib 与 MixinExtras）。凭据从 `~/.gradle/gradle.properties` 的 `gtodysseyRepositoryUsername` / `gtodysseyRepositoryPassword` 读取，不得写入仓库。
- 版本号写在 `gradle.properties` 的 `gto_version`，格式为 `x.y.z`，从上游 4.12.7 之后的 `4.12.8` 起，每次发布递增最后一段（发布插件只接受 `x.x.x[-后缀]`）。
- 本地联调用 `./gradlew publishToMavenLocal`，GTOCore 的 `ars_nouveau_version` 改成同一个版本号即可。正式发布用 `./gradlew gtoValidate`，确认无误后再运行 `./gradlew gtoPublishMaven`。
- 整合包统一发布时，如果本仓库自上次发布以来有改动，需要和 AE、GTM、植物魔法一起发布新版本，并同步更新 GTOCore 的 `ars_nouveau_version`。
- CurseForge 项目 ID 为 1755416，已开启 CurseForge 上传；令牌放在 `~/.gradle/gradle.properties` 的 `gtoCurseforgeToken`。`./gradlew gtoPublish` 会依次完成预检、构建，并发布到 Maven 和 CurseForge。
