# camel-as2-usage
Some time ago I needed something to start with Camel AS2. Official documentation is very poor.
This project shows the usage of Camel AS2: sending, receiving, processing MDN response.
So far I can't find a way to use HTTPS for sending out of the box.

Run the test contextLoads(). You have 100 seconds to initiate your requests say in Postman.

1. Sending is done to Mendelson endpoint - this software company maintains a service for testing of its products.
More details here https://mendelson-e-c.com/as2_testserver
Web UI to check receiving of your messages http://testas2.mendelson-e-c.com:8080/webas2/ login guest, password guest.
NEED_SIGNED_ENCRYPTED=false allows to send plain text - no signature and encryption.
NEED_SIGNED_ENCRYPTED=true allows to send signed and encrypted messages. I've already downloaded Mendelson test certificate and private key needed for that into resources dir. There is an utility class for reading them from files.
Please note that Mendelson's resource won't process plain as2 messages as well as with unexpected 'to' and 'from' fields and your own certificate/private key.
For testing send POST request with non-empty body to link http://localhost:3400/link - it will trigger an as2 request to Mendelson.

2. Sending and receiving is done locally with a self signed certificate and private key.
Sample command for generation:
openssl req -new -newkey rsa:1024 -nodes -keyout test_private_key.pem -x509 -days 500 -subj /C=RU/ST=MSK/L=MSK/O=OPS/OU=DK/CN=localhost/emailAddress=dk2k@mail.ru -out test_cacert.crt
Again:
NEED_SIGNED_ENCRYPTED=false allows to send plain text - no signature and encryption.
NEED_SIGNED_ENCRYPTED=true allows to send signed and encrypted messages.
As for MDN - for now the specified template is ignored, camel as2 server uses the hardcoded one - please see the class org.apache.camel.component.as2.api.protocol.ResponseMDN.
For testing send POST request with non-empty body to link http://localhost:3500/link - it will trigger an as2 request to a local as2 server.

Java Mail needed to process multipart messages.

If you feel that this repo saved your time and nerves, feel free to donate to paypal account krjukov@gmail.com ;)
