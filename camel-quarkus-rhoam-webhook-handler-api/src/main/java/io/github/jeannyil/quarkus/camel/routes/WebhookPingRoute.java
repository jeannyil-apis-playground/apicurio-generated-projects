package io.github.jeannyil.quarkus.camel.routes;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

/* Route that handles the webhook ping

/!\ The @ApplicationScoped annotation is required for @Inject and @ConfigProperty to work in a RouteBuilder. 
	Note that the @ApplicationScoped beans are managed by the CDI container and their life cycle is thus a bit 
	more complex than the one of the plain RouteBuilder. 
	In other words, using @ApplicationScoped in RouteBuilder comes with some boot time penalty and you should 
	therefore only annotate your RouteBuilder with @ApplicationScoped when you really need it. */
public class WebhookPingRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        
        from("direct:pingWebhook")
			.routeId("ping-webhook-route")
			.setBody()
				.method("responseMessageHelper", "generateOKResponseMessage()")
				.id("set-pingOK-reponseMessage")
            .end()
            .marshal().json(JsonLibrary.Jackson, true).id("marshal-pingOK-responseMessage-to-json")
		;

    }

}
