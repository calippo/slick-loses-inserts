FROM openjdk:8-alpine

ADD rompo.jar /srv/rompo.jar

CMD /usr/bin/java -jar /srv/rompo.jar
