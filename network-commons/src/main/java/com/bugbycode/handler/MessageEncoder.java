package com.bugbycode.handler;

import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;
import com.util.TransferUtil;
import com.bugbycode.module.Authentication;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<Message> {
	
	@Override
	protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
		int offset = 0;
		try {
			//附加数据标识
			//首先发送token
			String token = msg.getToken();
			byte[] token_buf;
			if(token == null) {
				token_buf = new byte[32];
			}else {
				token_buf = token.getBytes();
			}
			
			//类型
			int type = msg.getType();
			byte[] typeBuf = TransferUtil.toLen(msg.getType());
			
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
			
			int bodyLen = body.length;
			
			int len = 0x4 + token_buf.length + typeBuf.length + body.length;
			byte[] buf = new byte[len];
			byte[] len_buf = TransferUtil.toLen(len);
			System.arraycopy(len_buf, 0, buf, offset, len_buf.length);
			offset += len_buf.length;
			
			System.arraycopy(token_buf, 0, buf, offset, token_buf.length);
			offset += token_buf.length;
			
			System.arraycopy(typeBuf, 0, buf, offset, typeBuf.length);
			offset += typeBuf.length;
			
			System.arraycopy(body, 0, buf, offset, bodyLen);
			offset += bodyLen;
			
			out.writeBytes(buf);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

}
