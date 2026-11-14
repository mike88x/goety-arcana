# Goety Arcana 实现文档

面向想改数值、改机制或接手维护的人。玩的话看 [README.md](README.md) 就够了。

- 目标环境：Minecraft 1.20.1 / Forge 47.3.22（运行时 47.4.29）/ Goety 2.5.60.6
- mod id：`goetyarcana`；工程目录名 `goety-soulburst` 是历史遗留
- 许可证：MIT

---

## 一、工程结构

```
project/
├─ build.gradle / settings.gradle / gradle.properties
├─ README.md                     # 面向玩家
├─ IMPLEMENTATION.md             # 本文件
├─ docs/                         # 预览图
└─ src/main/
   ├─ java/com/goetyarcana/
   │  ├─ GoetyArcanaMod.java          # 入口（含 LOGGER）
   │  ├─ VoidScytheBalance.java       # ★ 镰刀全部数值
   │  ├─ SoulBurstBalance.java        # ★ 裂魂聚晶全部数值
   │  ├─ registry/
   │  │  ├─ ModItems.java             # 裂魂聚晶 / 虚空镰刀 / 虚空弩 / 虚空弩矢
   │  │  ├─ ModEntities.java          # 灵魂弹 / 剑气 / 虚空弩矢 / 虚空爆炸 / 处决动画
   │  │  ├─ ModEffects.java           # 处决 / 虚空之眼 / 流血 / 虚空爆炸
   │  │  ├─ ModParticles.java         # 自制虚空火粒子类型
   │  │  ├─ ModCreativeTabs.java      # 「诡厄奥术」创造页
   │  │  ├─ ModDamageTypes.java       # soul_burst / void_wave / execution
   │  │  └─ ModTags.java              # bosses / elites 实体标签
   │  ├─ item/
   │  │  ├─ SoulBurstFocus.java
   │  │  ├─ VoidScytheItem.java       # 13/0.9、右键蓄力、松开剑气
   │  │  ├─ VoidScytheTier.java
   │  │  ├─ VoidCrossbowItem.java     # 弩：弹药优先级、层数装填、枪口
   │  │  └─ VoidBoltItem.java         # 弩矢：createArrow 时判定长袍
   │  ├─ combat/
   │  │  ├─ ScytheCombat.java         # 360° 近战、虚空之蚀、处决触发
   │  │  ├─ CombatTier.java           # 普通/精英/Boss 分级
   │  │  ├─ CombatUtil.java           # 目标筛选
   │  │  ├─ VoidExplosion.java        # 统一爆炸表现 + 伤害
   │  │  └─ VoidRobe.java             # 虚空长袍检测（护甲槽 + Curios）
   │  ├─ effect/                      # Execution / VoidEye / Bleed / VoidExplosion
   │  ├─ entity/
   │  │  ├─ SoulBurstEntity.java
   │  │  ├─ ScytheWaveEntity.java     # 横向穿透剑气 + 扫掠判定
   │  │  ├─ ExecutionSlashEntity.java
   │  │  ├─ VoidBoltEntity.java       # 弩矢：追踪 / 穿透 / 引信
   │  │  └─ VoidExplosionEntity.java  # 纯客户端表现的双环动画载体
   │  ├─ event/
   │  │  ├─ ScytheEvents.java         # 左键实体/方块/空挥入口
   │  │  ├─ VoidCrossbowEvents.java   # 叠层、换武器清除、头顶紫火
   │  │  └─ CreativeTabEvents.java
   │  ├─ network/ModNetwork.java      # 空挥发包
   │  ├─ spell/SoulBurstSpell.java
   │  ├─ mixin/HumanoidModelMixin.java # 第三人称挥砍/双手蓄力动作
   │  └─ client/
   │     ├─ ClientEvents.java         # 渲染器、粒子、ItemProperties
   │     ├─ VoidScytheClientEvents.java
   │     ├─ VoidFlameParticle.java    # 自制虚空火
   │     ├─ ExecutionSlashRenderer.java / ExecutionDeathAnimation.java
   │     ├─ ScytheWaveRenderer.java
   │     ├─ VoidBoltRenderer.java     # 原版 ArrowRenderer 模板 + 虚空贴图
   │     └─ VoidExplosionRenderer.java # 地面双环
   └─ resources/
      ├─ META-INF/mods.toml
      ├─ goetyarcana.mixins.json / goetyarcana.refmap.json
      ├─ assets/goetyarcana/
      │  ├─ lang/{en_us,zh_cn}.json
      │  ├─ particles/void_flame.json
      │  ├─ models/item/…            # 镰刀帧模型、弩 / 已装填弩、弩矢
      │  └─ textures/{item,entity,particle}/…
      ├─ assets/goety/patchouli_books/black_book/{en_us,zh_cn}/…  # 黑暗秘典条目
      └─ data/
         ├─ goetyarcana/damage_type/{soul_burst,void_wave,execution}.json
         ├─ goetyarcana/tags/entity_types/{bosses,elites}.json
         ├─ goetyarcana/recipes/{soul_burst_focus,void_scythe,void_crossbow,void_crossbow_bolt}.json
         ├─ goetyarcana/advancements/recipes/misc/void_crossbow_bolt.json
         └─ minecraft/tags/damage_type/{bypasses_armor,bypasses_effects,bypasses_enchantments,bypasses_invulnerability}.json
```

