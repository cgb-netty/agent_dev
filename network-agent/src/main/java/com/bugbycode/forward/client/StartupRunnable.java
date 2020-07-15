package com.bugbycode.forward.client;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.agent.handler.AgentHandler;
import com.bugbycode.config.HandlerConst;
import com.bugbycode.config.IdleConfig;
import com.bugbycode.forward.handler.ClientHandler;
import com.bugbycode.handler.MessageDecoder;
import com.bugbycode.handler.MessageEncoder;
import com.bugbycode.module.Authentication;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;
import com.util.ssl.SSLContextUtil;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class StartupRunnable implements Runnable {

	private final Logger logger = LogManager.getLogger(StartupRunnable.class);
	
	private String host;
	
	private int port;
	
	private String username;
	
	private String password;
	
	private String keyStorePath;
	
	private String keyStorePassword;
	
	private Map<String,AgentHandler> agentHandlerMap;
	
	private Channel clientChannel;
	
	private EventLoopGroup group;
	
	public StartupRunnable(String host, int port, String username, String password,
			String keyStorePath,String keyStorePassword,
			Map<String,AgentHandler> agentHandlerMap) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		this.keyStorePath = keyStorePath;
		this.keyStorePassword = keyStorePassword;
		this.agentHandlerMap = agentHandlerMap;
	}

	@Override
	public void run() {
		Bootstrap client = new Bootstrap();
		group = new NioEventLoopGroup();
		client.group(group).channel(NioSocketChannel.class);
		client.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
		client.option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT);
		client.option(ChannelOption.TCP_NODELAY, true);
		client.option(ChannelOption.SO_KEEPALIVE, true);
		client.handler(new ChannelInitializer<SocketChannel>() {

			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.config().setAllocator(UnpooledByteBufAllocator.DEFAULT);
				/*
				SslContext context = SSLContextUtil.getClientContext(keyStorePath, keyStorePassword);
				SSLEngine engine = context.newEngine(ch.alloc());
		        engine.setUseClientMode(true);
		        ch.pipeline().addLast(new SslHandler(engine));
		        */
				ch.pipeline().addLast(new IdleStateHandler(IdleConfig.READ_IDEL_TIME_OUT,
						IdleConfig.WRITE_IDEL_TIME_OUT,
						IdleConfig.ALL_IDEL_TIME_OUT, TimeUnit.SECONDS));
				 ch.pipeline().addLast(new MessageDecoder(HandlerConst.MAX_FRAME_LENGTH, HandlerConst.LENGTH_FIELD_OFFSET, 
							HandlerConst.LENGTH_FIELD_LENGTH, HandlerConst.LENGTH_AD_JUSTMENT, 
							HandlerConst.INITIAL_BYTES_TO_STRIP));
				 ch.pipeline().addLast(new MessageEncoder());
				 ch.pipeline().addLast(new ClientHandler(StartupRunnable.this,agentHandlerMap));
			}
			
		});
		
		client.connect(host, port).addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					clientChannel = future.channel();
					logger.info("Connection to " + host + ":" + port + " success...");
					Message msg = new Message();
					msg.setType(MessageCode.AUTH);
					Authentication authInfo = new Authentication(username, password);
					msg.setData(authInfo);
					writeAndFlush(msg);
				 } else{
					 logger.error("Connection to " + host + ":" + port + " failed...");
					 restart();
				 }
			}
		});
	}
	
	public void restart() {
		if(this.group != null) {
			this.group.shutdownGracefully();
		}
		if(this.clientChannel != null && this.clientChannel.isOpen()) {
			this.clientChannel.close();
		}
		this.run();
	}
	
	public void writeAndFlush(Object msg) {
		if(this.clientChannel != null && clientChannel.isOpen()) {
			this.clientChannel.writeAndFlush(msg);
		}else {
			throw new RuntimeException("Unconnected forward server.");
		}
	}
}
