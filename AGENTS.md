# Agent 须知

GTORenders 是 GregTech Odyssey 系列模组（GTOCore、GTM fork、新生魔艺 fork、植物魔法 fork 等）共用的渲染库，Minecraft 1.20.1 Forge，JDK 21，ForgeGradle 6。

- 包名 `com.suried.gtorenders`，模组 ID `gtorenders`，Maven 坐标 `com.gto:gtorenders-forge-1.20.1:<版本>`（gtopublish 插件按 `archivesName` 加 `-forge-1.20.1` 生成）。
- 只放与具体玩法无关的通用渲染代码，不得依赖任何其它模组；着色器与生成贴图放在 `gtorenders` 命名空间下。
- 渲染器分六类：环（`RenderProFrame.ring`）、光点粒子（`MoteParticles`，代替模组自己的简单粒子，由粒子 Provider 调用后返回 null）、连线（`LinkEffects`）、爆散（`BurstEffects`）、原版粒子发射器（`ParticleEmitters`）、贴图盒（`SpriteQuads`）。光点一律在 GPU 上计算（`GpuMotes`：出生时写一次参数，变换反馈着色器每帧展开成四边形，再交给原版 eyes/粒子着色器绘制以兼容光影包），外观由 `MoteLook` 决定：GTO 自己的效果用像素光点，替代其他模组的粒子时一律用该模组原贴图（`MoteSprite.of(spriteSet)`），保留原模组特色。新需求先套进这六类；样式用参数记录（`*Style`），由使用方定义常量，库里不放具体玩法的样式。
- 渲染类全部标 `@OnlyIn(Dist.CLIENT)`；模组主类和服务端会加载的类不得引用客户端类，专用服必须能正常启动。
- 性能：每帧不分配对象，绘制命令复用对象池，批量提交给 GPU；OpenGL 能力不足时整体关闭并只记一次日志，不得崩溃。
- 不写单元测试，正确性与性能在使用方的游戏内验证。类注释最多两行，方法和字段上不写注释。
- 授权 PolyForm Noncommercial 1.0.0（与 GTOCore 代码相同），只允许非商业使用；LICENSE 首行是 `Required Notice`。
- 版本号写在 `gradle.properties` 的 `mod_version`，格式 `yy.mm.n`。本地联调用 `./gradlew publishToMavenLocal`；发布用 `./gradlew gtoValidate` 后 `./gradlew gtoPublish`，凭据从 `~/.gradle/gradle.properties` 读取，不得写入仓库。
