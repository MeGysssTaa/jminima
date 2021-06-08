# JMinima


**JMinima** is a lightweight Java (JVM) bytecode instrumentation (assembling/disassembling/manipulation/analysis) library written on top of [**ASM**](https://asm.ow2.io/). **JMinima** attempts to encourage *declarative* style of programming to make various bytecode instrumentation routines shorter, more concise, and easier to write and understand. That is, instead of writing *"create variable X with value Y, then check if Z, ..."* you're just doing *"I want a class with the following parameters to be assembled and injected: ..."*.




# Before installing


**JMinima** requires two other libraries to work: [**Apache Commons IO**](https://commons.apache.org/proper/commons-io/) and [**ASM**](https://asm.ow2.io/). However, **JMinima** is *not* shipped with these libraries inside. You have to add them in your project build script yourself.


That is, if you are writing another library based on **JMinima**, you should install both **JMinima** and the libraries above as *compile-time* dependencies. Otherwise, if you're building a complete runnable application based on **JMinima**, make sure to install all the libraries as *run-time* dependencies.


Note that you can use other versions of these libraries as well (not just the one specified in the installation instructions below). However, make sure that the version you are using is not *too* old — otherwise certain features might not be available. If something is not working as expected, please first try to use the exact versions of libraries that **JMinima** has been built against (you can find this information in installation instructions below).




# Installation (Gradle)


```groovy
repositories {
    mavenCentral()

    maven {
        name 'Public Reflex Repository'
        url 'https://archiva.reflex.rip/repository/public/'
    }
}

def asmVersion = '9.1'

dependencies {
    implementation group: 'commons-io', name: 'commons-io', version: '2.8.0'
    implementation group: 'org.ow2.asm', name: 'asm', version: "${asmVersion}"
    implementation group: 'org.ow2.asm', name: 'asm-util', version: "${asmVersion}"
    implementation group: 'me.darksidecode.jminima', name: 'jminima', version: '1.2.0'
}
```




# Installation (Maven)

```xml
<repositories>
    <repository>
        <id>reflex.public</id>
        <name>Public Reflex Repository</name>
        <url>https://archiva.reflex.rip/repository/public/</url>
    </repository>
</repositories>

<dependencies>
      <dependency>
        <groupId>commons-io</groupId>
        <artifactId>commons-io</artifactId>
        <version>2.8.0</version>
    </dependency>
    <dependency>
        <groupId>org.ow2.asm</groupId>
        <artifactId>asm</artifactId>
        <version>9.1</version>
    </dependency>
  <dependency>
        <groupId>org.ow2.asm</groupId>
        <artifactId>asm-util</artifactId>
        <version>9.1</version>
    </dependency>
    <dependency>
        <groupId>me.darksidecode.jminima</groupId>
        <artifactId>jminima</artifactId>
        <version>1.2.0</version>
    </dependency>
</dependencies>
```




# License

[**Apache License 2.0**](https://github.com/MeGysssTaa/jminima/blob/main/LICENSE)

