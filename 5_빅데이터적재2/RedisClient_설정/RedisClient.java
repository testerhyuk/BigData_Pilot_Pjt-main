package com.wikibook.bigdata.smartcar.redis;

import java.util.Set;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


public class RedisClient extends Thread{

	private String key;
	private Jedis jedis;


	public RedisClient(String k) {

		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
		JedisPool jPool = new JedisPool(jedisPoolConfig, "server02.hadoop.com", 6379);
		jedis = jPool.getResource();

		this.key = k;
		

	}

	@Override    
	public void run() {
		

		Set<String> overSpeedCarList = null;
		
		int cnt = 1;


		try {
			while(true) {

				overSpeedCarList = jedis.smembers(key);

				System.out.println("################################################");
				System.out.println("#####   Start of The OverSpeed SmartCar    #####");
				System.out.println("################################################");
				
				
				System.out.println("\n[ Try No." + cnt++ + "]");

				if(overSpeedCarList.size() > 0) { // 레디스에는 날짜를 key로 해서 과속 차량의 데이터셋이 저장됨. 키(날짜)에 해당되는 과속 차량 번호가 발생하면 즉시 가져와서 출력
					for (String list : overSpeedCarList) {
						System.out.println(list);
					}
					System.out.println("");
					
					jedis.del(key);
				}else{
					System.out.println("\nEmpty Car List...\n"); // Redis에서 가져온 키의 데이터를 삭제
				}

				System.out.println("################################################");
				System.out.println("######   End of The OverSpeed SmartCar    ######");
				System.out.println("################################################");
				System.out.println("\n\n");

				Thread.sleep(10 * 1000);
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if( jedis != null ) jedis.close();
		}
	}
}
