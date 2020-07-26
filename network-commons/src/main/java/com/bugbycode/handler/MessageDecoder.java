package com.bugbycode.handler;


import java.util.List;

import org.json.JSONObject;

import com.bugbycode.module.Authentication;
import com.bugbycode.module.ConnectionInfo;
import com.bugbycode.module.Message;
import com.bugbycode.module.MessageCode;
import com.util.TransferUtil;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class MessageDecoder extends ByteToMessageDecoder {

	private byte[] buf = {};
	
	private int len = 0;
	
	private int offset = 0;
	
	private int len_offset = 0;
	
	private byte[] len_buf = new byte[4];
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		int readableLen = 0;
		try {
			while((readableLen = in.readableBytes()) > 0) {
				if(len == 0) {
					if(len_offset < len_buf.length) {
						int temp_len = len_buf.length - len_offset;
						
						if(readableLen < temp_len) {
							temp_len = readableLen;
						}
						
						byte[] tmp_buf = new byte[temp_len];
						in.readBytes(tmp_buf);
						System.arraycopy(tmp_buf, 0, len_buf, len_offset, temp_len);
						len_offset += temp_len;
					} else {
						len = TransferUtil.toLen(len_buf);
						buf = new byte[len - len_buf.length];
					}
				} else if(offset < buf.length) {
					
					int temp_len = buf.length - offset;
					
					if(readableLen < temp_len) {
						temp_len = readableLen;
					}
					
					byte[] tmp_buf = new byte[temp_len];
					in.readBytes(tmp_buf);
					
					System.arraycopy(tmp_buf, 0, buf, offset, temp_len);
					offset += temp_len;
				} else if(offset == buf.length){
					
					int recv_offset = 0;
					byte[] token_buff = new byte[0x20];
					System.arraycopy(buf, recv_offset, token_buff, 0, token_buff.length);
					recv_offset += token_buff.length;
					
					byte[] type_buff = new byte[0x04];
					System.arraycopy(buf, recv_offset, type_buff, 0, type_buff.length);
					recv_offset += type_buff.length;
					
					Message message = new Message();
					
					String token = new String(token_buff);
					int type = TransferUtil.toLen(type_buff);
					
					if(!(type == MessageCode.HEARTBEAT || type == MessageCode.AUTH ||
							type == MessageCode.AUTH_ERROR || type == MessageCode.AUTH_SUCCESS)) {
						message.setToken(token);
					}
					
					message.setType(type);
					
					if(recv_offset < buf.length) {
						byte[] body_buf = new byte[buf.length - recv_offset];
						System.arraycopy(buf, recv_offset, body_buf, 0, body_buf.length);
						recv_offset += body_buf.length;
						
						//以下是消息内容
						if(type == MessageCode.AUTH) {
							String authInfo = new String(body_buf);
							JSONObject json = new JSONObject(authInfo);
							Authentication auth = new Authentication(json.getString("username"), json.getString("password"));
							message.setData(auth);
						}else if(type == MessageCode.CONNECTION) {
							String connInfo = new String(body_buf);
							
							JSONObject json = new JSONObject(connInfo);
							
							ConnectionInfo conn = new ConnectionInfo(json.getString("host"), json.getInt("port"));
							message.setData(conn);
						}else if(type == MessageCode.TRANSFER_DATA) {
							message.setData(body_buf);
						}
					}
					out.add(message);
					
					//初始化
					buf = new byte[0];
					len = 0;
					offset = 0;
					len_offset = 0;
					len_buf = new byte[4];
				} else {
					System.out.println("=============================");
				}
			}
		} catch (Exception e) {
			ctx.close();
			in.release();
			throw e;
		}
		
	}

}
