package io.github.jeannyil.quarkus.camel.configurations;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.apache.qpid.jms.JmsConnectionFactory;
import javax.jms.JMSException;

import org.apache.camel.quarkus.core.events.ComponentAddEvent;
import org.apache.camel.component.jms.JmsComponent;

@ApplicationScoped
public class CamelJmsConfiguration {

    @ConfigProperty(name="integrations-broker.username")
    String brokerUsername;
    @ConfigProperty(name="integrations-broker.password")
    String brokerPassword;
    @ConfigProperty(name="integrations-broker.url")
    String brokerUrl;
    @ConfigProperty(name="integrations-broker.pool-max-connections")
    int poolMaxConnections;
    @ConfigProperty(name="integrations-broker.max-sessions-per-connection")
    int maxSessionsPerConnection;

    // Returns the JmsPoolConnectionFactory for the integrations-broker
    public JmsPoolConnectionFactory getJmsPoolConnectionFactory() throws JMSException {
        JmsConnectionFactory connectionFactory = new JmsConnectionFactory();
        connectionFactory.setRemoteURI(brokerUrl);
        connectionFactory.setUsername(brokerUsername);
        connectionFactory.setPassword(brokerPassword);

        JmsPoolConnectionFactory poolConnectionFactory = new JmsPoolConnectionFactory();
        poolConnectionFactory.setConnectionFactory(connectionFactory);
        // The maximum number of connections for a single pool. The default is 1.
        poolConnectionFactory.setMaxConnections(poolMaxConnections);
        // The maximum number of sessions for each connection. The default is 500. A negative value removes any limit.
        poolConnectionFactory.setMaxSessionsPerConnection(maxSessionsPerConnection);

        return poolConnectionFactory;
    }
    
    // Configure the Camel JMS Component to use the JmsPoolConnectionFactory
    // Reference: https://access.redhat.com/documentation/en-us/red_hat_integration/2022.q3/html/developing_applications_with_camel_extensions_for_quarkus/camel-quarkus-extensions-configuration#cdi
    public void onComponentAdd(@Observes ComponentAddEvent event) throws JMSException {
        if (event.getComponent() instanceof JmsComponent) {
            JmsComponent jmsComponent = (JmsComponent) event.getComponent();
            jmsComponent.setConnectionFactory(getJmsPoolConnectionFactory());
        }
    }
}
