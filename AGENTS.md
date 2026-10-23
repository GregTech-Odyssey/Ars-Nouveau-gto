# Agent 须知

本仓库是 GregTech Odyssey 整合包维护的新生魔艺分支（Ars Nouveau GTO Edition），只维护 `1.20` 分支（Minecraft 1.20.1 Forge）。

- 任何改变游戏设计的修改（数值、机制、配方规则、物品与方块行为、显示与反馈等），必须在同一次提交中更新根目录的 [GTO_CHANGES.md](GTO_CHANGES.md)，记录方式见该文档的"维护约定"。
- GTO_CHANGES.md 只记录游戏设计层面的差异，不记录实现方式。
- 模组 ID 保持 `ars_nouveau`，不得改动资源命名空间与 Java 包名，以免破坏存档与附属模组兼容。
- 新生魔艺本体不得依赖 GregTech、GTOCore、GTOLib 等整合包模组；需要整合包参与的行为，通过通用接口或配方字段开放，由整合包侧注册或声明。
- 本分支的代码与素材依据 LGPLv3 授权，例外是并入 Ars Énergistique 时带进来的、以 AE2 贴图为基础改绘的 AE 联动贴图（如魔源存储元件及其外壳），沿用 AE2 素材的 CC BY-NC-SA 3.0（含非商业条款）。上游自 2025-02 起在 main 系分支将素材改为保留所有权利，因此不得从上游 main、1.21 等分支移植贴图、模型、音效等素材；移植代码前也要先确认对应提交的许可证。引入第三方素材时，要在 README 的许可证一节写明来源与协议。
- 允许的外部依赖例外只有 DataSyncLib、GTOUtils 与 GTORenders（另有可选的 AE2 联动，见下文）。DataSyncLib：方块实体向客户端的同步统一走它（`ModdedTile` 实现 `BlockEntitySync.Holder`，每 tick 合并、无人追踪跳过、只发变化的顶层 NBT 键），存盘仍用原版 NBT。`getUpdatePacket` 不再返回原版整包，改为登记同步；新增需要同步的状态照常写进 `saveAdditional` 并调用 `updateBlock()`，客户端只用部分字段的类覆写 `writeSyncTag`/`readSyncTag`（如 `RepositoryTile`）。不要修改 DataSyncLib 本身。
- GTOUtils（`com.suried.gtoutils`）：按范围找方块实体一律用它的索引，不要逐格扫描世界或遍历全维度。全部索引集中在 `ArsBlockEntityIndexes`（模组构造时初始化，新索引只能加在这里）：按类型收录用 `BlockEntityIndex`，区块增删方块实体时由 GTOUtils 自动登记注销，方块实体里不写钩子；按能力收录用 `ProbedBlockEntityIndex`；只能逐个判断能力的低频查找用 `ALL`。查询范围用 `BlockRange` 选球形或长方体，结果依赖原版遍历顺序的用 `BlockOrder` 排序。按方块状态在一片范围里找格子（找某种方块、数某种方块）用 `LevelSections` 的 `betweenClosed`/`inRange`/`withinManhattan`，它先按 16³ 区段的调色板跳过不可能命中的区段，结果顺序与原版逐格遍历相同，不要逐格 `getBlockState`。
- GTORenders（`com.suried.gtorenders`）：发光、闪光、连线三种粒子的 Provider 调用 `MoteParticles`（带本模组原贴图的 `MoteLook`，返回 null，不生成粒子对象），由 GPU 批量绘制；新代码照常 `addParticle(GlowParticleData...)` 即可，外观与原版一致。按像素画液面或贴图盒用 `SpriteQuads`（见 `LargeSourceJarRenderer`）。不要为了发光效果新建粒子类型或纯视觉实体。
- 视觉特效一律由客户端生成：服务端不生成纯视觉实体（`EntityFollowProjectile`、`EntityFlyingItem`），不逐粒子或每 tick 发包。需要服务端告知时机的，用 `ClientFx`（`spawn`、`poof`、`orb`、`beam`、`emit`、`windshear`）发一个 `PacketClientFx` 小事件，由客户端本地重建；持续的特效用 `emit` 一次性告诉客户端持续时长。
- 玩家魔力由客户端按服务端同步的回魔速度本地推算，服务端只在偏差、上限或回魔速度变化时以及定时兜底时校正（`ManaCapEvents`）。
- 应用能源 2（AE2）是可选联动，取代了 Ars Énergistique：只在 `common/compat/ae2` 里引用 AE 类（compileOnly，坐标来自 GTO maven 的 AE fork），入口 `ArsAE2.init` 由 `ModList.isLoaded("ae2")` 守住，没装 AE 时不得加载这个包里的任何类；核心代码需要 AE 参与时开放钩子（如 `ScribesTile.registerItemSource`、`ArsBlockEntityIndexes.registerSourceCandidateClass/registerSourceProbe`）由联动包注册。联动的配方放在 `data/ars_nouveau/recipes/ae2/` 并带 `forge:mod_loaded` 条件，方块掉落写在代码里，帕秋莉条目带 `"flag": "mod:ae2"`；改完要在不装 AE 的情况下跑一次 `runServer` 确认无报错。`arseng` 命名空间的注册名（含 `ae2:keytypes` 里的 `arseng:source`）通过别名和缺失映射转到 `ars_nouveau`。
- 魔源在两个持有者之间转移一律走 `ISourceTile.transfer(from, to, amount)`：它先模拟目标能收多少、从来源扣、只给实际扣到的量、给不出去的退回来源。单边加减用 `addSource/removeSource(amount, simulate)`，返回实际变化量（旧的 `addSource(int)`/`removeSource(int)` 返回的是新存量，不能当搬运量用）；存取结果与读数可能不一致的实现（ME 魔源罐、AE 槽位适配、AE 能量适配、包装类）必须覆写这两个方法如实返回。不要手写"先给后扣"；代理整个 ME 网络的方块不跨操作缓存读数，存储总线对着同网的 ME 魔源罐视为空。
- 魔源能力 `CapabilityRegistry.SOURCE_TILE`（`IAdvancedSourceTile`）是中继器连接与魔源查找的统一入口：普通 `ISourceTile` 方块实体自动挂上包装，其他方块实体自己提供该能力即可。`SourceUtil` 的范围查找里，魔源罐走 `ArsBlockEntityIndexes.SOURCE_JARS`，其余带该能力的方块实体走 `SOURCE_TILES`，都不要逐格扫描。
- 构建与运行都用 JDK 21（DataSyncLib 是 Java 21 字节码），构建工具是 ModDevGradle legacyforge。

