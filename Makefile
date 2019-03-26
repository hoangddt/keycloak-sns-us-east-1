SOURCE_FILE := target/keycloak-sms-authenticator-sns-*.jar
K_HOME := /Users/hoangddt/works/open-source/keycloak-4.8.3.Final/
K_PROVIDER := /Users/hoangddt/works/open-source/keycloak-4.8.3.Final/providers/

build:
	mvn package

copy-to-keycloak:
	cp -f ${SOURCE_FILE} ${K_PROVIDER}

deploy: build copy-to-keycloak

copy-template:
	cp -f templates/*.ftl "${K_HOME}themes/base/login/"