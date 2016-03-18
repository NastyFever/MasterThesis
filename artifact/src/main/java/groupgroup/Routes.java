package groupgroup;

import io.netty.handler.codec.http.HttpMethod;

import org.restexpress.RestExpress;

public abstract class Routes
{
	public static void define(Configuration config, RestExpress server)
    {
		//TODO: Your routes here...
		server.uri("/your/route/here/{sampleId}.{format}", config.getSampleController())
			.method(HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE)
			.name(Constants.Routes.SINGLE_SAMPLE);

		server.uri("/regulator/request?{"+Constants.Url.numberOfTries+"}.{format}", config.getSampleController())
            .method(HttpMethod.PUT)
            .name(Constants.Routes.REQUEST);

        server.uri("/regulator/init.{format}", config.getSampleController())
                .action("init", HttpMethod.PUT)
                .name(Constants.Routes.INIT);

        server.uri("/regulator/update?{" + Constants.Url.numberOfAcceptedJobs + "}.{format}", config.getSampleController())
            .action("updateRegulator", HttpMethod.PUT)
            .name(Constants.Routes.UPDATE);

        server.uri("/your/route/here.{format}", config.getSampleController())
			.action("readAll", HttpMethod.GET)
			.method(HttpMethod.POST)
			.name(Constants.Routes.SAMPLE_COLLECTION);


        // Debugging
        server.uri("/regulator/releasedTokens.{format}", config.getSampleController())
                .action("getReleasedTokens", HttpMethod.GET);

        server.uri("/regulator/releasedTokens?{" + Constants.Url.releasedTokens + "}.{format}", config.getSampleController())
                .action("setReleasedTokens", HttpMethod.PUT);








// or...
//		server.regex("/some.regex", config.getRouteController());
    }
}
