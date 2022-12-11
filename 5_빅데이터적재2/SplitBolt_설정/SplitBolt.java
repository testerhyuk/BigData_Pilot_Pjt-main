package com.wikibook.bigdata.smartcar.storm;

import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;


// SplitBolt의 역할은 KafKa Spout로 부터 전달받은 메시지를 HBase의 컬럼 필드 단위로 분리하기 위한 작업을 수행
public class SplitBolt extends BaseBasicBolt{

	private static final long serialVersionUID = 1L;

	public void execute(Tuple tuple, BasicOutputCollector collector) {

		String tValue = tuple.getString(0);  
		
		// 1) KafkaSpout에서 전달한 데이터가 튜플(Tuple)형식으로 수신된다. 그리고 ',' 단위로 구분해서 데이터를 담게된다
		//발생일시(14자리), 차량번호, 가속페달, 브레이크페달, 운전대회적각, 방향지시등, 주행속도, 운행지역
		String[] receiveData = tValue.split("\\,");
		
		// 2) 스마트카 운전자의 실시간 운행 정보 데이터셋의 형식을 정의. 해당 데이터를 배열로 구조화 하고 데이터를 다음단계로 전송
		// 비정형 로그 데이터를 정형 데이터로 변환
		collector.emit(new Values(	new StringBuffer(receiveData[0]).reverse() + "-" + receiveData[1]  , 
									receiveData[0], receiveData[1], receiveData[2], receiveData[3],
									receiveData[4], receiveData[5], receiveData[6], receiveData[7]));

	}

	// r_key : HBase 테이블에서 사용할 로우키
	// date : 운행 데이터 발생 일시
	// car_number : 스마트카 고유 차량 번호
	// speed_pedal : 과속 페달 단계
	// break_pedal : 브레이크 페달 단계
	// steer_angle : 운전대 회전 각도
	// direct_light : 방향 지시등
	// speed : 차량 속도
	// area : 차량 운행 지역
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("r_key"			, "date"		, "car_number", 
									"speed_pedal"	, "break_pedal"	, "steer_angle", 
									"direct_light"	, "speed"		, "area_number"));
	}

}