# Goety Arcana 1.9.6 项目整理

> Minecraft 1.20.1 / Forge 47.x / Goety 2.5.60.6  
> mod id：`goetyarcana` · 版本：**1.9.6** · 许可：MIT

---

## 一、项目概览

Goety Arcana（诡厄奥术）是 Goety（诡厄巫法）的附属模组，定位为「Goety 的奥术扩展」：从聚晶开始，逐步加入武器、仪式配方与黑暗秘典内容，不限于单一法术学派。

| 项目 | 内容 |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.x（开发依赖 47.3.22） |
| Goety | 2.5.60.6 |
| mod id | `goetyarcana` |
| 当前版本 | 1.9.6 |
| 工程目录 | `goety-soulburst`（历史名称，不影响构建） |
| 许可证 | MIT |

---

## 二、当前内容

### 1. 虚空镰刀（Void Scythe）

以第一版厚月牙设计为主体，融合 Goety 死神之镰（`death_scythe`）的 32×32 紧凑握持尺寸，并使用 `goety:item/scythe` 作为模型父级。

| 属性 | 数值 |
| --- | --- |
| 攻击伤害 | 13 点 |
| 攻击速度 | 0.9（约 1.11 秒一次） |
| 横扫范围 | 360°，水平半径 4.5 格，垂直容差 2.5 格 |
| 耐久 | 2031 |
| 获取方式 | 末地仪式（End Ritual） |

**左键：360° 横扫**

- 对周围所有合法敌人各结算一次伤害；
- 第三人称专属挥砍动作：双臂横扫、躯干扭转、头部反向补偿；
- 第一人称有横向甩镰动作；
- 命中时产生两层自制虚空火环（外圈 4.1 格、内圈 1.8–3.5 格）；
- 70% 施加虚空之蚀 I，30% 施加虚空之蚀 II，5% 施加处决。

**右键：双手蓄力 + 穿透剑气**

- 最短 5 tick（0.25 秒）可释放，30 tick（1.5 秒）蓄满；
- 专属双手蓄力动作，第一人称镰刀向身体中线收拢；
- 松开后沿视线生成一道向前飞行的月牙剑气：
  - 速度 1.0 格/tick；
  - 寿命 100 tick（5 秒）；
  - `noPhysics`，可穿墙；
  - 穿透生物，每个目标只结算一次；
  - 宽度 2.4–6 格、高度 2.2–5 格随蓄力提升；
  - 伤害为目标最大生命值 5%–15%，5% 概率处决；
  - 冷却 25 tick。

**处决（Execution）**

- 处决 debuff 持续 5 秒；
- 结束时先播放专属动画：巨型虚空镰刀斩下 + 虚空火爆发；
- 动画蓄势 `EXECUTION_ANIMATION_TICKS = 12` tick 后再结算；
- 普通敌人立即消灭；
- 精英损失 10% 最大生命值；
- Boss 损失 20% 最大生命值；
- 精英/Boss 伤害使用 `goetyarcana:execution`，可绕过 Goety 的 Boss 限伤。

### 2. 裂魂聚晶（Soul Burst Focus）

- 固定伤害 6 点；
- 额外造成目标最大生命值 7.5% 的伤害；
- 消耗 35 灵魂能量；
- 吟唱 10 tick，冷却 30 tick；
- 以虚空魔杖施放时附加虚空之蚀、夺取目标正面效果，并产生范围伤害。

### 3. 虚空弩（Void Crossbow，规划中）

当前**只有文本设定**，没有物品、模型、贴图、配方或实际机制：

- 远程虚空武器，发射充能虚空弩矢；
- 命中后短暂留下虚空裂隙；
- 具体数值待定。

---

## 三、1.9.6 收尾内容

### 模型与黑边

- 以第一版厚月牙轮廓为主体，融合死神之镰的 32×32 紧凑尺寸；
- 透明像素填为紫色 RGB `(72, 40, 112, 0)`，不再是透明黑；
- 去掉纯黑描边，最暗为深紫色；
- 缓解光影/mipmap 过滤产生的黑色边缘；
- 如果仍出现黑边，通常是光影对物品边缘的 outline/AA 后处理，可在光影设置中关闭物品描边/抗锯齿。

### 粒子统一

- 左键横扫不再使用原版 `SWEEP_ATTACK`；
- 外圈 + 内圈全部使用自制 `goetyarcana:void_flame`；
- 环绕、蓄力、剑气拖尾、处决爆发也使用同一套虚空火。

### 末地仪式配方

| 项目 | 内容 |
| --- | --- |
| 仪式类型 | `goety:ritual` / `goety:craft` |
| craftType | `end`（末地仪式） |
| 中央祭品 | 黑暗镰刀 `goety:dark_metal_scythe` |
| 外围祭品 | 2× 虚空回响 `goety:void_echo` + 2× 觉醒绿宝石 `goety:magic_emerald` |
| 时间 | 12 秒 |
| 灵魂消耗 | 每秒 5（Goety 每 20 tick 扣除一次） |
| 产物 | `goetyarcana:void_scythe` |

配方文件：`data/goetyarcana/recipes/void_scythe.json`

### 黑暗秘典

在《黑暗秘典》中新增顶层分类 **「诡厄奥术 / Goety Arcana」**，图标为虚空镰刀，集中收录模组物品：

- 虚空镰刀（介绍 + 末地仪式页 + 处决说明）
- 裂魂聚晶（从 Goety 虚空聚晶分类移入）
- 虚空弩（规划中，仅文本）

路径：

```text
assets/goety/patchouli_books/black_book/{en_us,zh_cn}/
├─ categories/goetyarcana.json
└─ entries/goetyarcana/
   ├─ void_scythe.json
   ├─ soul_burst_focus.json
   └─ void_crossbow.json
```

