# 智能学习辅助系统 UML 交付目录

本目录包含 9 张课程核心 UML 图、15 张智能学习业务扩展 UML 图、StarUML 可导入模型、StarUML 一键生成扩展和实验报告。

## 一键重新生成

```powershell
powershell -ExecutionPolicy Bypass -File docs\uml-staruml-delivery\generate-all.ps1
```

## 一键安装 StarUML 扩展

```powershell
powershell -ExecutionPolicy Bypass -File docs\uml-staruml-delivery\install-staruml-extension.ps1
```

## 一键打开 StarUML 并点击扩展生成图

最简单方式：双击交付目录里的 `一键打开StarUML并安装扩展.bat`。脚本会把扩展安装到 StarUML 用户扩展目录，然后打开 StarUML。StarUML 打开后，在顶部菜单选择：

`Tools > 生成课程核心 9 张 UML 图` 或 `Tools > 生成推荐 24 张 UML 图`

点击后会在 Model Explorer 中生成对应的 9 张或 24 张 UML 模型图。

也可以用 PowerShell 运行同一个流程：

```powershell
powershell -ExecutionPolicy Bypass -File docs\uml-staruml-delivery\install-and-open-staruml.ps1
```

## 备用：直接打开 StarUML 模型文件

```powershell
powershell -ExecutionPolicy Bypass -File docs\uml-staruml-delivery\open-in-staruml.ps1
```

该脚本会查找 `staruml` 命令或常见 Windows 安装路径，并直接用 StarUML 打开 `sources/smart-learning-system.mdj`。

## 一键使用 StarUML CLI 导出全部图片

```powershell
powershell -ExecutionPolicy Bypass -File docs\uml-staruml-delivery\export-staruml-images.ps1
```

StarUML 官方 CLI 的 `image` 命令默认选择器是 `@Diagram`，因此该脚本会从 `sources/smart-learning-system.mdj` 一次导出全部图。

## 主要产物

- `images/`：24 张 UML 图片 PNG。
- `images/contact-sheet.png`：24 张图的接触表，便于快速检查整体可读性。
- `sources/*.puml`：每张图的 PlantUML 骨架，便于课程答辩说明建模逻辑。
- `sources/uml-model-spec.json`：StarUML 扩展和图片生成共同使用的结构化模型源。
- `sources/smart-learning-system.mdj`：可直接在 StarUML 中打开的模型项目。
- `staruml-extension/smart-learning-uml-generator/`：StarUML 扩展源码。
- `smart-learning-uml-generator-staruml-extension.zip`：可解压安装的 StarUML 扩展包。
- `一键打开StarUML并安装扩展.bat`：双击后安装扩展并打开 StarUML，然后在 Tools 菜单选择核心 9 张或推荐 24 张生成入口。
- `open-in-staruml.ps1`：直接启动 StarUML 并打开 `.mdj` 模型。
- `install-and-open-staruml.ps1`：安装 StarUML 扩展后启动 StarUML。
- `export-staruml-images.ps1`：调用 StarUML CLI `image` 命令，把 `.mdj` 中全部图导出为 PNG。
- `report/智能学习辅助系统-UML建模实验报告.docx`：含每张 UML 图片和说明的实验报告。
- `report/智能学习辅助系统-UML建模实验报告.pdf`：若本机安装 Word，`generate-all.ps1` 会自动导出 PDF。

## StarUML 使用

1. 扩展生成：双击 `一键打开StarUML并安装扩展.bat`，等待 StarUML 打开。
2. 在 StarUML 顶部菜单点击 `Tools > 生成课程核心 9 张 UML 图` 或 `Tools > 生成推荐 24 张 UML 图`。
3. 生成后，在左侧 `Model Explorer` 展开 `智能学习辅助系统 UML 模型`，即可查看对应的 9 张或 24 张 UML 模型图。
4. 备用模型：StarUML 选择 `File > Open`，打开 `sources/smart-learning-system.mdj`。
5. 批量导图：安装 StarUML CLI 后运行 `export-staruml-images.ps1`，会按官方 `image` 命令导出全部图。
6. Windows 扩展目录通常是 `C:\Users\<用户名>\AppData\Roaming\StarUML\extensions\user`。

## 图清单

- `01-use-case.png`：课程核心，图1 智能学习辅助系统用例图，Use Case Diagram
- `02-class.png`：课程核心，图2 智能学习辅助系统类图，Class Diagram
- `03-object.png`：课程核心，图3 考试提交场景对象图，Object Diagram
- `04-sequence.png`：课程核心，图4 学生考试提交顺序图，Sequence Diagram
- `05-communication.png`：课程核心，图5 学生考试提交协作图，Communication Diagram
- `06-state.png`：课程核心，图6 考试提交记录状态图，Statechart Diagram
- `07-activity.png`：课程核心，图7 考试提交与分析活动图，Activity Diagram
- `08-component.png`：课程核心，图8 智能学习辅助系统组件图，Component Diagram
- `09-deployment.png`：课程核心，图9 Docker Compose 部署图，Deployment Diagram
- `10-role-use-case.png`：业务扩展，图10 角色分组用例图，Use Case Diagram
- `11-exam-assignment-class.png`：业务扩展，图11 考试作业核心类图，Class Diagram
- `12-user-course-permission-class.png`：业务扩展，图12 用户课程权限类图，Class Diagram
- `13-assignment-grading-object.png`：业务扩展，图13 作业批改场景对象图，Object Diagram
- `14-login-sequence.png`：业务扩展，图14 登录认证顺序图，Sequence Diagram
- `15-assignment-grading-sequence.png`：业务扩展，图15 作业批改顺序图，Sequence Diagram
- `16-ai-paper-sequence.png`：业务扩展，图16 AI 组卷顺序图，Sequence Diagram
- `17-analysis-notification-communication.png`：业务扩展，图17 学情分析通知协作图，Communication Diagram
- `18-ai-generation-communication.png`：业务扩展，图18 AI 生成协作图，Communication Diagram
- `19-assignment-submission-state.png`：业务扩展，图19 作业提交状态图，Statechart Diagram
- `20-notification-state.png`：业务扩展，图20 通知消息状态图，Statechart Diagram
- `21-login-activity.png`：业务扩展，图21 登录认证活动图，Activity Diagram
- `22-assignment-activity.png`：业务扩展，图22 作业发布批改活动图，Activity Diagram
- `23-ai-analysis-activity.png`：业务扩展，图23 AI 分析推荐活动图，Activity Diagram
- `24-core-microservice-component.png`：业务扩展，图24 核心微服务组件细化图，Component Diagram

## 文档依据

- StarUML 官方扩展文档：`main.js` 作为入口，`app.commands.register` 注册命令，`menus/*.json` 声明菜单。
- StarUML 官方 CLI 文档：`staruml image <file> -f png -o "out/<%=filenamify(element.name)%>.png"` 可批量导出全部图，默认选择器为 `@Diagram`。
- 项目本地文档：`docs/architecture.md`、`docs/deployment.md`、`docs/data-ownership.md`、`docs/service-boundary-and-monolith-shrink-plan.md`。