package com.bugbycode.agent.handler;

import java.util.LinkedList;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.bugbycode.client.startup.NettyClient;
import com.bugbycode.forward.client.StartupRunnable;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;
import com.bugbycode.module.Protocol;
import com.util.RandomUtil;
import com.util.StringUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;

public class AgentHandler extends SimpleChannelInboundHandler<ByteBuf> {

	private final Logger logger = LogManager.getLogger(AgentHandler.class);
	
	private Map<String,AgentHandler> agentHandlerMap;
	
	private Map<String,AgentHandler> forwardHandlerMap;
	
	private Map<String,NettyClient> nettyClientMap;
	
	private boolean firstConnect = false;
	
	private boolean isForward = false;
	
	private EventLoopGroup remoteGroup;
	
	private StartupRunnable startup;
	
	private final String token;
	
	private LinkedList<Message> queue;
	
	private boolean isClosed;
	
	public AgentHandler(Map<String, AgentHandler> agentHandlerMap, 
			Map<String,AgentHandler> forwardHandlerMap,
			Map<String,NettyClient> nettyClientMap,
			EventLoopGroup remoteGroup,
			StartupRunnable startup) {
		this.agentHandlerMap = agentHandlerMap;
		this.forwardHandlerMap = forwardHandlerMap;
		this.nettyClientMap = nettyClientMap;
		this.remoteGroup = remoteGroup;
		this.startup = startup;
		this.firstConnect = true;
		this.queue = new LinkedList<Message>();
		this.token = RandomUtil.GetGuid32();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
		byte[] data = new byte[msg.readableBytes()];
		msg.readBytes(data);
		if(firstConnect) {
			firstConnect = false;

			String connectionStr = new String(data).trim();
			logger.info(connectionStr);
			
			String[] connectArr = connectionStr.split("\r\n");
			
			int port = 0;
			String host = "";
			int protocol = Protocol.HTTP;
			
			for(String dataStr : connectArr) {
				if(dataStr.startsWith("GET") || dataStr.startsWith("POST")
						 || dataStr.startsWith("HEAD") || dataStr.startsWith("OPTIONS")
						 || dataStr.startsWith("PUT") || dataStr.startsWith("DELETE")
						 || dataStr.startsWith("TRACE")) {
					if(dataStr.contains("ftp:")) {
						protocol = Protocol.FTP;
					}
				}else if(dataStr.startsWith("CONNECT")) {
					protocol = Protocol.HTTPS;
				}else if(dataStr.startsWith("Host:")) {
					String[] serverArr = dataStr.split(":");
					int len = serverArr.length;
					if(len == 2) {
						if(protocol == Protocol.HTTPS) {
							port = 443;
						}else if(protocol == Protocol.HTTP) {
							port = 80;
						}else if(protocol == Protocol.FTP) {
							port = 21;
						}
						host = serverArr[1];
					}else if(len == 3) {
						port = Integer.valueOf(serverArr[2]);
						host = serverArr[1];
					}else {
						throw new RuntimeException("Host error.");
					}
				}else if(dataStr.startsWith("Proxy-Connection:")) {
					dataStr = dataStr.replace("Proxy-Connection:", "Connection:");
				}
			}
			
			if(StringUtil.isBlank(host) || port == 0) {
				throw new RuntimeException("Protocol error.");
			}
			
			host = host.trim();
			
			/*if(host.endsWith("google.com") || host.endsWith("facebook.com")
					 || host.endsWith("youtube.com") || 
					 host.endsWith("ytimg.com")) {
				isForward = true;
			}*/
			
			ConnectionInfo con = new ConnectionInfo(host, port);
			Message conMsg = new Message(token, MessageCode.CONNECTION, con);
			
			//threadPool.addMessage(conMsg);
			
			/*if(isForward) {
				forwardHandlerMap.put(token, this);
				startup.writeAndFlush(conMsg);
			}else {
				new NettyClient(conMsg, nettyClientMap, agentHandlerMap,remoteGroup)
				.connection();
			}*/
			
			new NettyClient(conMsg, nettyClientMap, agentHandlerMap,remoteGroup)
			.connection();
			
			Message message = read();
			
			if(message.getType() == MessageCode.CONNECTION_ERROR) {
				
				forwardHandlerMap.put(token, this);
				
				startup.writeAndFlush(conMsg);
				
				message = read();
				
				if(message.getType() == MessageCode.CONNECTION_ERROR) {
					throw new RuntimeException("Connection to " + host + ":" + port + " failed.");
				}
				
				isForward = true;
			}
			
			new WorkThread(ctx).start();
			
			message.setType(MessageCode.TRANSFER_DATA);
			message.setToken(token);
			if(protocol == Protocol.HTTPS) {
				String response = "HTTP/1.1 200 Connection Established\r\n\r\n";
				byte[] res = response.getBytes();
				message.setData(res);
				sendMessage(message);
			}else if(protocol == Protocol.FTP){
				String response = "HTTP/1.1 200 Connection Established\r\n\r\n";
				byte[] res = response.getBytes();
				message.setData(res);
				sendMessage(message);
			}else{
				if(isForward) {
					message.setType(MessageCode.TRANSFER_DATA);
					message.setData(data);
					message.setToken(token);
					startup.writeAndFlush(message);
				}else {
					NettyClient client = nettyClientMap.get(token);
					if(client == null) {
						throw new RuntimeException("token error.");
					}
//					ByteBuf buff = ctx.alloc().buffer(data.length);
//					buff.writeBytes(data);
					client.writeAndFlush(data);
				}
			}
		}else {
			if(isForward) {
				Message message = new Message();
				message.setType(MessageCode.TRANSFER_DATA);
				message.setData(data);
				message.setToken(token);
				startup.writeAndFlush(message);
			}else {
				NettyClient client = nettyClientMap.get(token);
				if(client == null) {
					throw new RuntimeException("token error.");
				}
//				ByteBuf buff = ctx.alloc().buffer(data.length);
//				buff.writeBytes(data);
				client.writeAndFlush(data);
			}
		}
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		//关闭连接
		logger.info("User client exit.");
		NettyClient client = nettyClientMap.get(token);
		if(client != null) {
			client.close();
		}
		agentHandlerMap.remove(token);
		if(isForward) {
			Message message = new Message(token, MessageCode.CLOSE_CONNECTION, null);
			startup.writeAndFlush(message);
		}
		forwardHandlerMap.remove(token);
		this.isClosed = true;
		notifyTask();
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		this.isClosed = false;
		agentHandlerMap.put(token, this);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		String error = cause.getMessage();
		if(StringUtil.isBlank(error)) {
			cause.printStackTrace();
		}
		logger.error(error);
		ctx.channel().close();
	}
	
	public synchronized void sendMessage(Message msg) {
		queue.addLast(msg);
		notifyTask();
	}
	
	private synchronized void notifyTask() {
		this.notifyAll();
	}
	
	private synchronized Message read() throws InterruptedException {
		while(queue.isEmpty()) {
			wait();
			if(isClosed) {
				throw new InterruptedException("Client closed." + token);
			}
		}
		return queue.removeFirst();
	}
	
	private class WorkThread extends Thread{

		private ChannelHandlerContext ctx;
		
		public WorkThread(ChannelHandlerContext ctx) {
			this.ctx = ctx;
		}
		
		@Override
		public void run() {
			Channel channel = ctx.channel();
			while(!isClosed) {
				try {
					Message msg = read();
					if(msg.getType() == MessageCode.CLOSE_CONNECTION) {
						ctx.close();
						continue;
					}
					
					if(msg.getType() != MessageCode.TRANSFER_DATA) {
						continue;
					}
					
					byte[] data = (byte[]) msg.getData();
					ByteBuf buff = channel.alloc().buffer(data.length);
					buff.writeBytes(data);
					channel.writeAndFlush(buff);
				} catch (InterruptedException e) {
					logger.error(e.getMessage());
				}
			}
		}
		
	}
}
