package com.wikibook.bigdata.smartcar.storm;


import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.apache.storm.guava.collect.Maps;
import org.apache.storm.redis.common.config.JedisPoolConfig;

import storm.kafka.BrokerHosts;
import storm.kafka.KafkaSpout;
import storm.kafka.SpoutConfig;
import storm.kafka.StringScheme;
import storm.kafka.ZkHosts;
import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.generated.StormTopology;
import backtype.storm.spout.Scheme;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.topology.TopologyBuilder;


public class SmartCarDriverTopology {


	public static void main(String[] args) throws AlreadyAliveException, InvalidTopologyException, InterruptedException, IOException {  

		StormTopology topology = makeTopology();

		Map<String, String> HBaseConfig = Maps.newHashMap();
		HBaseConfig.put("hbase.rootdir","hdfs://server01.hadoop.com:8020/hbase");

		Config config = new Config();
		config.setDebug(true);
		config.put("HBASE_CONFIG",HBaseConfig);

		config.put(Config.NIMBUS_HOST, "server01.hadoop.com");
		config.put(Config.NIMBUS_THRIFT_PORT, 6627);
		config.put(Config.STORM_ZOOKEEPER_PORT, 2181);
		config.put(Config.STORM_ZOOKEEPER_SERVERS, Arrays.asList("server02.hadoop.com"));

		StormSubmitter.submitTopology(args[0], config, topology);

	}  


	// 스톰 Spout의 기본 기능은 외부 시스템과의 연동을 통해 스톰의 Topology로 데이터를 가져오는 것이다.
	// Kafka가 외부시스템과 스톰의 Topology를 이어주는 역할을 하게 된다
	private static StormTopology makeTopology() {

		// 1) KafkaSpout 객체를 생성. 주키퍼 서버 정보, 카프카 토픽 정보, 메시지 형식, 메시지 수신 방식 등을 설정
		String zkHost = "server02.hadoop.com:2181";
		TopologyBuilder driverCarTopologyBuilder = new TopologyBuilder();
		
		// Spout Bolt
		BrokerHosts brkBost = new ZkHosts(zkHost);
		String topicName = "SmartCar-Topic";
		String zkPathName = "/SmartCar-Topic";

		SpoutConfig spoutConf = new SpoutConfig(brkBost, topicName, zkPathName, UUID.randomUUID().toString());
		spoutConf.scheme = new SchemeAsMultiScheme(new StringScheme());
		KafkaSpout kafkaSpout = new KafkaSpout(spoutConf);
		
		// 2) KafkaSpout 객체를 스톰의 Topology에 설정. "고유ID", "kafkaSpout객체", "병렬 처리 힌트 제공 여부"
		driverCarTopologyBuilder.setSpout("kafkaSpout", kafkaSpout, 1);
		
		
		// Grouping - SplitBolt & EsperBolt
		// 3) Kafka가 수신받은 데이터를 어떻게 라우팅할지에 대한 설정이다.
		// 스톰 Topology의 그루핑 설정으로 All-Grouping 기능을 설정한 "kafkaSpout"로부터 받은 두 데이터를 두개의 Bolt(Split/Esper)에 동일하게 전달
		driverCarTopologyBuilder.setBolt("splitBolt", new SplitBolt(),1).allGrouping("kafkaSpout");
		driverCarTopologyBuilder.setBolt("esperBolt", new EsperBolt(),1).allGrouping("kafkaSpout");
		
		
		// HBase Bolt
		// 4) HBase의 데이터를 적재하기 위한 Config 정보 설정
		// "DriverCarInfo"에 고유 테이블 번호인 "r_key"를 정의
		// 칼럼 패밀리 cf1에 해당하는 8개의 칼럼명 정보와 Zookeeper 연결 정보도 확인
		TupleTableConfig hTableConfig = new TupleTableConfig("DriverCarInfo", "r_key");
		hTableConfig.setZkQuorum("server02.hadoop.com");
		hTableConfig.setZkClientPort("2181");
		hTableConfig.setBatch(false);
		hTableConfig.addColumn("cf1", "date");
		hTableConfig.addColumn("cf1", "car_number");
		hTableConfig.addColumn("cf1", "speed_pedal");
		hTableConfig.addColumn("cf1", "break_pedal");
		hTableConfig.addColumn("cf1", "steer_angle");
		hTableConfig.addColumn("cf1", "direct_light");
		hTableConfig.addColumn("cf1", "speed");
		hTableConfig.addColumn("cf1", "area_number");
		
		// 5) 설정한 HBase의 Config 정보(hTableConfig)를 이요해 HBaseBolt 객체 생성
		HBaseBolt hbaseBolt = new HBaseBolt(hTableConfig);
		// 6) HBaseBolt 객체를 스톰의 Topology에 설정. "고유ID", "HBaseBolt 객체", "병렬 처리 힌트"를 설정.
		// SplitBolt로 부터 데이터를 전달받기 위해 그루핑명을 "splitBolt"로 설정. ( 3)단계에서 설정한 이름 )
		driverCarTopologyBuilder.setBolt("HBASE", hbaseBolt, 1).shuffleGrouping("splitBolt");
		
		
		// Redis Bolt
		// Redis의 클라이언트를 이용하기 위해 JedisPoolConfig를 사용. 레디스 서버 주소와 포트 정보를 Config로 설정하고, 해당 Config를 이용해 RedisBolt 객체를 생성
		String redisServer = "server02.hadoop.com";
		int redisPort = 6379;
		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig.Builder().setHost(redisServer).setPort(redisPort).build();
		RedisBolt redisBolt = new RedisBolt(jedisPoolConfig);
		// 생성한 RedisBolt 객체를 스톰의 Topology에 등록
		// EsperBolt로 부터 데이터를 전달받기 위해 그루핑명을 "esperBolt"로 설정. ( 3)단계에서 설정한 이름 )
		driverCarTopologyBuilder.setBolt("REDIS", redisBolt, 1).shuffleGrouping("esperBolt");

		return driverCarTopologyBuilder.createTopology();
	}

}  
