# Import base image
FROM eclipse-temurin:8-jre-alpine

# Create log file directory and set permission
RUN addgroup -S gwas-deposition-backend && adduser -S -G gwas-deposition-backend -h /home/gwas-deposition-backend gwas-deposition-backend
RUN if [ ! -d /var/log/gwas/ ];then mkdir /var/log/gwas/;fi
RUN chown -R gwas-deposition-backend:gwas-deposition-backend /var/log/gwas

# Move project artifact
ADD target/gwasdepo-deposition-service-*.jar /home/gwas-deposition-backend/
USER gwas-deposition-backend

# Launch application server
ENTRYPOINT exec $JAVA_HOME/bin/java $XMX $XMS -jar -Dspring.profiles.active=$ENVIRONMENT -Dspring.rabbitmq.password=$RABBIT_PWD /home/gwas-deposition-backend/gwasdepo-deposition-service-*.jar
