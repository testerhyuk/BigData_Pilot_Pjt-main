# 빅데이터 파일럿 프로젝트

- 참고서적 : [실무로 배우는 빅데이터 기술 2nd](https://github.com/wikibook/bigdata2nd)
- 시뮬레이터를 작동시켜 스마트카 상태 정보와 스마트카 운전자 운행 정보를 수집, 적재하는 상황입니다.
![Architecture](https://github.com/KimHyungkeun/BigData_Pilot_Pjt/blob/main/Pictures/Architecture.png)

# 1. 가상머신설정 
* 가상 하둡 클러스터 환경을 제작하기 위한 과정
- Oracle Virtual Box : 2대 (hostname : server01, server02)
- OS : CentOS 6
- RAM : (server01 : 3GB, server02 : 3GB 저사양 환경 진행)

# 2. Cloudera Manager 설치 
* Hadoop 서비스를 모니터링하기위한 서비스
- Cloudera Manager 설치 (Version CDH 6.3.2)
- CDH 설치 (Cloudera에서 제공하는 Hadoop ecosystem)
- 2020년 10월 이후, 정책의 변경으로 인해 개인적인 제품구매가 불가능하여 글쓴이가 제공하는 가상머신 및 설치방법을 통해 설치

# 3. 빅데이터 수집 
* 데이터 발생을 위한 시뮬레이터를 가동
![FlumeKafka](https://github.com/KimHyungkeun/BigData_Pilot_Pjt/blob/main/Pictures/Flume_Kafka_Flow.png)
- Flume : Source -> Channel -> Sink 의 구조를 가지며, 데이터를 수집하기 위한 기능을 담당 
- Kafka : 대규모 발생의 메시지성 데이터를 중계. 데이터를 전송하는 Producer와 데이터를 소비하는 Consumer로 나뉘며 이를 중계하는 Broker가 중간에 존재

![Kafka_Storm](https://github.com/KimHyungkeun/BigData_Pilot_Pjt/blob/main/Pictures/Kafka_Storm_Flow.png)
- Storm : 데이터를 인메모리 상에서 병렬 처리하기 위한 소프트웨어. Kafka로 부터 받은 데이터를 각각 HBase, Redis로 나누어 보낸다.
- Esper : 실시간 스트리밍 데이터의 복잡한 이벤트 처리가 필요할 때 사용하는 룰 엔진. (평균시속이 80km/h가 넘는 차량을 Redis에 담는다.)

# 4. 빅데이터 적재
- Zookeeper : 분산 코디네이터. 분산 환경에서 작동되는 작업들을 감시, 감독
* 발생되는 데이터를 저장시킴

1) 배치 처리(Batch Processing)
- HDFS(Hadoop) : 파일을 블록단위로 나누어서 각 클러스터에 분산 저장

2) 실시간 처리(Real-Time Processing)
![Kafka_Storm](https://github.com/KimHyungkeun/BigData_Pilot_Pjt/blob/main/Pictures/Kafka_Storm_Flow.png)
- HBase : 데이터를 Key-Value 구조로 단순화하고, 도큐먼트 형식의 제약사항이 적은 스키마 모델로 만들어 놓은 NoSQL의 유형
- Redis : 데이터를 RAM에 저장하는 In-memory 구조를 가지며, 저장사항을 기억하기 위한 Snapshot을 통해 데이터 손실을 방지
(Redis에서는 시속 80km/h를 초과하는 차량에 대해서만 기록을 한다)

# 5. 빅데이터 탐색 
* 적재된 데이터를 다양한 방면으로 검색
![Hive](https://github.com/KimHyungkeun/BigData_Pilot_Pjt/blob/main/Pictures/Hive_Flow.png)
- Hive : 하둡에 적재된 데이터를 DBMS 방식으로 접근하여 다루기 위한 SQL 쿼리엔진
- Spark : In-memory 방식을 통해 느린속도의 MapReduce보다 데이터를 더욱 효율적으로 처리
- Oozie : 워크플로우를 지정
- Hue : HDFS 및 쿼리엔진등을 Web UI를 통해 간편하게 다룰 수 있도록 한다

# 6. 빅데이터 분석 
- Impala : Hive 쿼리보다도 더 빠른 실시간 분석을 위한 쿼리엔진. 대용량 배치처리보다는 ad-hoc 쿼리를 통한 빠른 질의결과를 요구한다.
- 아래 그림은 스마트카 운행 지역별 평균 속도가 가장 높았던 스마트카 차량을 출력한 것이다.
![Impala](https://github.com/KimHyungkeun/BigData_Pilot_Pjt/blob/main/Pictures/Impala_Graph.png)
- Zeppelin : R과 HDFS를 서로 연결하여 원활한 데이터 분석 작업을 진행하기위한 툴. Spark를 기반으로 한다. 
- 아래 그림은 스마트카 운행 지역별 평균 속도가 가장 높았던 스마트카 차량을 출력한 것이다.
![Zeppelin](https://github.com/KimHyungkeun/BigData_Pilot_Pjt/blob/main/Pictures/Zeppelin_Graph.png)



