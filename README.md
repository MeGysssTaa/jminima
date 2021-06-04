# JMinima

**JMinima** is a lightweight Java (JVM) bytecode instrumentation (assembling/disassembling/manipulation/analysis) library written on top of [**ASM**](https://asm.ow2.io/). **JMinima** attempts to encourage *declarative* style of programming to make various bytecode instrumentation routines shorter, more concise, and easier to write and understand. That is, instead of writing *"create variable X with value Y, then check if Z, ..."* you're just doing *"I want a class with the following parameters to be assembled and injected: ..."*.
