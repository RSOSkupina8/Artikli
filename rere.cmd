call mvn clean package
call docker build -t fdemsar/artikli .
call docker run -p 8080:8080 --network rso -e KUMULUZEE_DATASOURCES0_CONNECTIONURL=jdbc:postgresql://pg-artikli:5432/artikli fdemsar/artikli
