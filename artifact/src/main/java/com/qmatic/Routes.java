package com.qmatic;

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

		server.uri("/regulator/request?{" + Constants.Url.numberOfTries + "}.{format}", config.getSampleController())
            .action("requestAccess", HttpMethod.PUT)
            .name(Constants.Routes.REQUEST);

        server.uri("/regulator/init.{format}", config.getSampleController())
                .action("init", HttpMethod.PUT)
                .name(Constants.Routes.INIT);

        server.uri("/regulator/update?{" + Constants.Url.numberOfAcceptedJobs + Constants.Url.jobTime + "}.{format}", config.getSampleController())
            .action("updateRegulatorJobTime", HttpMethod.PUT)
            .name(Constants.Routes.UPDATE_WITH_JOBTIME);

        server.uri("/your/route/here.{format}", config.getSampleController())
			.action("readAll", HttpMethod.GET)
			.method(HttpMethod.POST)
			.name(Constants.Routes.SAMPLE_COLLECTION);


        // Debugging
        server.uri("/regulator/releasedTokens.{format}", config.getRegulatorDebugController())
                .action("getReleasedTokens", HttpMethod.GET);

        server.uri("/regulator/releasedTokens?{" + Constants.Url.releasedTokens + "}.{format}", config.getRegulatorDebugController())
                .action("setReleasedTokens", HttpMethod.PUT);

        server.uri("/regulator/finishedJobs.{format}", config.getRegulatorDebugController())
                .action("getFinishedJobs", HttpMethod.GET);

        server.uri("/regulator/setNumberOfReleasedTokensToNumberOfFinishedJobs.{format}", config.getRegulatorDebugController())
                .action("setNumberOfReleasedTokensToNumberOfFinishedJobs", HttpMethod.PUT);
// or...
//		server.regex("/some.regex", config.getRouteController());
    }
}
