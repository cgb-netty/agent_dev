package com.bugbycode.proxy.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class StartupRunnable implements Runnable {

	private final Logger logger = LogManager.getLogger(StartupRunnable.class);
	
	private int serverPort; 
	
	private int so_backlog;
	
	private ChannelHandler serverChannelInitializer;
	
	private NioEventLoopGroup remoteGroup;
	
	public StartupRunnable(int serverPort, int so_backlog,NioEventLoopGroup remoteGroup, ChannelHandler serverChannelInitializer) {
		this.serverPort = serverPort;
		this.so_backlog = so_backlog;
		this.serverChannelInitializer = serverChannelInitializer;
		this.remoteGroup = remoteGroup;
	}

	@Override
	public void run() {
		
		ServerBootstrap bootstrap = new ServerBootstrap();
		
		bootstrap.group(remoteGroup, remoteGroup).channel(NioServerSocketChannel.class)
				.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.option(ChannelOption.SO_BACKLOG, so_backlog)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
				.childHandler(serverChannelInitializer);
		
		bootstrap.bind(serverPort).addListener(new ChannelFutureListener() {

			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					logger.info("Proxy server startup successfully, port " + serverPort + "......");
				} else {
					logger.info("Proxy server startup failed, port " + serverPort + "......");
					close();
				}
			}
		});
	}

	public void close() {
		logger.info("Proxy server shutdown, port " + serverPort + "......");
	}
}
