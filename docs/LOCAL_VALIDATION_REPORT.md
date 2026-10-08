# Skry 本地完善与验证报告

日期：2026-10-07。范围：既有离线相册审计、清理和加密保险箱；仅开发和本地验证。未发布、部署、推送远程、修改签名身份或隐私网站。接手时已有修改保留。

## 已实现

- 修复接手时 Room 缺列导致的构建失败；数据库升级至 v6，迁移保留已有审阅和保险箱记录。
- 相册同步以实际可访问的 MediaStore 快照为准，覆盖外部删除、授权撤销、部分照片授权和重新选择；编辑过的照片重新排队扫描。
- 删除统一应用确认，串行处理系统授权；Android 10 授权后重试实际删除，Android 11+ 分批请求。取消、重复点击、旋转、失败和结束后重新同步列表与统计。
- 扫描区分未扫描、失败与成功；修复解码方向、取消传播、重复启动、后台续扫和暂停状态。重复照片分组使用精确汉明距离候选索引，避免反复全量两两比较。
- 预览错误、缩放复位、选择语义和触控目标、中英文提示、中文大字体布局完善；未完成扫描时不展示“健康完成”分数。
- 保险箱保存防重入、后台自动锁定、预览取消保护、删除确认、旧副本与新原图区分。没有定位框的敏感结果采用全图马赛克；删除原图前需主动查看副本并另行确认。
- 保持无 INTERNET 权限和本地 OCR；新增可选原始位置元数据读取权限并解释缺失影响。禁止应用截图，数据库和保险箱排除备份/迁移。

## 验证证据

所有运行日志和 UI 层级记录位于项目 `build/`，属于本地忽略产物。可重跑的测试代码在 `app/src/test`、`app/src/androidTest`；隔离模拟器启动脚本为 `tools/Start-SyntheticEmulator.ps1`。

| 验证 | 结果 | 证据 |
|---|---|---|
| Debug 应用和测试 APK 构建 | 通过 | `build/local-validation.log` |
| JVM 单测 | 22 项通过，0 失败 | `app/build/test-results/testDebugUnitTest/` |
| Android lint | 0 错误、66 警告 | `app/build/reports/lint-results-debug.html` |
| Android 8 / API 26 | 10 项核心测试通过 | `build/api26-final.log` |
| Android 15 / API 35 | 迁移、3 项快照、4 项离线管线和中文大字体通过 | `build/instrumentation-api35-current.log`；其中旧删除测试失败由下行复测替代 |
| Android 15 删除端到端 | 1 项通过 | `build/api35-delete-final.log` |
| Android 15 后台续扫 | 41 张合成图跨过 40 张单次额度，重复启动后全部完成 | `build/api35-delete-worker.log` 中 WorkerFlowTest；同日志旧删除失败由专门复测替代 |
| Android 15 权限矩阵 | 全部、拒绝、部分各 1 项通过 | `build/api35-permission-{full,denied,partial}.log` |
| Android 15 系统照片选择器 | 选 1 张→显示 1；追加 1 张→显示 2；撤销→拒绝访问界面 | `build/api35-picker-{one,two,revoked}.xml` |
| Android 16 / API 36 | 11 项全部通过（146.356 秒），包含最新暂停/恢复和 41 张跨批次扫描 | `build/api36-final.log`；独立 `SkryApi36` 模拟器 |

实际测试包括 v4→v6 数据迁移、2,200 条数据库快照、10,000 个重复哈希/保存状态 URI、合成文字 OCR、损坏图片、EXIF 方向、加密读回、真实 MediaStore 删除刷新。大数据测试覆盖算法和数据库，不等于万张真实图片的整库性能测试。Android 15/16 在飞行模式下通过离线管线和扫描测试。API 26/35 核心测试在最后一个 Worker 暂停状态修复前执行；该最终修复通过 API 36 暂停/恢复测试及最终构建、JVM、lint 检查。

早期 API 35 冷启动曾出现进程启动 ANR，主线程位于 instrumentation 类加载，系统服务也出现 ANR。预编译测试包后运行稳定；不能据此证明所有低内存或 OEM 设备无 ANR。测试历史失败保留，最终结果按表中明确列出的复测判断。

## 未执行与剩余限制

- 未安装 API 29/33 专用系统镜像；Android 10 RecoverableSecurityException 重试及 Android 13 独立权限分支经过代码/编译检查，尚无该版本端到端运行证据。
- 未覆盖实体机/OEM 相册、真实生物识别硬件、完整 TalkBack 人工巡检、任意时刻进程终止，以及万张高分辨率照片的内存/耗电长测。不能把已执行的旋转测试视为完整进程死亡覆盖。
- OCR 仍为现有离线 Latin 模型和规则；中文界面不代表中文 OCR。检测和马赛克为启发式，用户必须检查副本，不能承诺发现全部敏感信息。
- lint 的 66 条警告主要涉及 KTX/Compose 风格、未使用资源、英文复数资源及依赖版本建议；本阶段未为消除提示而升级整个依赖栈。
- 隐私网站只读核查后的待办：说明 Android 14+ 部分照片访问、可选 ACCESS_MEDIA_LOCATION、数据库备份排除，并避免在未授权位置元数据时声称 GPS 检查完整。仅记录，不修改或部署网站。

## 本地交付

APK：`app/build/outputs/apk/debug/app-debug.apk`（调试包）。SHA-256：`622563CAC46E4D9F9A0246334F28AB5106E75D1259AFFBF8BD25A1C2D85E8689`。包名 `com.yishenghuang.skry`，版本仍为 1.0.3 / 4。当前改动保留在本地工作区，不包含远程发布操作。项目使用的三个隔离模拟器在验证后关闭，测试数据保留于项目 `build/isolated-avds`，未操作其他模拟器的相册。
