# Building CordSync

## Prerequisites

- **Java Development Kit (JDK) 17** or higher
- **Apache Maven 3.8+**
- **Git** (for cloning the repository)

## Quick Start

```bash
# Clone the repository
git clone https://github.com/musbabaff/CordSync.git
cd CordSync

# Build the plugin
mvn clean package

# The compiled JAR will be at:
# target/CordSync-{version}.jar
```

## Detailed Steps

### 1. Install JDK 17+

Download and install from [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/).

Verify installation:
```bash
java -version
```

### 2. Install Maven

Download from [Apache Maven](https://maven.apache.org/download.cgi) or use a package manager:

```bash
# Windows (Chocolatey)
choco install maven

# macOS (Homebrew)
brew install maven

# Linux (apt)
sudo apt install maven
```

Verify installation:
```bash
mvn -version
```

### 3. Build

```bash
mvn clean package
```

The shaded JAR (with all dependencies included) will be generated at `target/CordSync-{version}.jar`.

### 4. Install

Copy the JAR file to your server's `plugins/` directory and restart the server.

## IDE Setup

### IntelliJ IDEA
1. Open the project folder
2. IntelliJ will auto-detect the Maven project
3. Click "Load Maven Project" if prompted
4. Run `mvn clean package` from the Maven tool window

### Eclipse
1. File → Import → Maven → Existing Maven Projects
2. Select the project folder
3. Right-click project → Run As → Maven Build → Goals: `clean package`

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `java: error: release version 17 not supported` | Update your JDK to version 17+ |
| `Cannot resolve symbol 'JavaPlugin'` | Ensure Maven dependencies are downloaded |
| `BUILD FAILURE` | Run `mvn clean install -U` to force update dependencies |
