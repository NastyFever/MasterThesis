package groupgroup;

import java.util.Collections;
import java.util.List;

import groupgroup.Regulator.Regulator;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.restexpress.Request;
import org.restexpress.Response;
import org.json.simple.*;

public class SampleController
{
    private Regulator regulator;
    private int SOME_LWM = 100;
    private int SOME_HWM = 400;
    private int SOME_AM = 250;

    public SampleController()
	{
        super();
        regulator = new Regulator(SOME_LWM, SOME_HWM, SOME_AM);
	}

	public Object create(Request request, Response response)
	{
        System.out.println("HTTP POST");
        //TODO: Your 'POST' logic here...
		return null;
	}

	public Object read(Request request, Response response)
	{
		//TODO: Your 'GET' logic here...
        System.out.println("GET");

        return null;
	}

	public List<Object> readAll(Request request, Response response)
	{
		//TODO: Your 'GET collection' logic here...
        System.out.println("GET Collection");
        return Collections.emptyList();
	}

    public void init(Request request, Response response) {
        System.out.println("Innit recieved from central. Restarting the regulator.");
        regulator = new Regulator(SOME_LWM, SOME_HWM, SOME_AM);
        response.setResponseNoContent();
    }

	public void update(Request request, Response response)
	{
		//TODO: Your 'PUT' logic here...
        System.out.println("HTTP PUT");

        QueryStringDecoder decoder = new QueryStringDecoder(request.getUrl());
        int numberOfRetries = Integer.parseInt(decoder.parameters().get(Constants.Url.numberOfTries).get(0));
        System.out.println("" + numberOfRetries);

        JSONObject jc = regulator.handleRequest(numberOfRetries);

        response.setBody(jc);
	}

    public void updateRegulator(Request request, Response response){
        QueryStringDecoder decoder = new QueryStringDecoder(request.getUrl());
        long numberOfAcceptedJobs = Long.parseLong(decoder.parameters().get(Constants.Url.numberOfAcceptedJobs).get(0));
        System.out.println("Update recieved from application server. Number of accpeted jobs " + numberOfAcceptedJobs);
        regulator.recievedUpdateFromApplicationServer(numberOfAcceptedJobs);
        response.setResponseNoContent();
    }

    public void delete(Request request, Response response)
	{
		//TODO: Your 'DELETE' logic here...
        System.out.println("HTTP DELETE");
        response.setResponseNoContent();
	}

    public Regulator getRegulator() {
        return regulator;
    }
}
