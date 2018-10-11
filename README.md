# keycloak-sms-authenticator-sns

To install the SMS Authenticator one has to:

* Build and package the project:
  * `$ mvn package`

* Add the jar to the Keycloak server:
  * `$ cp target/keycloak-sms-authenticator-sns-*.jar _KEYCLOAK_HOME_/providers/`

* Add three templates to the Keycloak server:
  * `$ cp templates/sms-validation.ftl _KEYCLOAK_HOME_/themes/base/login/`
  * `$ cp templates/sms-validation-error.ftl _KEYCLOAK_HOME_/themes/base/login/`
  * `$ cp templates/sms-validation-mobile-number.ftl _KEYCLOAK_HOME_/themes/base/login/`

* Append the additional template messages to the Keycloak base template:
  * `$ cat templates/messages/messages_en.properties >> _KEYCLOAK_HOME_/themes/base/login/messages/messages_en.properties`


## Configuration

Configure your REALM to use the SMS Authentication.
First create a new REALM (or select a previously created REALM).

Under Authentication > Flows:
* Copy the 'Browser' flow to 'Browser with SMS' flow
* Click on 'Actions > Add execution' on the 'Browser with SMS Forms' line and add the 'SMS Authentication'
* Set 'SMS Authentication' to 'REQUIRED' or 'ALTERNATIVE'
* To configure the SMS Authenticator, click 'Actions > Config' and fill in the relevant attributes
  * When using **GOV.UK Notify**, the only attribute that needs changing is to select 'GOVUK_NOTIFY' as the SMS gateway.

Under Authentication > Bindings:
* Select 'Browser with SMS' as the 'Browser Flow' for the REALM.

Under Authentication > Required Actions:
* Click on the 'Register' button and select 'Update Mobile Number' to add the Required Action to the REALM.
* Make sure that for 'Update Mobile Number' both the 'Enabled' and 'Default Action' check boxes are checked.


## Contributions

Malys contributions (for [Lyra Network](https://www.lyra-network.com/))
* Internationalization support
* Vault, Java properties, environment variables parameters support
* Lyrasms gateway support
* Add mobilephone number verification
* Add input mobile phone number on authenticator
* Refactoring
* Template cleaning
* Documentation