---

## 二、数值常量

### 虚空镰刀

`src/main/java/com/goetyarcana/VoidScytheBalance.java`：

| 常量 | 含义 | 默认 |
| --- | --- | --- |
| `ATTACK_DAMAGE` | 面板伤害 | 13.0F |
| `ATTACK_SPEED` | 面板攻速 | 0.9F |
| `MELEE_RADIUS` | 360° 横扫半径 | 4.5 |
| `MELEE_HEIGHT` | 横扫垂直容差 | 2.5 |
| `VOID_EROSION_TICKS` | 虚空之蚀时长 | 100 tick |
| `VOID_EROSION_I_CHANCE` | 虚空之蚀 I 概率 | 0.70F |
| `EXECUTION_CHANCE` | 处决概率 | 0.05F |
| `EXECUTION_TICKS` | 处决 debuff 时长 | 100 tick |
| `EXECUTION_ANIMATION_TICKS` | 处决动画蓄势/延迟结算 | 12 tick |
| `EXECUTION_DEATH_TICKS` | 处决致死后的死亡动画时长 | 20 tick |
| `EXECUTION_ELITE_PERCENT` | 精英处决伤害 | 0.10F |
| `EXECUTION_BOSS_PERCENT` | Boss 处决伤害 | 0.20F |
| `MIN_CHARGE_TICKS` | 最短可释放蓄力 | 5 tick |
| `FULL_CHARGE_TICKS` | 蓄满时间 | 30 tick |
| `WAVE_LIFETIME` | 剑气寿命 | 100 tick（5 秒） |
| `WAVE_SPEED` | 剑气速度 | 1.0 格/tick |
| `WAVE_MIN_WIDTH` / `WAVE_MAX_WIDTH` | 剑气宽度 | 3 / 7 格 |
| `WAVE_MIN_HEIGHT` / `WAVE_MAX_HEIGHT` | 剑气高度 | 1.6 / 3 格 |
| `WAVE_LAUNCH_TICKS` / `WAVE_DISSIPATE_TICKS` | 起飞加速 / 末尾减速 | 4 / 12 tick |
| `WAVE_LAUNCH_SPEED` / `WAVE_END_SPEED` | 起飞 / 消散速度倍率 | 0.5 / 0.75 |
| `WAVE_MIN_PERCENT` / `WAVE_MAX_PERCENT` | 剑气百分比 | 5% / 15% |
| `WAVE_HIT_DEPTH` | 剑气扫掠判定厚度 | 1.25 格 |
| `WAVE_COOLDOWN_TICKS` | 剑气冷却 | 25 tick |

### 裂魂聚晶

`src/main/java/com/goetyarcana/SoulBurstBalance.java`：

| 常量 | 含义 | 默认 |
| --- | --- | --- |
| `FIXED_DAMAGE` | 固定伤害 | 6.0F |
| `PERCENT_OF_MAX_HEALTH` | 最大生命值百分比 | 0.075F |
| `POTENCY_BONUS` | 每级「强效」额外固定伤害 | 1.0F |
| `SOUL_COST` | 灵魂能量消耗 | 35 |
| `CAST_DURATION` | 吟唱 tick | 10 |
| `COOLDOWN` | 冷却 tick | 30 |
| `PROJECTILE_SPEED` | 射弹速度（格/tick） | 3.6 |
| `SPAWN_OFFSET` | 出生点沿视线前移 | 0.6 |
| `VOID_AOE_RADIUS` | 范围伤害半径 | 3.0 |
| `VOID_AOE_MULTIPLIER` | 范围伤害倍率 | 0.5F |
| `PROJECTILE_LIFETIME` | 射弹存活 tick | 60 |

