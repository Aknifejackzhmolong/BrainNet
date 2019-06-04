
- [在线演示](http://202.197.66.217:18080)
- [使用文档](https://shimo.im/docs/p8GvHzdf7n0bhF2c/)

## 功能
- [x] 登录/注销
- [x] 文件上传/下载
- [x] SaaS核磁共振影像数据预处理
- [x] WebSocket通信反馈处理结果
- [x] ...

## 开发和发布
```bash
# 克隆项目
git clone https://github.com/Aknifejackzhmolong/BrainNet.git

# 安装依赖&打包
mvn install package

# 后台启动服务
java -jar brainnet-0.0.1-SNAPSHOT.jar --server.port=8080 --spring.profiles.active=dev > brainnet.log 2> brainnet.error &

# 脚本和数据暂不提供