package com.bugbycode.agent.server;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.bugbycode.agent.handler.AgentHandler;
import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.forward.client.StartupRunnable;
import com.bugbycode.mapper.host.HostMapper;
import com.bugbycode.mapper.table.TableMapper;

import io.netty.channel.nio.NioEventLoopGroup;

@Component
@Configuration
public class AgentStartup implements ApplicationRunner {

	@Autowired
	private Map<String,AgentHandler> agentHandlerMap;
	
	@Autowired
	private Map<String,AgentHandler> forwardHandlerMap;
	
	@Autowired
	private Map<String,NettyClient> nettyClientMap;
	
	//@Value("${spring.keystore.path}")
	private String keystorePath = "";
	
	//@Value("${spring.keystore.password}")
	private String keystorePassword = "";
	
	@Value("${spring.netty.auth.host}")
	private String host;
	
	@Value("${spring.netty.auth.port}")
	private int port;
	
	@Value("${spring.netty.agent.port}")
	private int agentPort;
	
	//@Value("${spring.netty.auth.username}")
	private String username = "admin";
	
	//@Value("${spring.netty.auth.password}")
	private String password = "admin";
	
	@Autowired
	private TableMapper tableMapper;
	
	@Autowired
	private HostMapper hostMapper;
	
	@Autowired
	private NioEventLoopGroup remoteGroup;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
		
		tableMapper.initHostTable();
		
		StartupRunnable startup = new StartupRunnable(host, port, username, password, 
				keystorePath,keystorePassword,forwardHandlerMap,remoteGroup); 
		startup.run();
		AgentServer server = new AgentServer(agentPort, agentHandlerMap,forwardHandlerMap,nettyClientMap,remoteGroup,startup,hostMapper);
		new Thread(server).start();
	}

}
