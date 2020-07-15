package com.bugbycode.client.startup;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.agent.handler.AgentHandler;
import com.bugbycode.client.handler.ClientHandler;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class NettyClient {
	
	private final Logger logger = LogManager.getLogger(NettyClient.class);
	
	private Bootstrap remoteClient;
	
	private EventLoopGroup remoteGroup;
	
	private Map<String,NettyClient> nettyClientMap;
	
	private Map<String,AgentHandler> agentHandlerMap;
	
	private String host = "";
	
	private int port;
	
	private Message msg;
	
	private String token = "";
	
	private Channel clientChannel;
	
	public NettyClient(Message msg,Map<String,NettyClient> nettyClientMap,
			Map<String,AgentHandler> agentHandlerMap,EventLoopGroup remoteGroup) {
		this.remoteClient = new Bootstrap();
		this.remoteGroup = remoteGroup;
		this.nettyClientMap = nettyClientMap;
		this.nettyClientMap.put(msg.getToken(), this);
		this.agentHandlerMap = agentHandlerMap;
		this.token = msg.getToken();
		this.msg = msg;
	}
	
	public void connection() {
		
		ConnectionInfo conn = (ConnectionInfo) msg.getData();
		host = conn.getHost();
		port = conn.getPort();
		
		this.remoteClient.group(remoteGroup).channel(NioSocketChannel.class);
		this.remoteClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS,3000);
		this.remoteClient.option(ChannelOption.TCP_NODELAY, true);
		this.remoteClient.option(ChannelOption.SO_KEEPALIVE, true);
		this.remoteClient.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new ClientHandler(agentHandlerMap,token,NettyClient.this));
			}
		});
		
		this.remoteClient.connect(host, port).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				Message message = new Message(token, MessageCode.CONNECTION_SUCCESS, null);
				if(future.isSuccess()) {
					logger.info("Connection to " + host + ":" + port + " successfully.");
					message.setType(MessageCode.CONNECTION_SUCCESS);
					clientChannel = future.channel();
				}else {
					nettyClientMap.remove(token);
					logger.info("Connection to " + host + ":" + port + " failed.");
					message.setType(MessageCode.CONNECTION_ERROR);
				}
				AgentHandler handler = agentHandlerMap.get(token);
				if(handler != null) {
					handler.sendMessage(message);
				}
			}
		});
	}
	
	public void writeAndFlush(byte[] data) {
		if(clientChannel == null) {
			return;
		}
		ByteBuf buff = clientChannel.alloc().buffer(data.length);
		buff.writeBytes(data);
		clientChannel.writeAndFlush(buff);
	}
	
	public void close() {
		
		if(clientChannel != null && clientChannel.isOpen()) {
			clientChannel.close();
		}
		
		Message message = new Message(token, MessageCode.CLOSE_CONNECTION, null);
		AgentHandler handler = agentHandlerMap.get(token);
		if(handler != null) {
			handler.sendMessage(message);
		}
		
		nettyClientMap.remove(token);
		
		logger.info("Disconnection to " + host + ":" + port + " .");
	}
}
