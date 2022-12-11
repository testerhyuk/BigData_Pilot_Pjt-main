package com.wikibook.bigdata.smartcar.storm;

import org.apache.storm.redis.bolt.AbstractRedisBolt;
import org.apache.storm.redis.common.config.JedisClusterConfig;
import org.apache.storm.redis.common.config.JedisPoolConfig;

import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Tuple;

// RedisBolt는 과속 차량의 데이터가 발생한 경우에만 작동하며, 레디스 서버에 "과속 일자"를 키로 하고 과속한 운전자의 "차량번호+타임스탬프" 데이터값으로 적재한다
public  class RedisBolt extends AbstractRedisBolt {

	private static final long serialVersionUID = 1L;

	public RedisBolt(JedisPoolConfig config) {
		super(config);
	}

	public RedisBolt(JedisClusterConfig  config) {
		super(config);
	}


	@Override
	public void execute(Tuple input) {

		// 튜플 타입인 input 객체에는 에스퍼 Bolt에서 전송한 과속 운전자의 "과속날짜"와 "차량번호" 데이터가 있다.
		// 이 데이터를 String 타입의 "date", "car_number" 변수에 각각 할당
		String date = input.getStringByField("date");
		String car_number = input.getStringByField("car_number");

		// 레디스 클라이언트 라이브러리인 JedisCommands를 이용
		// 생성한 "과속날짜"를 key로 하고, 차량번호를 set 타입의 값으로 해서 레디스 서버에 적재
		// 적재 데이터에 대한 만료시간을 604800초(1주일)로 설정해서 1주일이 경과하면 영구 삭제되도록 함
		JedisCommands jedisCommands = null;

		try {

			jedisCommands = getInstance();
			jedisCommands.sadd(date, car_number);
			
			jedisCommands.expire(date, 604800);

		} catch (JedisConnectionException e) {
			throw new RuntimeException("Exception occurred to JedisConnection", e);
		} catch (JedisException e) {
			System.out.println("Exception occurred from Jedis/Redis" + e);
		} finally {

			if (jedisCommands != null) {
				returnInstance(jedisCommands);
			}
			this.collector.ack(input);
		}
	}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
	}
}