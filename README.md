## jPOS Template

Clone this project in order to create your own jPOS based application.

The template ships with a `.sdkmanrc` file that pins Java 25.0.1-amzn and Gradle 9.4.0 so every shell can run `sdk env install && sdk env` to reproduce the toolchain.
It also applies the published `org.jpos.jposapp` Gradle plugin (v0.0.16), which replaces the legacy `jpos-app.gradle` helper and provides the `installApp`, `dist`, `distnc`, `zip`, and `run` tasks out of the box.

We recommend that you install [Gradle](http://gradle.org/) in order to build your jPOS projects, but if you don't have it installed, you can use the Gradle wrapper scripts `gradlew` and `gradlew.bat`. In the following instructions, when we say `gradle` we really mean either your installed Gradle or one of the wrapper scripts (depending if you are on Unix or DOS based platforms).

### Build an eclipse project
````
gradle eclipse
````

### Build an IDEA project
````
gradle idea
````

### Build your own jar
````
gradle jar
````

### Check the jPOS version
````
gradle version
````

### Create a distribution of your application
````
gradle dist
````
This creates a tar gzipped file in the `build/distributions` directory.

### Install application in 'build/install' directory
````
gradle installApp
````
Installs application in `build/install` with everything you need to run jPOS. Once the directory is created, you can `cd build/install` and call `java -jar your-project-version.jar` or the `bin/q2` (or `q2.bat`) script available in the `bin` directory.

### Generate an install a Maven artifact
````
gradle install
````

### List available Gradle tasks
````
gradle tasks
````

### 8583 ASCII Q2 server (port 8080)
The template now includes a Q2 `QServer` deployment at `src/dist/deploy/20_ascii_8583_server.xml`.
It listens on `8080` using `org.jpos.iso.channel.ASCIIChannel` + `XMLPackager` and replies with:
- response MTI (for example `0200 -> 0210`)
- field `39=00`

### Run and test the server
1. Install app:
````
./gradlew installApp
````
2. Start Q2:
````
./build/install/jpos-template-card/bin/q2
````
3. In another terminal, run the integration test:
````
./gradlew test --tests org.jpos.template.iso.AsciiQ2ServerIntegrationTest
````

### Build and run Docker image
````
docker build -t jpos-template-card:local .
docker run --rm -p 8080:8080 jpos-template-card:local
````

