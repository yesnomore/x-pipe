package com.ctrip.xpipe.redis.keeper.protocal.cmd;


import com.ctrip.xpipe.api.payload.InOutPayload;
import com.ctrip.xpipe.exception.XpipeException;
import com.ctrip.xpipe.payload.ByteArrayOutputStreamPayload;
import com.ctrip.xpipe.redis.core.exception.RedisRuntimeException;
import com.ctrip.xpipe.redis.keeper.protocal.RedisClientProtocol;
import com.ctrip.xpipe.redis.keeper.protocal.protocal.ArrayParser;
import com.ctrip.xpipe.redis.keeper.protocal.protocal.BulkStringParser;
import com.ctrip.xpipe.redis.keeper.protocal.protocal.IntegerParser;
import com.ctrip.xpipe.redis.keeper.protocal.protocal.RedisErrorParser;
import com.ctrip.xpipe.redis.keeper.protocal.protocal.SimpleStringParser;

import io.netty.buffer.ByteBuf;

/**
 * @author wenchao.meng
 *
 * 2016年3月24日 下午12:04:13
 */
public abstract class AbstractRedisCommand extends AbstractRequestResponseCommand {
	
	
	public static enum COMMAND_RESPONSE_STATE{
		READING_SIGN,
		READING_CONTENT;
	}
	
	private COMMAND_RESPONSE_STATE commandResponseState = COMMAND_RESPONSE_STATE.READING_SIGN;
	
	private int sign;
	
	private RedisClientProtocol<?> redisClientProtocol;
	
	protected AbstractRedisCommand() {
	}
	protected String[] splitSpace(String buff) {
		
		return buff.split("\\s+");
	}

	@Override
	protected Object readResponse(ByteBuf byteBuf) throws XpipeException{

		switch(commandResponseState){
		
			case READING_SIGN:
				int readable = byteBuf.readableBytes();
				for(int i = 0; i < readable ; i++){
					
					sign = byteBuf.readByte();
					switch(sign){
						case '\r':
							break;
						case '\n':
							break;
						case RedisClientProtocol.MINUS_BYTE:
							redisClientProtocol = new RedisErrorParser();
							break;
						case RedisClientProtocol.ASTERISK_BYTE:
							redisClientProtocol = new ArrayParser();
							break;
						case RedisClientProtocol.DOLLAR_BYTE:
							redisClientProtocol = new BulkStringParser(getBulkStringPayload());
							break;
						case RedisClientProtocol.COLON_BYTE:
							redisClientProtocol = new IntegerParser();
							break;
						case RedisClientProtocol.PLUS_BYTE:
							redisClientProtocol = new SimpleStringParser();
							break;
						default:
							throw new RedisRuntimeException("unkonwn sign:" + (char)sign);
					}
					
					if(redisClientProtocol != null){
						commandResponseState = COMMAND_RESPONSE_STATE.READING_CONTENT;
						break;
					}
				}
				
				if(redisClientProtocol == null){
					break;
				}
			case READING_CONTENT:
				RedisClientProtocol<?> result = redisClientProtocol.read(byteBuf);
				if(result != null){
					return result.getPayload();
				}
				break;
			default:
				break;
		}
		
		return null;
	}
	
	protected InOutPayload getBulkStringPayload() {
		return new ByteArrayOutputStreamPayload();
	}

	
	@Override
	protected void doReset() {
		commandResponseState = COMMAND_RESPONSE_STATE.READING_SIGN;
	}
	
	@Override
	protected void doConnectionClosed() {
		
	}
}