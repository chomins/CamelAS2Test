package ru.outofrange.as2.route;

import com.sun.istack.ByteArrayDataSource;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.as2.api.AS2EncryptionAlgorithm;
import org.apache.camel.component.as2.api.AS2MessageStructure;
import org.apache.camel.component.as2.api.AS2SignatureAlgorithm;
import org.apache.camel.component.as2.api.entity.DispositionNotificationMultipartReportEntity;
import org.apache.camel.http.common.HttpMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import ru.outofrange.cert.MendelsonCertLoader;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;

/*
ediMessage – - EDI message to transport
requestUri – - resource location to deliver message
subject – - message subject
from – - RFC2822 address of sender
as2From – - AS2 name of sender
as2To – - AS2 name of recipient
as2MessageStructure – - the structure of AS2 to send; see AS2MessageStructure
ediMessageContentType – - the content type of EDI message
ediMessageTransferEncoding – - the transfer encoding used to transport EDI message
signingAlgorithm – - the algorithm used to sign the message or null if sending EDI message unsigned
signingCertificateChain – - the chain of certificates used to sign the message or null if sending EDI message unsigned
signingPrivateKey – - the private key used to sign EDI message
compressionAlgorithm – - the algorithm used to compress the message or null if sending EDI message uncompressed
dispositionNotificationTo – - an RFC2822 address to request a receipt or null if no receipt requested
signedReceiptMicAlgorithms – - the senders list of signing algorithms for signing receipt, in preferred order, or null if requesting an unsigned receipt.
encryptingAlgorithm – - the algorithm used to encrypt the message or null if sending EDI message unencrypted
encryptingCertificateChain – - the chain of certificates used to encrypt the message or null if sending EDI message unencrypted
 */

@Component
public class MendelsonAs2ClientRouteBuilder extends RouteBuilder {

    private static final boolean NEED_SIGNED_ENCRYPTED = true;

    @Autowired
    CamelContext camelContext;

    @Autowired
    private MendelsonCertLoader mendelsonCertLoader;

    //@Value("${as2.version}")
    //private String as2Version;

    private static org.apache.http.entity.ContentType contentType =
            org.apache.http.entity.ContentType.create("application/edifact", (Charset) null);

