package groupgroup;

import java.util.Collections;
import java.util.List;

import io.netty.handler.codec.http.QueryStringDecoder;
import org.restexpress.Request;
import org.restexpress.Response;

public class SampleController
{
	public SampleController()
	{
		super();
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
        System.out.println("bajs read");

        return null;
	}

	public List<Object> readAll(Request request, Response response)
	{
		//TODO: Your 'GET collection' logic here...
        System.out.println("bajs");
        return Collections.emptyList();
	}

	public void update(Request request, Response response)
	{
		//TODO: Your 'PUT' logic here...
        System.out.println("HTTP PUT");
        QueryStringDecoder decoder = new QueryStringDecoder(request.getUrl());
        int numberOfRetries = Integer.parseInt(decoder.parameters().get(Constants.Url.numberOfTries).get(0));
        System.out.println("" + numberOfRetries);

        boolean accessService = handleRequest(numberOfRetries);

		response.setResponseNoContent();
	}

    private boolean handleRequest(int numberOfRetries) {
        return true;
    }

    public void delete(Request request, Response response)
	{
		//TODO: Your 'DELETE' logic here...
        System.out.println("HTTP DELETE");
        response.setResponseNoContent();
	}
}
