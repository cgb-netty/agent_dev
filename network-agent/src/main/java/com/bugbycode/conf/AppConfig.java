package com.bugbycode.conf;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bugbycode.agent.handler.AgentHandler;
import com.bugbycode.client.startup.NettyClient;

import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

@Configuration
public class AppConfig {
	
	public static final int WORK_THREAD_NUMBER = 100;
	
	@Bean("channelGroup")
	public ChannelGroup getChannelGroup() {
		return new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
	}
	
	@Bean
	public Map<String,NettyClient> nettyClientMap(){
		return Collections.synchronizedMap(new HashMap<String,NettyClient>());
	}
	
	@Bean
	public Map<String,AgentHandler> agentHandlerMap(){
		return Collections.synchronizedMap(new HashMap<String,AgentHandler>());
	}
	
	@Bean
	public Map<String,AgentHandler> forwardHandlerMap(){
		return Collections.synchronizedMap(new HashMap<String,AgentHandler>());
	}
	
	@Bean
	public NioEventLoopGroup remoteGroup() {
		return new NioEventLoopGroup(WORK_THREAD_NUMBER);
	}
}
