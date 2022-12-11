package com.wikibook.bigdata.smartcar.storm;

import java.util.Map;

import backtype.storm.task.TopologyContext;
import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;


// EsperBolt의 역할은 스마트카 운전자 중, 과속을 하는 운전자를 찾아 이벤트를 발생시킨다. 과속의 기준은 80km/h 이다.
public class EsperBolt extends BaseBasicBolt{

	private static final long serialVersionUID = 1L;

	private EPServiceProvider espService;

	private boolean isOverSpeedEvent = false;

	public void prepare(Map stormConf, TopologyContext context) {

		Configuration configuration = new Configuration();
		configuration.addEventType("DriverCarInfoBean", DriverCarInfoBean.class.getName());

		espService = EPServiceProviderManager.getDefaultProvider(configuration);
		espService.initialize();
		
		// 1) 실시간으로 유입되는 스트림 데이터를 대상으로 매 30초 동안 평균 속도 80km/h를 초과한 스마트카 운전자를 감지하기 위한 에스퍼 EPL 쿼리 정의
		int avgOverSpeed = 80; // 평균 속도
		int windowTime  = 30; // Windows 운영체제 기준으로 30초
		
		String overSpeedEpl =  "SELECT date, carNumber, speedPedal, breakPedal, "
								+ "steerAngle, directLight, speed , areaNumber "
								+ " FROM DriverCarInfoBean.win:time_batch("+windowTime+" sec) "
								+ " GROUP BY carNumber HAVING AVG(speed) > " + avgOverSpeed;

		EPStatement driverCarinfoStmt = espService.getEPAdministrator().createEPL(overSpeedEpl);

		// 2) 1)에서 정의한 EPL 쿼리 조건에 일치하는 데이터가 발생했을 때 호출될 이벤트 메소드를 등록. 메호드 이름은 OverSpeedEventListener
		driverCarinfoStmt.addListener((UpdateListener) new OverSpeedEventListener());
	}



	public void execute(Tuple tuple, BasicOutputCollector collector) {

		// TODO Auto-generated method stub
		String tValue = tuple.getString(0); 

		// 발생일시(14자리), 차량번호, 가속페달, 브레이크페달, 운전대회적각, 방향지시등, 주행속도, 운행지역
		// 튜플로부터 받은 데이터를 ","로 분리해서 배열에 담는다
		String[] receiveData = tValue.split("\\,");

		// 에스퍼에서 이벤트를 처리하기 위해 자바 VO를 사용. 스마트카 운전자의 운행 정보를 객체화한 DriverCarInfoBean이라는 VO를 생성한다
		// 그 후, ","로 분리한 운행 정보를 설정 한 뒤 에스퍼 엔진에 등록
		DriverCarInfoBean driverCarInfoBean =new DriverCarInfoBean();

		driverCarInfoBean.setDate(receiveData[0]);
		driverCarInfoBean.setCarNumber(receiveData[1]);
		driverCarInfoBean.setSpeedPedal(receiveData[2]);
		driverCarInfoBean.setBreakPedal(receiveData[3]);
		driverCarInfoBean.setSteerAngle(receiveData[4]);
		driverCarInfoBean.setDirectLight(receiveData[5]);
		driverCarInfoBean.setSpeed(Integer.parseInt(receiveData[6]));
		driverCarInfoBean.setAreaNumber(receiveData[7]);

		espService.getEPRuntime().sendEvent(driverCarInfoBean); 

		// 과속 이벤트가 발생하면 해당 데이터를 다음 Bolt로 전송
		// 이 때 과속 "발생일자"와 "과속 차량번호 + 타임스탬프"를 데이터로 전송. 
		// 뒤에서 "발생 일자"는 레디스의 키로 사용되고 "과속 차량번호 + 타임스탬프"는 Set 데이터 타입의 값으로 사용됨
		if(isOverSpeedEvent) {
			//발생일시(14자리), 차량번호
			collector.emit(new Values(	driverCarInfoBean.getDate().substring(0,8), 
										driverCarInfoBean.getCarNumber()+"-"+driverCarInfoBean.getDate()));
			isOverSpeedEvent = false;
		}

	}

	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		// TODO Auto-generated method stub
		declarer.declare(new Fields("date", "car_number"));
	}


	private class OverSpeedEventListener implements UpdateListener
	{
		@Override
		public void update(EventBean[] newEvents, EventBean[] oldEvents) {
			if (newEvents != null) {
				try {
					isOverSpeedEvent = true; // EPL로 정의한 이벤트 조건이 발생할 때 호출되는 OverSpeedEventListener 메소드다. true 값 설정을 통해 해당 조건의 이벤트가 발생했음을 인지
				} catch (Exception e) {
					System.out.println("Failed to Listener Update" + e);
				} 
			}
		}
	}

}
