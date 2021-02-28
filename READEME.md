#### NRPC 项目介绍

旨在帮助中小型企业快速搭建可用的RPC-中间件服务

项目依赖netty进行底层的网络异步通信
   依赖zookeeper进行服务的注册与发现
   使用spring，进行ioc容器管理，以及其他动态代理，编解码，序列化等相关功能
   
项目会在TCP协议之上，自建一套私有协议完成编解码以及业务的协议逻辑处理

#### NRPC 项目结构
项目包括nrpc-server，
       nrpc-client，
       nrpc-common
       
       
       

  
   
   
   
   
