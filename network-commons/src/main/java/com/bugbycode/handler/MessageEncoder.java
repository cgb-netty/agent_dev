package com.bugbycode.handler;

import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;
import com.util.EncriptUtil;
import com.bugbycode.module.Authentication;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<Message> {

	private EncriptUtil eu = new EncriptUtil();
	
	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
		String token = msg.getToken();
		//首先发送token
		byte[] token_buf;
		if(token == null) {
			token_buf = new byte[32];
		}else {
			token_buf = token.getBytes();
		}
		
		eu.encriptArray(token_buf);
		
		out.writeBytes(token_buf);
		//发送消息类型
		int type = msg.getType();
		out.writeInt(type);
		//计算长度
		Object obj = msg.getData();
		byte[] body;
		if(type == MessageCode.AUTH) {
			Authentication auth = (Authentication) obj;
			String authInfo = auth.toString();
			body = authInfo.getBytes();
		}else if(type == MessageCode.CONNECTION) {
			ConnectionInfo conn = (ConnectionInfo) obj;
			String connInfo = conn.toString();
			body = connInfo.getBytes();
		}else if(type == MessageCode.TRANSFER_DATA) {
			body = (byte[]) obj;
		}else {
			body = new byte[0];
		}

		eu.encriptArray(body);
		
		int length = body.length;
		//发送消息长度和内容
		out.writeInt(length);
		if(length > 0) {
			out.writeBytes(body);
		}
	}

}
