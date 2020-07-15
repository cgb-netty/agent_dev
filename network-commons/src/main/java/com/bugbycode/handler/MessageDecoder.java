package com.bugbycode.handler;


import org.json.JSONObject;

import com.bugbycode.module.Authentication;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;
import com.util.EncriptUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class MessageDecoder extends LengthFieldBasedFrameDecoder {

	private final int HEADER_SIZE = 40;
	
	private EncriptUtil eu = new EncriptUtil();
	
	public MessageDecoder(int maxFrameLength, 
			int lengthFieldOffset, 
			int lengthFieldLength, 
			int lengthAdjustment,
			int initialBytesToStrip) {
		super(maxFrameLength, lengthFieldOffset, lengthFieldLength, 
				lengthAdjustment, initialBytesToStrip);
	}

	@Override
	protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {

		Message message = new Message();
		
		try {
			in = (ByteBuf) super.decode(ctx,in);  
			
			if(in == null){
	            return null;
	        }
			
			if(in.readableBytes() < HEADER_SIZE){
	            throw new Exception("头部信息长度错误");
	        }
			//读取token信息 总共32个字节
			byte[] token_buff = new byte[32];
			in.readBytes(token_buff);
			//读取消息类型总共4字节
			int type = in.readInt();
			//读取长度 总共4个字节
			int length = in.readInt();
			
			if(in.readableBytes() != length) {
				 throw new Exception("标记的长度不符合实际长度");
			}
			byte[] data = {};
			if(length > 0) {
				data = new byte[length];
				in.readBytes(data);
			}
			
			eu.encriptArray(data);
			eu.encriptArray(token_buff);
			
			if(!(type == MessageCode.HEARTBEAT || type == MessageCode.AUTH ||
					type == MessageCode.AUTH_ERROR || type == MessageCode.AUTH_SUCCESS)) {
				String token = new String(token_buff);
				message.setToken(token);
			}
			
			message.setType(type);
			
			//以下是消息内容
			if(type == MessageCode.AUTH) {
				String authInfo = new String(data);
				JSONObject json = new JSONObject(authInfo);
				Authentication auth = new Authentication(json.getString("username"), json.getString("password"));
				message.setData(auth);
			}else if(type == MessageCode.CONNECTION) {
				String connInfo = new String(data);
				
				JSONObject json = new JSONObject(connInfo);
				
				ConnectionInfo conn = new ConnectionInfo(json.getString("host"), json.getInt("port"));
				message.setData(conn);
			}else if(type == MessageCode.TRANSFER_DATA) {
				message.setData(data);
			}
			return message;
		}catch (Exception e) {
			e.printStackTrace();
			throw e;
		} finally {
			if(in != null) {
				in.release();
			}
		}
	}

	

}
