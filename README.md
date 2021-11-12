# WebSocket-Android说明
本Demo实现的功能：

1.与websocket建立长连接

2.与websocket进行即时通讯

3.Service和Activity之间通讯和UI更新

4.心跳检测和重连（保证websocket连接稳定性）

5.服务（Service）保活.通过测试可在app手动退出的情况下，服务任运行

# Bug
2021.11.12

部分机型可能在连接socket后anr或闪退的情况，可以把 JWebSocketClientService 的 GRAY_SERVICE_ID 改为1001等，原因不知道

然后在调用App()中的hideServiceNotification方法来隐藏前台通知栏（可以在连接成功后调用）


# demo截图
![image](https://user-images.githubusercontent.com/44353535/141421089-9265dc47-03f4-49c5-be76-d2f915498091.png)

# 没多少代码，具体请直接运行demo

