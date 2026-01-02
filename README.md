Sonata --A learning VPN project implemented by java

### Build tools : gradle

### version : 0.1

### 设计目标和原则
>FSM 是唯一协议决策者
> session执行fsm产生的protocolEffect
> Transport 只处理字节流
> 避免过度开发
> 以最小可运行闭环驱动设计

### 模块划分 (current)

transport

packet

protocol (FSM)

session

sandbox (demo / 实验)

_*单项依赖*_

### current stage

✔ 单连接 handshake

✔ client / server demo

✘ 并发

✘ 完整 proxy

✘ 加密


### 运行/调试(HandshakeDemo)

``` cmd
cd .

# terminal 1
.\gradlew.bat :module-common-sandbox:runHandshakeServer --no-daemon

# terminal 2
.\gradlew.bat :module-common-sandbox:runHandshakeClient --no-daemon
```



        