    @Override
    public void configure() {

        from("jetty:http://localhost:3403/link")
                .routeId("as2ClientMendelson")
                .process(new Processor() {

                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String messageIn = exchange.getIn().getBody(String.class);
                        System.out.println("Request Body Message: " + messageIn);

                        if (exchange.getIn() instanceof HttpMessage) {
                            HttpMessage httpMessage =
                                    (HttpMessage) exchange.getIn();
                            HttpServletRequest request = httpMessage.getRequest();
                            String httpMethod = request.getMethod();

                            System.out.println("HTTP method: " + httpMethod);

                            if ("POST".equals(httpMethod)) {
                                System.out.println("POST request");
                            } else {
                                System.out.println("not POST request");
                            }
                        }

                        exchange.getIn().setBody(messageIn);

                        // enriching
                        exchange.getIn().setHeader("CamelAS2.as2To", "mendelsontestAS2");
                        exchange.getIn().setHeader("CamelAS2.as2From", "mycompanyAS2");
                        exchange.getIn().setHeader("CamelAS2.as2Version", "1.1");
                        exchange.getIn().setHeader("CamelAS2.ediMessageContentType", contentType);
                        exchange.getIn().setHeader("CamelAS2.server", "DKAS2Client");
                        exchange.getIn().setHeader("CamelAS2.subject", "testDK");
                        exchange.getIn().setHeader("CamelAS2.from", "DKEdi");
                        exchange.getIn().setHeader("CamelAS2.dispositionNotificationTo", "dk2k@mail.ru");
                        exchange.getIn().setHeader("CamelAS2.requestUri", "/as2/HttpReceiver");


                        if (NEED_SIGNED_ENCRYPTED) {
                            exchange.getIn().setHeader("CamelAS2.as2MessageStructure", AS2MessageStructure.SIGNED_ENCRYPTED);
                            exchange.getIn().setHeader("CamelAS2.signingAlgorithm", AS2SignatureAlgorithm.SHA1WITHRSA);
                            exchange.getIn().setHeader("CamelAS2.encryptingAlgorithm", AS2EncryptionAlgorithm.DES_EDE3_CBC);

                            exchange.getIn().setHeader("CamelAS2.signingCertificateChain", mendelsonCertLoader.getChain());
                            exchange.getIn().setHeader("CamelAS2.signingPrivateKey", mendelsonCertLoader.getPrivateKey());
                            exchange.getIn().setHeader("CamelAS2.encryptingCertificateChain", mendelsonCertLoader.getChain());
                        } else {
                            exchange.getIn().setHeader("CamelAS2.as2MessageStructure", AS2MessageStructure.PLAIN);
                        }

                        //exchange
                        //        .getOut()
                        //        .setBody("Hello from Camel");

                    }
                })
                .to("as2://client/send?targetHostName=testas2.mendelson-e-c.com" +
                        "&targetPortNumber=8080" + // http
                        "&inBody=ediMessage" +
                        "&requestUri=/as2/HttpReceiver"
                )
                //.to(clientEndpoint)
                .id("DKAS2sender")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String messageIn = exchange.getIn().getBody(String.class);
                        System.out.println("MDN Message: " + messageIn);
                        processMultipartMessage(exchange.getIn());
                    }
                });
    }

    private void processMultipartMessage(Message message) {
        //org.apache.camel.component.as2.api.entity.DispositionNotificationMultipartReportEntity
        if (message.getBody() instanceof DispositionNotificationMultipartReportEntity) {
            DispositionNotificationMultipartReportEntity dispositionNotificationMultipartReportEntity =
                    (DispositionNotificationMultipartReportEntity) message.getBody();
            try {
                InputStream inputStream = dispositionNotificationMultipartReportEntity.getContent();

                ByteArrayDataSource datasource = new ByteArrayDataSource(inputStream.readAllBytes(), "multipart/report");
                MimeMultipart multipart = new MimeMultipart(datasource);

                int count = multipart.getCount();
                log.debug("count " + count);
                for (int i = 0; i < count; i++) {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    if (bodyPart.isMimeType("text/plain")) {
                        log.info("text/plain");
                        System.out.println(bodyPart.getContent().getClass());
                        Enumeration<javax.mail.Header> headerEnumeration = bodyPart.getAllHeaders();
                        while (headerEnumeration.hasMoreElements()) {
                            javax.mail.Header header = headerEnumeration.nextElement();
                            System.out.println(header.getName() + ": " + header.getValue());
                        }
                        System.out.println("----");
                        System.out.println(bodyPart.getContent());
                        System.out.println("----");
                    } else if (bodyPart.isMimeType("application/octet-stream")) {
                        log.info("application/octet-stream");
                        System.out.println(bodyPart.getContent().getClass());
                    } else if (bodyPart.isMimeType("message/disposition-notification")) {
                        // MDN!
                        log.info("message/disposition-notification");
                        Enumeration<javax.mail.Header> headerEnumeration = bodyPart.getAllHeaders();
                        while (headerEnumeration.hasMoreElements()) {
                            javax.mail.Header header = headerEnumeration.nextElement();
                            System.out.println(header.getName() + ": " + header.getValue());
                        }
                        if (bodyPart.getContent() instanceof ByteArrayInputStream) {
                            ByteArrayInputStream byteArrayInputStream = (ByteArrayInputStream) bodyPart.getContent();
                            int n = byteArrayInputStream.available();
                            byte[] bytes = new byte[n];
                            byteArrayInputStream.read(bytes, 0, n);
                            String s = new String(bytes, StandardCharsets.UTF_8);
                            System.out.println("----");
                            System.out.println(new String(bytes));
                            System.out.println("----");
                        }
                    } else {
                        System.out.println(bodyPart.getContent().getClass());
                        log.warn("default " + bodyPart.getContentType());
                    }
                }
            } catch (IOException | MessagingException e) {
                e.printStackTrace();
            }
        }
    }
}
