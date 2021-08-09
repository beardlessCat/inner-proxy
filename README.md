# 工程简介

# 延伸阅读


各组件退出处理
- client退出,触发ExposeServerHandler#inactive()【正常现象】
发送退出消息，关闭innerClient。

- innerClient退出，触发InnerClientHandler#inactive()【正常现象】
不进行任何处理，当有新的消息进入时，发现连接已经断开，将会重新启动客户端连接server。

- proxyClient退出，触发ProxyServerHandler#inactive()【正常现象】
触发ProxyServerHandler#inactive()方法，关闭与之关联的exposeServer

- proxyServer退出，触发ProxyClientHandler#inactive()【异常】
触发ProxyClientHandler#inactive()方法，
