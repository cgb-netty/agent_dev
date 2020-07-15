# agent_dev
这是一个使用netty实现网络代理的简单实例，本项目仅供学习研究使用。

#代理客户端配置

1、修改network-agent配置文件src/main/resources/application.yml中的spring.netty.auth.host为network-server所部属的ip或域名如下所示：

spring:

  main:
  
    web-application-type: NONE
    
  netty: 
  
    agent: 
    
      port: 50000 #自己电脑internet代理端口
      
    auth: 
    
      host: www.example.com #network-server 部署的ip地址
      
      port: 36500 #network-server 配置的代理端口
      
loggin:

  config: classpath:log4j2.xml  
  
#代理转发服务配置

1、修改network-agent配置文件src/main/resources/application.yml中的spring.netty.port为自己希望监听的端口如下所示：

spring:

  main:
  
    web-application-type: NONE
    
  netty:
  
    port: 36500 #代理端口
    
    so_backlog: 1000
    
loggin:

  config: classpath:log4j2.xml  

#使用方式
