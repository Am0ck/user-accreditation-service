#!/usr/bin/env sh
./mvnw clean package -DskipTests && java -jar target/*.jar
