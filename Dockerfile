FROM java
ENTRYPOINT ["/usr/bin/java", "-jar", "/usr/share/backend/prepaidcard.jar"]

ADD target/lib /usr/share/backend/lib
ADD target/prepaidcard.jar /usr/share/backend/prepaidcard.jar
EXPOSE 9001