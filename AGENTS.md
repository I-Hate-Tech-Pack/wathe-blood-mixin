# AGENTS.md

## 项目概况

Minecraft NeoForge Mod 项目，使用 `net.neoforged.gradle.userdev` 插件构建。

## CI/CD 配置

### 配置文件

| 文件 | 用途 |
|------|------|
| `.github/workflows/ci.yml` | CI 流水线：`gradle check` + `gradle build` |
| `.github/workflows/publish.yml` | Publish 流水线：发布 Jar + sourcesJar 到自建 Maven |
| `build.gradle` | 定义 sourcesJar 任务和 publishing 仓库配置 |

### Secrets 配置

在 GitHub 仓库 **Settings → Secrets and variables → Actions** 中添加：

| Secret | 用途 |
|--------|------|
| `MAVEN_USERNAME` | Maven 仓库用户名 |
| `MAVEN_PASSWORD` | Maven 仓库密码（或 Token） |

### sourcesJar 配置 (build.gradle)

```groovy
tasks.register('sourcesJar', Jar) {
    archiveClassifier = 'sources'
    manifest {
        attributes([
                "Specification-Title": "pipez",
                "Specification-Vendor": "Max Henkel",
                "Specification-Version": "1",
                "Implementation-Title": "pipez",
                "Implementation-Version": "${version}",
                "Implementation-Vendor": "Max Henkel",
                "Implementation-Timestamp": new Date().format("yyyy-MM-dd'T'HH:mm:ssZ")
        ])
    }
    from sourceSets.main.allJava
}
```

### Publishing 配置 (build.gradle)

```groovy
publishing {
    publications {
        mavenJava(MavenPublication) {
            artifact Jar
            artifact sourcesJar
        }
    }
    repositories {
        maven {
            name = "HowXu"
            url = uri("https://maven.howxu.cn/")
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }
        }
    }
}
```

> **注意**：`maven-publish` 插件默认使用项目的 `group` 和 `version` 作为 `groupId` 和 `version`，无需显式指定。

### Publish Workflow

```yaml
name: Publish

on:
  push:
    branches:
      - main
      - master

jobs:
  publish:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Setup JDK
        uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: 'temurin'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Generate Data
        run: ./gradlew runData

      - name: Publish
        run: ./gradlew publish
        env:
          MAVEN_USERNAME: ${{ secrets.MAVEN_USERNAME }}
          MAVEN_PASSWORD: ${{ secrets.MAVEN_PASSWORD }}
```

### CI Workflow

```yaml
name: CI

on:
  push:
    branches:
      - main
      - master
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Setup JDK
        uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: 'temurin'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Check
        run: ./gradlew check

      - name: Build
        run: ./gradlew build
```

## 常见问题与应对策略

### 2. 插件移除后报错 `Missing plugin 'de.maxhenkel.cursegradle'`

**原因**：`mod.gradle` 脚本依赖 `cursegradle` 插件。

**原则**：不要轻易移除项目正在使用的插件。如果不需要某些插件的上传功能，只删除使用对应 Secrets 的地方，不要删除插件本身。

### 3. `runData` 任务不存在

**原因**：有些项目没有定义 `runData` 任务。

**解决**：如果项目不需要 data 生成步骤，从 workflow 中移除该步骤。

### 4. 环境变量警告（如 `CURSEFORGE_API_KEY`）

**原因**：某些插件会读取环境变量，即使没有使用也会警告。

**原则**：AGENTS.md 中没有列出的 Secrets 不要添加到 workflow 的 `env` 中。如果插件仍然报错，再考虑移除插件。

### 5. 插件选择原则

| 插件 | 用途 | 是否可删 |
|------|------|---------|
| `maven-publish` | 发布到自建 Maven | 否（核心功能） |
| `cursegradle` | 上传 CurseForge | 如果不用则可删 |
| `minotaur` | 上传 Modrinth | 如果不用则可删 |
| `mod-update` | Mod 更新检查 | 如果不用则可删 |

## GitHub Secrets 类型说明

| 类型 | 作用域 |
|------|--------|
| **Repository secrets** | 仅当前仓库可用 |
| **Environment secrets** | 关联特定 Environment，可限制分支和审批 |
| **Organization secrets** | 该组织下所有仓库可用 |

优先级：Environment > Repository > Organization

## 其他注意事项

- **Repository name 不合法**：Maven 仓库名只能包含 `A-Za-z0-9_\-.`，不能用单引号或空格
- **sourcesJar 任务不存在**：`sourcesJar` 不会自动创建，必须用 `tasks.register('sourcesJar', Jar)` 显式定义