---

## 四、构建与安装

### 环境

- JDK 17
- Forge 47.x
- Goety 2.5.60.6
- Gradle（工程未附带 wrapper，可使用系统 Gradle）

### 构建

```bash
cd project
gradle build
```

产物：

```text
project/build/libs/goetyarcana-1.9.6.jar
```

### 安装

1. 安装 Forge 1.20.1 47.x；
2. 将 Goety 2.5.60.6 与本模组 jar 放入 `mods/`；
3. 启动游戏。

模组依赖（`mods.toml`）：

- Forge `[47,)`
- Minecraft `[1.20.1,1.21)`
- Goety `[2.5.0,)`

---

## 五、GitHub 发布

### 源码仓库

在 `project/` 中初始化 Git 并推送：

```powershell
cd project
git init
git branch -M main
git add .
git commit -m "Goety Arcana 1.9.6"
git remote add origin https://github.com/<用户名>/<仓库名>.git
git push -u origin main
```

`.gitignore` 已排除：

```text
.gradle/
build/
run/
.idea/
out/
.gradle-home/
goety-*.jar
```

不要把约 95 MB 的 Goety 本体 jar、构建缓存或 `run/` 提交到仓库。

### Release

在 GitHub 仓库页面：

1. `Releases` → `Draft a new release`
2. Tag：`v1.9.6`
3. 上传：
   - `goetyarcana-1.9.6.jar`
   - `goetyarcana-1.9.6-src.zip`
4. 填写更新说明并发布。

也可用 GitHub CLI：

```powershell
gh release create v1.9.6 `
  ..\releases\goetyarcana-1.9.6.jar `
  ..\releases\goetyarcana-1.9.6-src.zip `
  --title "Goety Arcana 1.9.6" `
  --notes "End Ritual recipe, Black Book category, Void Crossbow text"
```

### 以后更新

```powershell
# 修改代码/资源
gradle build

git add .
git commit -m "描述本次更新"
git push

# 到 GitHub Releases 上传新 jar
```

---

## 六、版本记录

| 版本 | 内容 |
| --- | --- |
| 1.9.0 | 虚空镰刀动作、穿透穿墙剑气、自制虚空火、模型重绘（因 Mixin 问题无法启动） |
| 1.9.1 | 尝试修复 Mixin 字段定位（仍失败） |
| 1.9.2 | 尝试 SRG 别名（Mixin 禁止非 private 别名，仍失败） |
| 1.9.3 | `@Shadow` 直接使用 SRG 字段名 + `remap = false`，修复启动崩溃 |
| 1.9.4 | 模型改为死神之镰 32×32 基础，加强第一人称动作，修复英文 lang JSON |
| 1.9.5 | 融合第一版厚月牙与死神之镰尺寸；黑边处理；处决专属动画 |
| 1.9.6 | 横扫粒子统一；末地仪式配方；黑暗秘典分类；裂魂聚晶移入；虚空弩文本 |

---

## 七、验收清单

进游戏后建议依次检查：

- [ ] 虚空镰刀模型大小/握持位置正常，第一人称不挡视野；
- [ ] 光影下镰刀周围没有明显黑边；
- [ ] 左键有横扫动作，右击蓄力有双手动作；
- [ ] 剑气向前飞、穿透生物、可穿墙、约 5 秒消失；
- [ ] 左键横扫粒子是紫色虚空火，不再混用原版白色横扫；
- [ ] 末地仪式：中央黑暗镰刀 + 2 虚空回响 + 2 觉醒绿宝石，12 秒 / 每秒 5 灵魂；
- [ ] 《黑暗秘典》→「诡厄奥术」能看到虚空镰刀、裂魂聚晶、虚空弩三页；
- [ ] 处决 debuff 结束后能看到巨型镰刀斩击动画；
- [ ] 裂魂聚晶仍能正常施放。

### 已知注意事项

- 光影若额外对物品做描边/抗锯齿，黑边可能仍会出现，需要在光影设置中关闭对应后处理；
- 如果安装了 Epic Fight 等替换玩家模型/动画的模组，第三人称自定义手臂动作可能被其覆盖，可先用第一人称验证；
- 处决动画会在 debuff 结束后延迟约 12 tick 结算，不是立刻死亡；
- 虚空弩目前只有文本，没有物品。

---

## 八、文件索引

| 内容 | 路径 |
| --- | --- |
| 模组入口 | `src/main/java/com/goetyarcana/GoetyArcanaMod.java` |
| 数值 | `src/main/java/com/goetyarcana/VoidScytheBalance.java` |
| 动作 Mixin | `src/main/java/com/goetyarcana/mixin/HumanoidModelMixin.java` |
| 第一人称动作/粒子 | `src/main/java/com/goetyarcana/client/VoidScytheClientEvents.java` |
| 剑气 | `src/main/java/com/goetyarcana/entity/ScytheWaveEntity.java` |
| 处决动画 | `src/main/java/com/goetyarcana/entity/ExecutionSlashEntity.java` |
| 处决渲染 | `src/main/java/com/goetyarcana/client/ExecutionSlashRenderer.java` |
| 虚空火粒子 | `src/main/java/com/goetyarcana/client/VoidFlameParticle.java` |
| 末地仪式配方 | `src/main/resources/data/goetyarcana/recipes/void_scythe.json` |
| 黑暗秘典分类 | `src/main/resources/assets/goety/patchouli_books/black_book/zh_cn/categories/goetyarcana.json` |
| 中文语言 | `src/main/resources/assets/goetyarcana/lang/zh_cn.json` |
| 模型贴图 | `src/main/resources/assets/goetyarcana/textures/item/` |

---

*本文档由项目当前 1.9.6 源码与发布信息整理；如后续改动数值/配方，请同步更新本文件。*
