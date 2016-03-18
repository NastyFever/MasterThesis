package groupgroup;

import groupgroup.Constants;
import groupgroup.Regulator.Regulator;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.restexpress.Request;
import org.restexpress.Response;

public class RegulatorDebugController {
    private final SampleController sampleController;


    public RegulatorDebugController(SampleController sampleController) {
        this.sampleController = sampleController;
   }

    public void getReleasedTokens(Request request, Response response){
        response.setBody(sampleController.getRegulator().getNumberOfReleasedTokens());
    }

    public void setReleasedTokens(Request request, Response response){
        System.out.printf("apa");
        QueryStringDecoder decoder = new QueryStringDecoder(request.getUrl());
        long numberOfReleasedTokens = Long.parseLong(decoder.parameters().get(Constants.Url.releasedTokens).get(0));
        sampleController.getRegulator().setNumberOfReleasedTokens(numberOfReleasedTokens);
        response.setBody(sampleController.getRegulator().getNumberOfReleasedTokens());
    }

    public void getFinishedJobs(Request request, Response response) {
        response.setBody(sampleController.getRegulator().getNumberOfFinishedJobs());
    }

}
