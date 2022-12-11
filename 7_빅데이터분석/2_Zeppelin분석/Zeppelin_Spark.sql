-- HDFS의 데이터를 로드하여 스파크 데이터셋으로 생성하였음
val url="hdfs://server01.hadoop.com:8020"
val dPath="/user/hive/warehouse/managed_smartcar_drive_info/biz_date=20200322/*"
val driveData=sc.textFile(url + dPath)
case class DriveInfo(car_num: String,        sex: String,           age: String,            
                     marriage: String,       region: String,        job: String,           
                     car_capacity: String,   car_year: String,      car_model: String,
                     speed_pedal: String,    break_pedal: String,   steer_angle: String, 
                     direct_light: String,   speed: String,         area_num: String,       
                     date: String)

val drive = driveData.map(sd=>sd.split(",")).map(
                sd=>DriveInfo(sd(0).toString, sd(1).toString, sd(2).toString, sd(3).toString,
                              sd(4).toString, sd(5).toString, sd(6).toString, sd(7).toString,
                              sd(8).toString, sd(9).toString, sd(10).toString,sd(11).toString,
                              sd(12).toString,sd(13).toString,sd(14).toString,sd(15).toString
        )
)

drive.toDF().registerTempTable("DriveInfo")

-- 운행한 지역의 평균속도를 구하고, 평균 속도가 높은 순서대로 출력한다
%spark.sql
select T1.area_num, T1.avg_speed     
from  (select area_num, avg(speed) as avg_speed
       from DriveInfo
       group by area_num
       ) T1
order by T1.avg_speed desc

-- 운행한 지역의 평균속도가 60이상인 곳을 구해서, 속도가 높은순서대로 출력한다
%spark.sql
select T1.area_num, T1.avg_speed     
from  (select area_num, avg(speed) as avg_speed
       from DriveInfo 
       group by area_num having avg_speed >= ${AvgSpeed=60}
       ) T1
order by T1.avg_speed desc