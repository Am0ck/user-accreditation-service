#!/bin/bash
./mvnw clean package -DskipTests && java -jar target/*.jar
