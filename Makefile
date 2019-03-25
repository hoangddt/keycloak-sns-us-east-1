SOURCE_FILE := target/keycloak-sms-authenticator-sns-*.jar
K_PROVIDER := '/Users/hoangddt/works/open-source/keycloak-4.8.3.Final/providers/'
build:
	mvn package

copy-to-keycloak:
	cp -f ${SOURCE_FILE} ${K_PROVIDER}