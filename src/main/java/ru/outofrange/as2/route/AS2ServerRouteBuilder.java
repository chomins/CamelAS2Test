package ru.outofrange.as2.route;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.as2.AS2Component;
import org.apache.camel.component.as2.AS2Configuration;
import org.apache.camel.component.as2.api.AS2EncryptionAlgorithm;
import org.apache.camel.component.as2.api.AS2MessageStructure;
import org.apache.camel.component.as2.api.AS2ServerConnection;
import org.apache.camel.component.as2.api.AS2SignatureAlgorithm;
import org.apache.camel.component.as2.api.util.AS2Utils;
import org.apache.camel.component.as2.internal.AS2ApiName;
import org.apache.camel.component.as2.internal.AS2Constants;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpCoreContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.outofrange.cert.SelfSignedCertLoader;


import java.nio.charset.StandardCharsets;

@Component
public class AS2ServerRouteBuilder extends RouteBuilder {

    private static final boolean NEED_SIGNED_ENCRYPTED = true;

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private SelfSignedCertLoader selfSignedCertLoader;

    @Value("${as2.version}")
    private String as2Version;

    @Value("${camel.server.port}")
    private Integer serverPortNumber;

    @Value("${camel.server.uri}")
    private String as2RequestUri;

    @Override
    public void configure() throws Exception {

        final Endpoint as2ServerEndpoint = configureAs2ServerEndpoint();

        from(as2ServerEndpoint)
                .id("as2Server")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        System.out.println("Message in: " + exchange.getIn().getBody().getClass());
                        HttpCoreContext context = exchange.getProperty(AS2Constants.AS2_INTERCHANGE, HttpCoreContext.class);
                        String ediMessage = exchange.getIn().getBody(String.class);
                        if (context != null) {
                            HttpRequest request = context.getRequest();
                            HttpEntity entity = ((HttpEntityEnclosingRequest)request).getEntity();
                            String result = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8);
                            System.out.println(result);
                            log.info("\n*******************************************************************************"
                                    + "\n*******************************************************************************"
                                    + "\n\n******************* AS2 Server Endpoint Received Request **********************"
                                    + "\n\n" + AS2Utils.printMessage(request) + "\n"
                                    + "\n************************** Containing EDI message *****************************"
                                    + "\n\n" + ediMessage + "\n"
                                    + "\n*******************************************************************************"
                                    + "\n*******************************************************************************");
                        } else {
                            log.info("AS2 Interchange missing from context");
                        }
                    }
                })
                .log("redirecting message to stdout:");
    }

    private Endpoint configureAs2ServerEndpoint() throws Exception {
        String methodName = "listen";
        AS2ApiName as2ApiNameServer = AS2ApiName.SERVER;

        AS2Configuration endpointConfiguration = new AS2Configuration();
        endpointConfiguration.setTargetPortNumber(serverPortNumber);
        endpointConfiguration.setApiName(as2ApiNameServer);
        endpointConfiguration.setMethodName(methodName);
        endpointConfiguration.setRequestUri(as2RequestUri);
        endpointConfiguration.setAs2Version(as2Version);

        // camel as2 server component will send its own hardcoded template anyway
        endpointConfiguration.setMdnMessageTemplate("some template");

        if (NEED_SIGNED_ENCRYPTED) {
            AS2SignatureAlgorithm signingAlgorithm = AS2SignatureAlgorithm.SHA1WITHRSA;
            endpointConfiguration.setAs2MessageStructure(AS2MessageStructure.SIGNED_ENCRYPTED);
            endpointConfiguration.setSigningAlgorithm(signingAlgorithm);
            endpointConfiguration.setSigningCertificateChain(selfSignedCertLoader.getChain());
            endpointConfiguration.setDecryptingPrivateKey(selfSignedCertLoader.getPrivateKey());
            endpointConfiguration.setEncryptingAlgorithm(AS2EncryptionAlgorithm.DES_EDE3_CBC);
            endpointConfiguration.setEncryptingCertificateChain(selfSignedCertLoader.getChain());
        } else {
            endpointConfiguration.setAs2MessageStructure(AS2MessageStructure.PLAIN);
        }

        AS2Component as2Component = new AS2Component();
        as2Component.setCamelContext(camelContext);
        as2Component.setConfiguration(endpointConfiguration);

        Endpoint serverEndpoint = as2Component.createEndpoint("as2:server/listen?serverPortNumber={{camel.server.port}}&requestUriPattern={{camel.server.uri}}");
        System.out.println(serverEndpoint);
        return serverEndpoint;
    }
}