### 虚空弩（散落在各类里）

| 位置 | 数值 |
| --- | --- |
| `VoidCrossbowItem.BASE_ARROW_SPEED` | 1.6（每层 +20%） |
| `VoidCrossbowItem` 层数装填 | 阈值 ×（1 − 0.2 × 层数） |
| `VoidBoltEntity.MAX_PIERCES` | 6 |
| `VoidBoltEntity.FUSE_TICKS` / `_HOMING` | 80 / 140 tick |
| `VoidBoltEntity.TRACK_RANGE` / `TRACK_TURN` / `TRACK_CONE` | 26 格 / 0.5 / −0.2 |
| `VoidCrossbowEvents.DURATION` | 20×20 tick（虚空之眼时长） |
| `VoidExplosion` 伤害 | 10 点 × 距离衰减（最低 30%） |

---

## 三、实现要点

### 虚空镰刀

1. **360° 近战**：`ScytheEvents` 取消 `AttackEntityEvent` / `LeftClickBlock`，由 `ScytheCombat` 对 AABB 内所有合法目标逐个 `hurt`；空挥由 `ModNetwork` 发包到服务端。
2. **专属动作**：`HumanoidModelMixin` 注入 `HumanoidModel#setupAnim` 末尾叠加手臂/躯干旋转（玩家模型的 `super.setupAnim` 之后会自动复制袖子/外套，避免覆盖层错位）；左键读 `getAttackAnim()` 做横扫，右键读 `getTicksUsingItem()` 做双手蓄力。第一人称由 `VoidScytheClientEvents#onRenderHand` 处理。
3. **蓄力**：`VoidScytheItem#use` 开始使用，`releaseUsing` 按使用 tick 算 charge，沿视线生成 `ScytheWaveEntity`。
4. **横向穿透剑气**：`ScytheWaveEntity` 每 tick 沿速度方向前进，用 smoothstep 在起飞/消散阶段加减速，`noPhysics` 保证穿墙；渲染按实体朝向把横向刃面转到垂直飞行方向；命中盒取「上一 tick + 当前 tick」的并集，`Set<UUID>` 保证每个目标只结算一次，命中后剑气继续飞。
5. **百分比伤害**：剑气伤害 = `目标最大生命值 × Mth.lerp(charge, 5%, 15%)`，没有固定值；`void_wave` 只带 `bypasses_armor` / `bypasses_effects` / `bypasses_enchantments`，不带 `bypasses_invulnerability`。
6. **处决**：`ExecutionEffect` 在最后一 tick 结算；普通怪 `kill()`，精英/Boss 用带 `bypasses_invulnerability` 的 `execution` 伤害；致死时向 96 格内客户端发 `ExecutionDeathPacket`，由 `ExecutionDeathAnimation` 播 20 tick「虚空湮灭」。
7. **模型/动画**：以死神之镰 32×32 轮廓为底，父级 `goety:item/scythe`；四档刀身 + `layer1/layer2` 光环；`charge` predicate 在四个阈值切换模型。
8. **自制虚空火**：`ModParticles` 注册 `goetyarcana:void_flame`，`VoidFlameParticle` + 4 帧贴图。

### 虚空弩

1. **装填/发射**：`VoidCrossbowItem` 继承 `CrossbowItem`；层数只影响 `releaseUsing` 里的阈值判断与 `performShooting` 初速。原版「装填后右键直接发射」走 `use()`，所以枪口特效挂在 `use()` 里，而不是 `releaseUsing`。
2. **弹药优先级**：原版 `LivingEntity#getProjectile` 只用两个 `Predicate<ItemStack>` 挑弹药，谓词拿不到射击者。所以在 `use` / `releaseUsing` 里把射击者塞进 `ThreadLocal LOADING_FOR`，谓词据此判断 `hasBolt(shooter)`：有弩矢就拒绝普通箭，背包扫描自然落到弩矢上。谓词只在两处有上下文，其它调用点按「无偏好」处理。
3. **模型切换**：`void_crossbow.json` 用 `overrides` + `charged` 谓词切到 `void_crossbow_loaded.json`。JSON 的 predicate 键必须和 `ItemProperties.register` 的名字一致——两者都注册了 `minecraft:charged` 与 `goetyarcana:charged` 两种写法。
4. **追踪**：`VoidBoltEntity#steerToTarget` 每 tick 把速度方向朝目标 lerp（保留速度），带提前量；每 4 tick 或目标失效时重选，索敌锥约 100°；穿透后立刻清空目标，避免绕回去打尸体。
5. **穿透**：`onHitEntity` 里自己结算伤害（`ceil(速度 × baseDamage)` 夹 1~40，末影人走无 owner 魔法伤害），`piercedIds` 去重；**另外每 tick 用 `sweepPierce(from,to)` 扫一遍这一 tick 走过的线段**，因为箭速可达 2.5 格/tick，原版每 tick 只回报最近的**一个**碰撞结果，会漏怪/隧穿。第 6 个目标命中后 `explode()`。
6. **引信**：普通模式 80 tick，追踪模式 140 tick（留时间绕行），到点在当前位置爆炸。
7. **长袍判定**：`combat/VoidRobe` 按注册 id `goety:void_robe` 查 `ForgeRegistries` 并缓存；Curios 是可选依赖，`CuriosVoidRobe` 全程反射（`CuriosApi.getCuriosInventory → LazyOptional.orElse → ICuriosItemHandler.getEquippedCurios → IItemHandler`），且只在 `ModList.isLoaded("curios")` 为真时被类加载，反射失败只降级 + 打一条 warn。
8. **爆炸**：`VoidExplosion.explode` 统一入口（弩矢、debuff、空爆都走这里），表现层整体抬 0.3 格，伤害判定仍用原爆炸点；`VoidExplosionRenderer` 画地面双环。

