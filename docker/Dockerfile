FROM eclipse-temurin:17-jdk-alpine

# install maven
RUN \
	wget https://dlcdn.apache.org/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz -O /opt/apache-maven-3.9.6-bin.tar.gz && \
	tar xzf /opt/apache-maven-3.9.6-bin.tar.gz -C /opt/ && \
	ln -s /opt/apache-maven-3.9.6 /opt/maven && \
	rm -f /opt/apache-maven-3.9.6-bin.tar.gz
ENV PATH="/opt/maven/bin:$PATH"

# set keycloak-db-userfederation directories
RUN mkdir -p /opt/keycloak-db-userfederation
WORKDIR /opt/keycloak-db-userfederation

# set image cmd
ADD cmd.sh /opt/cmd.sh
RUN chmod 744 /opt/cmd.sh

CMD [ "/opt/cmd.sh" ]