## 发布

- 发布方式与 GTO 的 AE、GTM、植物魔法分支相同：使用 `com.gto.gtopublishgradleplugin`，发布到 `https://gtodyssey.com/maven/releases`，坐标为 `com.hollingsworth.ars_nouveau:ars_nouveau-1.20.1-forge-1.20.1:<版本>`（插件按 `archivesBaseName` 加 `-forge-1.20.1` 生成），发布的是重混淆后的正式 jar（`jar` 之后自动跑 `reobfJar` 原地覆盖，不内嵌 GeckoLib 与 MixinExtras）。凭据从 `~/.gradle/gradle.properties` 的 `gtodysseyRepositoryUsername` / `gtodysseyRepositoryPassword` 读取，不得写入仓库。
- 版本号写在 `gradle.properties` 的 `gto_version`，格式为 `x.y.z`，从上游 4.12.7 之后的 `4.12.8` 起，每次发布递增最后一段（发布插件只接受 `x.x.x[-后缀]`）。
- 本地联调用 `./gradlew publishToMavenLocal`，GTOCore 的 `ars_nouveau_version` 改成同一个版本号即可。正式发布用 `./gradlew gtoValidate`，确认无误后再运行 `./gradlew gtoPublishMaven`。
- 整合包统一发布时，如果本仓库自上次发布以来有改动，需要和 AE、GTM、植物魔法一起发布新版本，并同步更新 GTOCore 的 `ars_nouveau_version`。
- CurseForge 项目 ID 为 1755416，已开启 CurseForge 上传；令牌放在 `~/.gradle/gradle.properties` 的 `gtoCurseforgeToken`。`./gradlew gtoPublish` 会依次完成预检、构建，并发布到 Maven 和 CurseForge。CurseForge 文件的前置依赖写在 `gtoPublish` 的 `curseforgeRelations`（键是项目 slug，现为 `gtoutils: 'required'`），上传时自动声明，不用去网页补；新增外部依赖时同步加上。