### 合成与手册

- 虚空弩：`data/goetyarcana/recipes/void_crossbow.json`（`goety:ritual` + `craftType: end`，`activation_item` = 原版弩，4 个外围祭品）。
- 虚空弩矢：`data/goetyarcana/recipes/void_crossbow_bolt.json`（`crafting_shapeless`，1 液态虚空瓶 + 1 箭 → 1 支）。
- 配方书解锁：`data/goetyarcana/advancements/recipes/misc/void_crossbow_bolt.json`。
- 黑暗秘典：`assets/goety/patchouli_books/black_book/{en_us,zh_cn}/entries/goetyarcana/*.json`，分类在 `categories/goetyarcana.json`。

---

## 四、踩过的坑 / 注意事项

- **Mixin 字段**：`@Shadow` 直接用生产环境 SRG 名（`f_102808_` 等）并 `remap = false`；用别名会被 Mixin 0.8.5 拒绝。
- **ItemProperties 注册时机**：必须在 `FMLClientSetupEvent` 里直接注册，放进 `enqueueWork` 会晚于模型烘焙，override 永远不生效。
- **模型 override 键名**：JSON 的 predicate 键与注册名必须完全一致，否则静默走默认模型。
- **动画贴图**：本环境里 `item/generated` + 16×N 竖直动画条（8 帧、`.mcmeta` 带 `frametime`）是可用的；弩和弩矢都用这条管线。
- **Patchouli 文本**：页面文本会被格式化解析，**半角 `%` 会让整页变成 `Format error:` + 原文直出**；中文写「三成」或全角 `％`，英文写 “percent”。`$(br2)`、`$(bold)…$()` 是合法命令。
- **爆炸观感**：粒子数和 spread 别开太大，贴脸时会把屏幕糊成大白团；双环半径也要收敛，否则摄像机在环中心时会看到一圈巨型方块。
- **百分比伤害与限伤**：`void_wave` / `soul_burst` 不带 `bypasses_invulnerability`；只有 `execution` 带，用来绕过 Goety 的 Boss 限伤。
- **Boss/精英分级**：优先查 `goetyarcana:bosses` / `elites` 实体标签，再退回原版 Boss、Goety 包名、最大生命值兜底；整合包可以用数据包给别的模组实体加标签。
- **伤害只在服务端结算**，客户端只负责模型、粒子、剑气/爆炸渲染。
- **前置**：Forge 47.x + Goety 2.5.x（Curios / Patchouli）。

---

## 五、怎么改贴图

几张贴图是用脚本生成的，改之前先看脚本，直接手改会被下次生成覆盖：

| 产物 | 生成脚本 |
| --- | --- |
| `textures/item/void_crossbow_loaded_anim.png`（已装填弩，8 帧） | `tools/gen_crossbow_loaded.py`（仓库外的归档目录） |
| `textures/item/void_crossbow_bolt.png`（弩矢图标，8 帧） | `tools/gen_bolt_item.py` |
| 虚空火粒子、剑气、镰刀帧模型/贴图 | `tools/` 下对应的 `gen_*.py` / `build_*.py` |

> 这些脚本在归档仓库的 `goety-arcana/tools/` 下，不随 `project/` 一起发布。
